# Sistema de Chamados do IFRN

Projeto individual da disciplina de Desenvolvimento de Sistemas Corporativos (DSC).
API REST para registrar e acompanhar chamados de suporte do IFRN.

## Tecnologias

- Java 21 e Spring Boot 4
- Maven (Wrapper incluido no projeto)
- Spring Web MVC, Spring Data JPA e Bean Validation
- PostgreSQL e Flyway
- Lombok

## Estado atual

A persistencia, as validacoes, os cadastros e o fluxo dos chamados estao prontos.
As listagens de chamados, seus filtros e a execucao com Docker Compose ficam
para as proximas etapas.

## Organizacao

Os pacotes ficam em `src/main/java/br/edu/ifrn/chamados`:

| Pacote | Responsabilidade |
| --- | --- |
| `controller` | Receber requisicoes, chamar o Service e devolver respostas. |
| `service` | Aplicar as regras de negocio. |
| `repository` | Acessar o banco com Spring Data JPA. |
| `model` | Representar entidades e enums. |
| `dto` | Definir os dados de entrada e saida da API. |
| `exception` | Padronizar respostas de erro. |

As dependencias entre classes serao recebidas pelo construtor, sem `@Autowired` em atributos.
Os arquivos `package-info.java` documentam a responsabilidade de cada pacote.

## Compilar

Requer JDK 21. Na primeira execucao, o Maven Wrapper precisa de internet para baixar
o Maven e as dependencias.

No PowerShell, dentro da pasta do projeto:

```powershell
.\mvnw.cmd clean package -DskipTests
```

No Linux ou macOS:

```bash
./mvnw clean package -DskipTests
```

Esse comando verifica a compilacao e gera o JAR em `target/`. Os testes nao sao
executados com `-DskipTests`. Para executar a suite completa, configure o PostgreSQL
e use `clean verify`, como explicado abaixo.

## Configuracao do banco

A aplicacao recebe a conexao por variaveis de ambiente:

| Variavel | Conteudo |
| --- | --- |
| `DB_URL` | URL JDBC, por exemplo `jdbc:postgresql://localhost:5432/chamados`. |
| `DB_USERNAME` | Usuario do PostgreSQL. |
| `DB_PASSWORD` | Senha do PostgreSQL. |

Nao coloque credenciais no `application.properties` nem no Git. Arquivos `.env`
sao ignorados, mas o Spring Boot nao os carrega automaticamente.

O Flyway ficara responsavel por criar e evoluir o banco. O Hibernate usa
`ddl-auto=validate`, portanto nao cria nem altera tabelas. As migrations ficam em
`src/main/resources/db/migration`: `V1` cria as tabelas e `V2` insere os dados iniciais.
Sao 2 setores, 4 categorias e 4 usuarios ficticios: 2 solicitantes e 2 tecnicos.

As listagens usam paginas de 10 itens por padrao, com limite de 50.

## Desenvolvimento

Usaremos uma branch por task e commits pequenos, agrupados por mudanca com sentido.
Ao concluir e conferir a task, abriremos um Pull Request para a `main`.
Nao e necessario criar uma branch `develop` neste projeto individual.

Exemplos de branches: `chore/estrutura-inicial`, `feat/persistencia` e `feat/cadastros`.
Exemplos de commits: `feat: adiciona entidade Setor` e `fix: impede email duplicado`.

A entrega final sera a versao disponivel na `main`, com Docker Compose,
exemplos de requisicao e as orientacoes completas de execucao.


## Validar a persistencia localmente

Com o Docker Desktop iniciado, crie um PostgreSQL de desenvolvimento (uma unica vez):

```powershell
docker run --name chamados-postgres -e POSTGRES_DB=chamados -e POSTGRES_USER=chamados -e POSTGRES_PASSWORD=chamados_local -p 5432:5432 -d postgres:17
```

A senha acima e apenas um exemplo para o banco local de desenvolvimento.
Se o container ja existir e estiver parado, use `docker start chamados-postgres`.
Espere o banco ficar pronto antes de executar os testes:

```powershell
docker exec chamados-postgres pg_isready -U chamados -d chamados
```

Defina as variaveis na mesma sessao do PowerShell:

```powershell
$env:DB_URL = "jdbc:postgresql://localhost:5432/chamados"
$env:DB_USERNAME = "chamados"
$env:DB_PASSWORD = "chamados_local"
.\mvnw.cmd clean verify
```

Os testes usam um PostgreSQL de desenvolvimento. O Flyway aplica as migrations e
o Hibernate valida o mapeamento. O teste de persistencia verifica os relacionamentos
e desfaz os registros criados no teste ao terminar.

Para iniciar a aplicacao depois dos testes:

```powershell
.\mvnw.cmd spring-boot:run
```

A raiz nao tem endpoint. Para conferir a API, acesse `http://localhost:8080/setores`.
O Docker Compose que iniciara toda a aplicacao sera adicionado na task de Docker.

## Migrations

O Flyway registra cada migration aplicada na tabela `flyway_schema_history`,
incluindo a versao e o checksum do arquivo. Se uma migration aplicada for editada,
o checksum muda e a validacao falha. Por isso, correcoes entram em uma nova
migration com a proxima versao; nao alteramos as migrations ja aplicadas.

Nao usamos `ddl-auto=update` em producao porque as alteracoes automaticas do
Hibernate nao substituem um historico de SQL revisado e versionado. Aqui o Flyway
altera o esquema e `ddl-auto=validate` confere se ele corresponde as entidades.

## Validacoes e erros

Os DTOs de entrada validam campos obrigatorios, email, limites de tamanho e IDs
positivos. Os Controllers dos cadastros usam `@Valid` ao receber esses DTOs.
As regras de negocio continuam nos Services.

Os erros retornam `timestamp`, `status`, `mensagem` e `caminho`. Dados invalidos
retornam 400; recursos inexistentes, 404; conflitos de negocio ou integridade, 409.
Os Services usam `RecursoNaoEncontradoException` e `RegraNegocioException`.

Para executar apenas os testes de validacao e tratamento de erros, sem banco:

```powershell
.\mvnw.cmd "-Dtest=ValidacaoDtosTests,ApiExceptionHandlerTests" test
```

O Controller auxiliar de `ApiExceptionHandlerTests` existe somente no teste.

## Cadastros

| Recurso | Rotas | Filtros da listagem |
| --- | --- | --- |
| Setores | POST e GET `/setores`; GET, PUT e DELETE `/setores/{id}` | `nome` |
| Categorias | POST e GET `/categorias`; GET, PUT e DELETE `/categorias/{id}` | `nome`, `setorId` |
| Categorias de um setor | GET `/setores/{id}/categorias` | `nome` |
| Usuarios | POST e GET `/usuarios`; GET e PUT `/usuarios/{id}` | `nome`, `tipo`, `setorId` |

POST retorna 201 com o endereco do recurso em `Location`; consultas e atualizacoes
retornam 200; exclusoes retornam 204. Nao ha DELETE de usuarios.

As listagens aceitam `page`, `size` e `sort`. O JSON inclui `content`, `number`,
`size`, `totalElements` e `totalPages`. Os filtros podem ser combinados e o nome
e buscado por trecho, sem diferenciar maiusculas de minusculas.

Exemplos:

```http
GET http://localhost:8080/setores?nome=tecnologia&page=0&size=10&sort=nome,asc
GET http://localhost:8080/categorias?nome=rede&setorId=1
GET http://localhost:8080/usuarios?tipo=TECNICO&setorId=1
```

Sigla e email duplicados retornam 409, inclusive quando mudam apenas maiusculas
e minusculas. A atualizacao permite manter a sigla ou o email do proprio cadastro.
Tecnicos precisam de um setor; setor informado e inexistente retorna 404.

Setores e categorias com chamados nao podem ser excluidos, independentemente do
status do chamado. Setores com categorias ou usuarios vinculados tambem retornam
409: remova ou reatribua esses vinculos antes de excluir, sem apagar dados em cascata.

O arquivo `requisicoes.http` contem as operacoes dos cadastros e os fluxos dos
chamados. Com a extensao REST Client no VS Code, execute as requisicoes na ordem
do arquivo; os IDs dos novos registros sao reaproveitados nas requisicoes seguintes.
A secao de chamados usa os usuarios e a categoria da migration V2; ajuste as
variaveis do inicio do arquivo se esses registros tiverem sido alterados.

Para testar Controllers, Services, validacoes e erros sem PostgreSQL:

```powershell
.\mvnw.cmd "-Dtest=CadastroServiceTests,CadastroControllerTests,ValidacaoDtosTests,ApiExceptionHandlerTests" test
```

Com o banco e as variaveis de ambiente configurados, `./mvnw clean verify`
(ou `.\mvnw.cmd clean verify` no PowerShell) tambem executa os testes de persistencia
dos cadastros, filtros e bloqueios de exclusao. Os dados desses testes sao desfeitos
ao terminar.

## Fluxo dos chamados

| Metodo e rota | Operacao | Sucesso |
| --- | --- | --- |
| POST `/chamados` | Abrir um chamado | 201 |
| GET `/chamados/{id}` | Consultar detalhes e interacoes | 200 |
| PATCH `/chamados/{id}/assumir` | Atribuir o chamado ao tecnico | 200 |
| PATCH `/chamados/{id}/resolver` | Resolver com uma solucao | 200 |
| PATCH `/chamados/{id}/cancelar` | Cancelar pelo solicitante | 200 |
| POST `/chamados/{id}/interacoes` | Registrar andamento ou mais detalhes | 201 |

As regras ficam no `ChamadoService`. O sistema preenche as datas, abre o chamado
com status `ABERTO` e usa prioridade `MEDIA` quando ela nao e informada.
Somente usuarios do tipo `SOLICITANTE` podem abrir chamados.

| Acao | Status exigido | Quem pode executar | Novo status |
| --- | --- | --- | --- |
| Assumir | ABERTO | Tecnico do setor da categoria | EM_ATENDIMENTO |
| Resolver | EM_ATENDIMENTO | Tecnico atribuido, informando a solucao | RESOLVIDO |
| Cancelar | ABERTO | Proprio solicitante | CANCELADO |
| Registrar interacao | ABERTO ou EM_ATENDIMENTO | Solicitante ou tecnico atribuido | Sem alteracao |

A resolucao e o cancelamento preenchem `dataFechamento`. Chamados encerrados nao
aceitam novas acoes nem interacoes. As transicoes invalidas e as acoes de usuarios
sem permissao retornam 409. IDs inexistentes retornam 404 e entradas invalidas,
como uma solucao vazia, retornam 400.

Nao ha autenticacao: `solicitanteId`, `tecnicoId` ou `autorId` identificam o usuario
no corpo da requisicao. A consulta detalhada traz as interacoes ordenadas por
`dataHora` e, em caso de empate, por `id`.

Exemplo de abertura, usando os dados iniciais:

```http
POST http://localhost:8080/chamados
Content-Type: application/json

{
  "titulo": "Wi-Fi indisponivel",
  "descricao": "Nao consigo conectar a rede",
  "local": "Laboratorio 1",
  "solicitanteId": 1,
  "categoriaId": 1
}
```

Para testar todos os Controllers, Services, validacoes e erros sem banco:

```powershell
.\mvnw.cmd "-Dtest=ChamadoServiceTests,ChamadoControllerTests,CadastroServiceTests,CadastroControllerTests,ValidacaoDtosTests,ApiExceptionHandlerTests" test
```

Com o PostgreSQL configurado, `.\mvnw.cmd clean verify` tambem verifica os fluxos
completos de resolucao e cancelamento, a recuperacao do historico e os bloqueios
apos encerramento. Esses testes desfazem seus dados ao terminar.

As rotas GET `/chamados` e GET `/usuarios/{id}/chamados`, com paginacao e filtros,
serao implementadas na proxima task.
