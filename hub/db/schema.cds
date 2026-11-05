using { cuid, managed } from '@sap/cds/common';

namespace dev.jefferson.partnerhub;

type Category : String enum {
  PERSON;
  ORGANIZATION;
  GROUP;
}

entity Partners : cuid, managed {
  partnerNumber : String(10)  @mandatory;
  name          : String(100) @mandatory;
  category      : Category default #ORGANIZATION;
  country       : String(2);
  blocked       : Boolean default false;
  invoices      : Composition of many Invoices on invoices.partner = $self;
}

entity Invoices : cuid, managed {
  partner  : Association to Partners;
  amount   : Decimal(15,2);
  currency : String(3);
  dueDate  : Date;
  paid     : Boolean default false;
}