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

A estrutura inicial, as entidades, as migrations, os DTOs e o tratamento de erros estao prontos.
Os cadastros, as regras de negocio e a execucao com Docker Compose serao implementados nas proximas etapas.
Ainda nao ha endpoints disponiveis.

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
executados nessa etapa porque o teste de contexto gerado pelo Initializr precisa
do PostgreSQL configurado. A execucao dos testes sera incluida na validacao das
proximas funcionalidades.

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

As listagens usarao paginas de 10 itens por padrao, com limite de 50.

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

Ainda nao ha endpoints: uma resposta 404 na raiz e esperada nesta etapa.
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
positivos. Os Controllers usarao `@Valid` ao receber esses DTOs nas proximas tasks.
As regras de negocio continuam nos Services.

Os erros retornam `timestamp`, `status`, `mensagem` e `caminho`. Dados invalidos
retornam 400; recursos inexistentes, 404; conflitos de negocio ou integridade, 409.
As excecoes dos Services serao `RecursoNaoEncontradoException` e `RegraNegocioException`.

Para executar apenas os testes de validacao e tratamento de erros, sem banco:

```powershell
.\mvnw.cmd "-Dtest=ValidacaoDtosTests,ApiExceptionHandlerTests" test
```

Os Controllers usados nesses testes existem somente nos arquivos de teste.
