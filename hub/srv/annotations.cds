using PartnerService as service from './partner-service';

annotate service.Partners with @(
  UI.LineItem: [
    { Value: partnerNumber },
    { Value: name },
    { Value: category },
    { Value: country },
    { Value: blocked }
  ]
);

annotate service.Invoices with @(
  UI.LineItem: [
    { Value: partner_ID },
    { Value: amount },
    { Value: currency },
    { Value: dueDate },
    { Value: paid }
  ]
);
