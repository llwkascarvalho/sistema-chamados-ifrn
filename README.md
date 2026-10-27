# Sistema de Chamados do IFRN

API REST da disciplina de Desenvolvimento de Sistemas Corporativos (DSC) para
registrar pedidos de suporte, atribuir técnicos e acompanhar o atendimento.

Java 21, Spring Boot 4, Maven, Spring Web, JPA, Validation, PostgreSQL, Flyway e
Docker Compose. O projeto também usa Lombok.

## Executar

Com o Docker Desktop iniciado:

```bash
git clone https://github.com/llwkascarvalho/sistema-chamados-ifrn.git
cd sistema-chamados-ifrn
docker compose up
```

A primeira execução baixa as imagens e as dependências. A API aguarda o banco
ficar saudável, aplica as migrations e responde em `http://localhost:8080`.
Não é necessário criar `.env`, banco ou tabelas manualmente. A porta 8080 deve
estar livre.

Após a inicialização, confira em outro terminal:

```powershell
Invoke-RestMethod http://localhost:8080/setores
```

O PostgreSQL fica na rede interna do Compose e usa um volume persistente.
`docker compose down` encerra os containers e mantém os dados. Para reconstruir
a imagem após alterar o código, use `docker compose up --build`.

## Requisições

O arquivo [requisicoes.http](requisicoes.http) contém todas as 23 rotas, fluxos
válidos e exemplos de erro. No VS Code, use a extensão REST Client e execute na
ordem do arquivo. Os títulos indicam os erros esperados. Os cadastros e chamados
desses exemplos ficam no banco; o email de teste usa um identificador gerado
para permitir uma nova execução.

Os IDs de usuários e setor usados nos exemplos vêm da migration inicial:
solicitantes 1 e 2; técnicos 3 e 4, dos setores 1 e 2, respectivamente.
Ajuste as variáveis do início do arquivo se esses registros forem alterados.

| Recurso | Rotas |
| --- | --- |
| Setores | POST e GET `/setores`; GET, PUT e DELETE `/setores/{id}` |
| Categorias | POST e GET `/categorias`; GET, PUT e DELETE `/categorias/{id}` |
| Categorias de um setor | GET `/setores/{id}/categorias` |
| Usuários | POST e GET `/usuarios`; GET e PUT `/usuarios/{id}` |
| Abertura e consulta de chamados | POST e GET `/chamados`; GET `/chamados/{id}` |
| Chamados do usuário | GET `/usuarios/{id}/chamados` |
| Assumir | PATCH `/chamados/{id}/assumir` |
| Resolver | PATCH `/chamados/{id}/resolver` |
| Cancelar | PATCH `/chamados/{id}/cancelar` |
| Interações | POST `/chamados/{id}/interacoes` |

POST retorna 201; consultas e atualizações retornam 200; exclusões retornam 204.
O detalhe do chamado inclui suas interações. As transições permitidas são
`ABERTO → EM_ATENDIMENTO → RESOLVIDO` e `ABERTO → CANCELADO`.
Não há autenticação: o corpo informa `solicitanteId`, `tecnicoId` ou `autorId`.

## Paginação e filtros

Todas as listagens aceitam `page`, `size` e `sort`. A página inicial é 0, o tamanho
padrão é 10 e o máximo é 50. A resposta inclui `content`, `number`, `size`,
`totalElements` e `totalPages`.

| Listagem | Filtros opcionais |
| --- | --- |
| `/setores` | `nome` |
| `/categorias` | `nome`, `setorId` |
| `/setores/{id}/categorias` | `nome` |
| `/usuarios` | `nome`, `tipo`, `setorId` |
| `/chamados` | `status`, `prioridade`, `setorId`, `categoriaId`, `solicitanteId`, `tecnicoId`, `dataInicio`, `dataFim` |
| `/usuarios/{id}/chamados` | `status` |

Os filtros se combinam; os omitidos são ignorados. A busca por nome usa trecho
e não diferencia maiúsculas de minúsculas. As datas usam formato ISO com horário,
como `2026-10-01T00:00:00`; ambos os limites são inclusivos e podem ser usados
separadamente. Um início posterior ao fim retorna 400.

Prioridades: `BAIXA`, `MEDIA`, `ALTA`. Status: `ABERTO`, `EM_ATENDIMENTO`,
`RESOLVIDO`, `CANCELADO`. Tipos de usuário: `SOLICITANTE`, `TECNICO`.
A consulta por usuário retorna os chamados abertos pelo solicitante ou os
atribuídos ao técnico.

```http
GET http://localhost:8080/chamados?status=ABERTO&setorId=1&prioridade=ALTA&page=0&size=10&sort=dataAbertura,desc
GET http://localhost:8080/usuarios/1/chamados?status=RESOLVIDO
```

## Erros e regras

Os erros retornam `timestamp`, `status`, `mensagem` e `caminho`:
400 para dados inválidos, 404 para recurso inexistente e 409 para conflitos de
negócio ou integridade, como email duplicado ou técnico de outro setor.

Um chamado nasce ABERTO, com data do sistema e prioridade MEDIA quando omitida.
Somente solicitantes podem abrir chamados; o técnico deve pertencer ao setor da
categoria para assumir e somente o atribuído pode resolver, informando a solução.
O próprio solicitante pode cancelar enquanto ABERTO. Apenas os participantes
registram interações; chamados encerrados bloqueiam novas ações e interações.

Setores e categorias com chamados não podem ser excluídos. Setores também exigem
que categorias e usuários sejam removidos ou reatribuídos antes da exclusão,
preservando os vínculos do banco.

## Organização e banco

Os pacotes `controller`, `service`, `repository` e `model` separam as camadas.
As regras ficam nos Services, a injeção usa construtores e os DTOs em `record`
representam entradas e saídas. O `@RestControllerAdvice` padroniza os erros.

A conexão vem de `DB_URL`, `DB_USERNAME` e `DB_PASSWORD`. O Compose fornece
valores locais de exemplo. O PostgreSQL do Compose usa um volume próprio,
separado do banco de desenvolvimento usado nos testes abaixo.

As migrations ficam em `src/main/resources/db/migration`. V1 cria as cinco
tabelas; V2 insere dois setores, quatro categorias e quatro usuários.
O Flyway registra versões e checksums em `flyway_schema_history`. Alterar uma
migration aplicada muda seu checksum e faz a validação falhar; correções entram
em uma nova versão. O Hibernate usa `ddl-auto=validate` para conferir o esquema.
`ddl-auto=update` não substitui migrations em produção, pois não mantém um
histórico SQL revisado e versionado das alterações.

## Testes (opcional)

Requer JDK 21. Para executar os testes sem banco no PowerShell:

```powershell
.\mvnw.cmd "-Dtest=*ServiceTests,*ControllerTests,ValidacaoDtosTests,ApiExceptionHandlerTests" test
```

Para a suíte completa, crie um PostgreSQL de desenvolvimento uma vez:

```powershell
docker run --name chamados-postgres -e POSTGRES_DB=chamados -e POSTGRES_USER=chamados -e POSTGRES_PASSWORD=chamados_local -p 5432:5432 -d postgres:17
```

Se ele já existir, use `docker start chamados-postgres`. Aguarde
`docker exec chamados-postgres pg_isready -U chamados -d chamados` indicar
`accepting connections` e execute na mesma sessão:

```powershell
$env:DB_URL = "jdbc:postgresql://localhost:5432/chamados"
$env:DB_USERNAME = "chamados"
$env:DB_PASSWORD = "chamados_local"
.\mvnw.cmd clean verify
```

Os testes de persistência desfazem os registros criados ao terminar. No Linux ou
macOS, use `./mvnw` e exporte as mesmas variáveis. O build da imagem Docker usa
`-DskipTests`, pois a suíte com PostgreSQL é executada separadamente.
