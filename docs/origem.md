# Origem do projeto

## Base direta: entrega da ATVI

Esta entrega continua o projeto https://github.com/Yuri-Dev-OH/atv1_java (commit `0f6a452`), que completou os CRUDs de cliente, endereço, documento e telefone sobre a base do professor da ATVI (https://github.com/gerson-pn/atvi-autobots-microservico-spring, commit `d5bdb729abee6e6d39b9e23aed202ebc66912ce9`).

Já vinham da ATVI: Spring Boot 3.5 e Java 21, DTOs validados, serviços transacionais, Flyway, H2 em arquivo, bloqueio pessimista por cliente e verificação de pertencimento nas URLs.

## Referência da ATVII

Repositório indicado no PDF: https://github.com/gerson-pn/atvii-autobots-microservico-spring (branch `master`, commit único, consultado em 18/10/2026).

O que essa base contém:

- Spring Boot 2.6.4, Java 17 e `spring-boot-starter-hateoas`.
- A entidade `Cliente` estende `RepresentationModel<Cliente>`.
- A interface `AdicionadorLink<T>` e o componente `AdicionadorLinkCliente`, que usa `WebMvcLinkBuilder.linkTo(methodOn(...))`.
- Links somente para o cliente: `self` nos itens da lista e `clientes` no item individual.
- Consultas retornando `302 FOUND`, e `404` para lista vazia.

## Alterações nesta entrega

- `spring-boot-starter-hateoas` adicionado; HAL e HAL-FORMS habilitados.
- `AdicionadorLinkCliente`, `AdicionadorLinkEndereco`, `AdicionadorLinkDocumento` e `AdicionadorLinkTelefone` no pacote `modelo`, com links e affordances para todos os recursos.
- `RaizControle` (`GET /`) como ponto de entrada.
- A listagem de clientes passou a usar `PagedModel` com links `first`, `prev`, `next` e `last`, mantendo os parâmetros `pagina` e `tamanho` e sua validação.
- `Location` das criações passou a ser absoluto e igual ao link `self`.
- Erros passaram a usar Problem Details (`application/problem+json`), inclusive os erros do próprio Spring MVC (405, 406 e 415).
- Coleções no formato HAL (`_embedded`), com testes e `scripts/demo.ps1` atualizados.

As justificativas das diferenças em relação à base da ATVII estão em [requisitos.md](requisitos.md#decisões-em-relação-à-base-do-professor).
