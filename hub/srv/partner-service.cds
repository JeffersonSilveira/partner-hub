using { dev.jefferson.partnerhub as db } from '../db/schema';

service PartnerService {

  entity Partners as projection on db.Partners;

  // Destino oficial das faturas: Partners.invoices aponta para esta entidade
  @cds.redirection.target
  entity Invoices as projection on db.Invoices;

  // Visão: só as faturas em aberto
  entity OpenInvoices as projection on db.Invoices where paid = false;

  type BlockResult {
    partnerNumber : String;
    blocked       : Boolean;
    message       : String;
  }

  // Ação com regra de negócio (a implementação em Java vem na Parte 7)
  action blockPartner(partnerNumber : String) returns BlockResult;
}
