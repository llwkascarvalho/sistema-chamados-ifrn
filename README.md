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

A estrutura inicial esta pronta. Os cadastros, as regras de negocio, as migrations
com dados iniciais e a execucao com Docker Compose serao implementados nas proximas etapas.
Ainda nao ha endpoints disponiveis.

## Organizacao

Os pacotes ficam em `src/main/java/br/edu/ifrn/chamados`:

| Pacote | Responsabilidade |
| --- | --- |
| `controller` | Receber requisicoes, chamar o Service e devolver respostas. |
| `service` | Aplicar as regras de negocio. |
| `repository` | Acessar o banco com Spring Data JPA. |
| `model` | Representar entidades e enums. |

As dependencias entre classes serao recebidas pelo construtor, sem `@Autowired` em atributos.
Os arquivos `package-info.java` documentam os pacotes enquanto as classes ainda nao existem.

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
`ddl-auto=validate`, portanto nao cria nem altera tabelas. As migrations ainda
serao adicionadas na etapa de persistencia; esta etapa inicial nao valida a
inicializacao completa da aplicacao.

As listagens usarao paginas de 10 itens por padrao, com limite de 50.

## Desenvolvimento

Usaremos uma branch por task e commits pequenos, agrupados por mudanca com sentido.
Ao concluir e conferir a task, abriremos um Pull Request para a `main`.
Nao e necessario criar uma branch `develop` neste projeto individual.

Exemplos de branches: `chore/estrutura-inicial`, `feat/persistencia` e `feat/cadastros`.
Exemplos de commits: `feat: adiciona entidade Setor` e `fix: impede email duplicado`.

A entrega final sera a versao disponivel na `main`, com Docker Compose,
exemplos de requisicao e as orientacoes completas de execucao.
