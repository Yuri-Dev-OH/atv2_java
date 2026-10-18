# AutoBots — automanager com níveis de maturidade REST (ATVII)

API Java/Spring de cadastro de clientes, endereços, documentos e telefones. Esta versão cobre os **quatro níveis do Richardson Maturity Model (RMM)** pedidos em `atvii-autoboots-micro-servico-java.pdf`, incluindo **HATEOAS** com HAL e HAL-FORMS. A origem está em [docs/origem.md](docs/origem.md), e o escopo e a verificação em [docs/requisitos.md](docs/requisitos.md).

## Executar

É necessário **JDK 21**, com `JAVA_HOME` configurado. O projeto já inclui o Maven Wrapper; não é preciso instalar Maven, Docker ou banco separado. A primeira compilação precisa de internet para baixar as dependências.

No PowerShell, na pasta deste README:

```powershell
.\mvnw.cmd "-Dmaven.repo.local=.tools/m2" verify
java -jar target/automanager-2.0.0.jar
```

No Git Bash ou Linux:

```bash
sh mvnw -Dmaven.repo.local=.tools/m2 verify
java -jar target/automanager-2.0.0.jar
```

A API fica em **http://localhost:8081/** (ponto de entrada HATEOAS). O banco H2 é gravado em `dados/` e permanece após reiniciar. Para outra porta: `java -jar target/automanager-2.0.0.jar --server.port=8082`.

Opcionalmente, com Docker: `docker compose up --build -d`.

## Testar

### 1. Testes automatizados

```powershell
.\mvnw.cmd "-Dmaven.repo.local=.tools/m2" verify
```

O resultado esperado é **BUILD SUCCESS**, com 47 testes, zero falhas e zero erros. A cobertura fica em `target/site/jacoco/index.html`. Para rodar só os testes de maturidade REST:

```powershell
.\mvnw.cmd "-Dmaven.repo.local=.tools/m2" test "-Dtest=MaturidadeRestIntegrationTest"
```

| Classe | O que verifica |
|---|---|
| `CrudIntegrationTest` | Níveis 0 e 1, os quatro CRUDs, validação, duplicidades, pertencimento, rollback e concorrência |
| `MaturidadeRestIntegrationTest` | Nível 2 (405, 415, 406, HEAD, OPTIONS, `Location`, Problem Details) e nível 3 (raiz, navegação só por links, paginação, HAL-FORMS) |
| `PersistenciaIntegrationTest` | Dados preservados após fechar e reabrir o banco em arquivo |

### 2. Demonstração HTTP real

Com a API aberta, em outro PowerShell:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\demo.ps1
```

O script conhece **apenas a raiz** `http://localhost:8081/`. Todas as demais URIs são obtidas dos `_links` das respostas. Ele executa os quatro CRUDs, confere os templates HAL-FORMS, `HEAD` e `405`, e remove o cliente criado. Para outra porta: `-BaseUrl http://localhost:8082`.

### 3. Manualmente (curl)

```bash
curl http://localhost:8081/
curl -i -X POST http://localhost:8081/clientes -H "Content-Type: application/json" \
     -d '{"nome":"Maria Silva","dataNascimento":"1995-04-10"}'
curl http://localhost:8081/clientes/1
curl -H "Accept: application/prs.hal-forms+json" http://localhost:8081/clientes/1
curl "http://localhost:8081/clientes?pagina=0&tamanho=5"
curl -i -X PATCH http://localhost:8081/clientes/1
```

No PowerShell, use `curl.exe` em vez de `curl`, que é um alias de `Invoke-WebRequest`.

## Níveis de maturidade (RMM)

| Nível | Requisito | Implementação |
|---|---|---|
| 0 | Uso do protocolo HTTP | API HTTP/JSON com Spring MVC |
| 1 | URI única por recurso | `/clientes/{id}`, `/clientes/{id}/endereco`, `/clientes/{id}/documentos/{documentoId}`, `/clientes/{id}/telefones/{telefoneId}` |
| 2 | Métodos e suas semânticas | `GET` seguro, `PUT` idempotente, `POST` cria com `201` e `Location` absoluto, `DELETE` responde `204`. Status `400`, `404`, `405` (com `Allow`), `406`, `409` e `415`. `HEAD` e `OPTIONS` automáticos. Erros em `application/problem+json` (RFC 9457) |
| 3 | HATEOAS | Todas as respostas trazem `_links` (HAL). A raiz `/` é o ponto de entrada. Coleções têm links de navegação e de paginação. Com `Accept: application/prs.hal-forms+json`, as respostas trazem `_templates` com método HTTP e campos das ações permitidas |

### Exemplo de cliente (HAL)

```json
{
  "id": 1, "nome": "Maria Silva", "nomeSocial": null, "dataNascimento": "1995-04-10",
  "dataCadastro": "2026-10-18T19:06:40.265137Z", "endereco": null, "documentos": [], "telefones": [],
  "_links": {
    "self":       { "href": "http://localhost:8081/clientes/1" },
    "clientes":   { "href": "http://localhost:8081/clientes{?pagina,tamanho}", "templated": true },
    "endereco":   { "href": "http://localhost:8081/clientes/1/endereco" },
    "documentos": { "href": "http://localhost:8081/clientes/1/documentos" },
    "telefones":  { "href": "http://localhost:8081/clientes/1/telefones" }
  }
}
```

### Links por recurso

| Recurso | Relações | Templates HAL-FORMS |
|---|---|---|
| Raiz `/` | `self`, `clientes` (template) | `default`: POST cliente |
| Página de clientes | `self`, `first`, `prev`, `next`, `last`; `page` com totais | `default`: POST cliente |
| Cliente | `self`, `clientes`, `endereco`, `documentos`, `telefones` | `default`: PUT, `excluir`: DELETE e, **somente sem endereço**, `cadastrar`: POST endereço |
| Endereço | `self`, `cliente` | `default`: PUT, `excluir`: DELETE |
| Lista de documentos ou telefones | `self`, `cliente` | `default`: POST |
| Documento ou telefone | `self`, a coleção (`documentos` ou `telefones`), `cliente` | `default`: PUT, `excluir`: DELETE |

Listas vazias seguem o padrão do HAL: trazem `_links`, mas não `_embedded`.

### Erros (Problem Details)

```json
{ "type": "about:blank", "title": "Recurso não encontrado.", "status": 404,
  "detail": "Cliente não encontrado.", "instance": "/clientes/999" }
```

Erros de validação acrescentam `detalhes`, com uma mensagem por campo.

## Rotas

| Método | Caminho | Operação |
|---|---|---|
| GET | `/` | Ponto de entrada com links |
| POST | `/clientes` | Cadastrar cliente, opcionalmente com endereço, documentos e telefones |
| GET | `/clientes?pagina=0&tamanho=20` | Listar clientes paginados; tamanho de 1 a 100 |
| GET | `/clientes/{id}` | Consultar cliente e seus dados |
| PUT | `/clientes/{id}` | Atualizar nome, nome social e nascimento |
| DELETE | `/clientes/{id}` | Excluir cliente e todos os dados vinculados |
| POST, GET, PUT, DELETE | `/clientes/{clienteId}/endereco` | CRUD do endereço (POST cadastra) |
| POST, GET | `/clientes/{clienteId}/documentos` | Cadastrar e listar documentos |
| GET, PUT, DELETE | `/clientes/{clienteId}/documentos/{id}` | Consultar, atualizar e excluir documento |
| POST, GET | `/clientes/{clienteId}/telefones` | Cadastrar e listar telefones |
| GET, PUT, DELETE | `/clientes/{clienteId}/telefones/{id}` | Consultar, atualizar e excluir telefone |

## Regras

- IDs e data de cadastro são gerados pelo servidor. Campos desconhecidos são rejeitados, inclusive `id`, `dataCadastro` e `_links` enviados no corpo.
- Nascimento usa `AAAA-MM-DD` e não pode estar no futuro. Nome e nascimento são obrigatórios.
- Um cliente tem no máximo um endereço e pode ter vários documentos e telefones. Cidade, rua e número são obrigatórios no endereço. O CEP, quando informado, tem oito dígitos.
- O número do documento é único em todo o cadastro. O mesmo telefone pode existir em clientes diferentes, mas não pode se repetir no mesmo cliente.
- `PUT` substitui os campos do recurso; campos opcionais omitidos são limpos. Atualizar o cliente não altera seus contatos.
- Excluir um contato preserva o cliente. Excluir o cliente remove tudo na mesma transação.
- Uma URL não permite manipular documento ou telefone de outro cliente. Isso não substitui autenticação, que o PDF não pede.
- Os links são gerados a partir do host da requisição. Atrás de um proxy reverso confiável, defina `FORWARD_HEADERS_STRATEGY=framework` para que os links usem o host público. O padrão é `none`, para não confiar em cabeçalhos `X-Forwarded-*` enviados diretamente por clientes.

## Organização

```text
src/main/java/com/autobots/automanager/
  controles/     rotas HTTP; RaizControle é o ponto de entrada HATEOAS
  modelo/        AdicionadorLink* (links e affordances), selecionador e atualizadores
  dto/           entradas validadas e respostas (com @Relation para o HAL)
  entidades/     as quatro entidades JPA
  servicos/      transações e regras de cadastro
  repositorios/  acesso ao banco
  erros/         Problem Details (RFC 9457)
  configuracao/  JSON estrito e suporte a HAL e HAL-FORMS
src/main/resources/db/migration/  esquema versionado com Flyway
src/test/                        testes automatizados
scripts/demo.ps1                 demonstração HTTP navegando por links
```

O diagrama está em [docs/diagrama.puml](docs/diagrama.puml).
