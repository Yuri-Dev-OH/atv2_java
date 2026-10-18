# Requisitos da atividade ATVII

Fonte: `atvii-autoboots-micro-servico-java.pdf`, duas páginas, incluindo as figuras 1 (níveis RMM) e 2 (QR code do repositório).

## Escopo

O automanager, já com todos os CRUDs (ATVI), deve atingir os quatro níveis do Richardson Maturity Model, "utilizando todos os recursos necessários e disponíveis nos frameworks e tecnologias utilizadas". O PDF não numera tarefas: a atividade única é implementar os níveis de maturidade.

| Nível | Texto da Figura 1 |
|---|---|
| 0 | Uso do protocolo HTTP |
| 1 | URI única por recurso |
| 2 | Uso dos métodos e suas semânticas |
| 3 | HATEOAS |

O PDF indica como ponto de partida o repositório https://github.com/gerson-pn/atviiautobots-microservico-spring (link quebrado em duas linhas no PDF; endereço correto: `gerson-pn/atvii-autobots-microservico-spring`). Ele usa `spring-boot-starter-hateoas` e links HATEOAS somente no cliente, gerados por `AdicionadorLinkCliente` (ver [origem.md](origem.md)).

O PDF não pede interface gráfica, autenticação, novos microsserviços nem meta de cobertura.

## Verificação da entrega

| Requisito | Implementação | Verificação |
|---|---|---|
| Nível 0: HTTP | Spring MVC, JSON sobre HTTP | Todos os testes MockMvc e `scripts/demo.ps1` |
| Nível 1: URI por recurso | Cliente, endereço, documento e telefone com URIs próprias e hierárquicas | `CrudIntegrationTest` |
| Nível 2: métodos e semântica | GET, POST (`201` com `Location` absoluto), PUT, DELETE (`204`); `400`, `404`, `405` com `Allow`, `406`, `409`, `415`; HEAD e OPTIONS; erros em Problem Details | `MaturidadeRestIntegrationTest.metodosEMidiasSeguemASemanticaHttp`, `errosUsamProblemDetail`, `locationDaCriacaoApontaParaOProprioRecurso` |
| Nível 3: HATEOAS | `spring-boot-starter-hateoas`; `AdicionadorLinkCliente`, `AdicionadorLinkEndereco`, `AdicionadorLinkDocumento` e `AdicionadorLinkTelefone`; raiz `/`; HAL e HAL-FORMS | `raizEhOPontoDeEntradaDaApi`, `clienteCompletoEhGerenciadoApenasSeguindoLinks`, `paginacaoOfereceLinksDeNavegacao`, `halFormsAnunciaAcoesConformeOEstadoDoCliente`, `halFormsDescreveCamposDasEntradas` |
| Recursos do framework | Spring HATEOAS (`EntityModel`, `CollectionModel`, `PagedModel`, `WebMvcLinkBuilder`, affordances), `ProblemDetail` do Spring 6, validação Jakarta | Código em `modelo/`, `controles/`, `erros/` e `configuracao/` |
| CRUDs preservados | Serviços, validações e regras da ATVI sem alteração de comportamento | `CrudIntegrationTest` e `PersistenciaIntegrationTest` |

Em 18/10/2026, no Windows com Java 21:

- `verify`: **47 testes, zero falhas, zero erros, nenhum ignorado; BUILD SUCCESS**.
- Cobertura JaCoCo: **275 de 278 linhas (98,9%)** e **34 de 36 ramificações (94,4%)**.
- JAR iniciado na porta 18082 com banco temporário. `scripts/demo.ps1` (Windows PowerShell 5.1) navegou a partir da raiz somente por links, executou os quatro CRUDs, conferiu HAL-FORMS, `HEAD` e `405`, e removeu o cliente criado.

## Decisões em relação à base do professor

- **Status de sucesso:** a base retorna `302 FOUND` em consultas e `404` para lista vazia. `302` indica redirecionamento e exigiria `Location`; isso contraria o nível 2. A entrega usa `200 OK` e devolve a lista vazia com seus links.
- **Onde ficam os links:** na base, a entidade JPA estende `RepresentationModel`. Aqui, os links envolvem os DTOs de resposta (`EntityModel`), o que mantém a separação entre persistência e representação feita na ATVI e evita `LazyInitializationException` com `open-in-view=false`.
- **Nomes:** as classes `AdicionadorLink*` seguem o vocabulário da base, mas devolvem um novo modelo em vez de alterar o objeto recebido.
- **HAL-FORMS:** os records de entrada não têm setters, e o Spring HATEOAS marcaria todos os campos como `readOnly`. `HipermidiaConfiguracao` omite esse atributo, que por especificação vale `false`. Textos usam `@Length` em vez de `@Size` para que o template publique `maxLength` em vez de `type: range`.

## Inspeção do PDF

O texto das duas páginas e as figuras foram examinados. Não foram encontradas instruções dirigidas à IA, ações JavaScript, arquivos incorporados ou comandos de execução. Essa inspeção não constitui uma análise antivírus completa.
