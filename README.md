# Partner Hub

Hub de integração com SAP, construído com CAP Java 5 (Spring Boot 4) e, nas próximas fases, com uma camada de IA.

## Estado atual

- Modelo CDS: Partners (parceiros de negócio) e Invoices (faturas), ligados por composição
- Serviço OData V4 com CRUD automático, visão OpenInvoices e anotações de interface
- Dados de teste carregados por CSV, banco H2 em memória

## Como rodar

Requisitos: JDK 21+, Maven 3.9+, Node 22+ e @sap/cds-dk.

    cd hub
    mvn spring-boot:run

Abrir http://localhost:8080 para ver os serviços e o Fiori preview.

## Regras de negócio (Java)

Handlers em `hub/srv/src/main/java/customer/hub/handlers/`:

- `@Before` em CREATE de Partners: valida `partnerNumber` (7 a 10 dígitos) e remove espaços do nome
- `@On` na ação `blockPartner`: bloqueia um parceiro pelo número e devolve um resultado tipado

### Exemplos

    curl -X POST http://127.0.0.1:8080/odata/v4/PartnerService/blockPartner \
      -H "Content-Type: application/json" -d '{"partnerNumber":"1000001"}'

Respostas verificadas: 400 para número inválido, 404 para parceiro inexistente.

## Segurança

Autorização declarada em `hub/srv/authorization.cds` com `@restrict` e `@requires`. Modo de autenticação `model-strict` (o que não está liberado exige login).

Usuários de teste (apenas ambiente local): `viewer`, `manager` e `admin` (a senha é igual ao nome).

| Papel | Permissões |
|---|---|
| Viewer | Ler parceiros e faturas |
| Manager | Ler, criar e alterar |
| Admin | Tudo, inclusive apagar e `blockPartner` |

    curl -u admin:admin http://127.0.0.1:8080/odata/v4/PartnerService/Partners

Os usuários de teste valem só localmente. Na BTP quem autentica é o XSUAA ou o IAS, e as anotações de autorização continuam as mesmas.

## Testes

Testes de integração em `hub/srv/src/test/java/customer/hub/PartnerServiceTest.java`: sobem a aplicação numa porta aleatória e chamam a API com Basic Auth, usando os usuários de teste.

    cd hub
    mvn test

Cobrem autenticação (401), autorização por papel (403), validações (400), a ação `blockPartner`, a composição (apagar o parceiro apaga as faturas) e a visão `OpenInvoices`.
