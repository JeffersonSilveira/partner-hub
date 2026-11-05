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
