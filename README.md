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
