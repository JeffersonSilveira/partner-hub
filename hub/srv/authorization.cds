using PartnerService as service from './partner-service';

annotate service.Partners with @(restrict: [
  { grant: 'READ',               to: ['Viewer', 'Manager', 'Admin'] },
  { grant: ['CREATE', 'UPDATE'], to: ['Manager', 'Admin'] },
  { grant: 'DELETE',             to: 'Admin' }
]);

annotate service.Invoices with @(restrict: [
  { grant: 'READ',                        to: ['Viewer', 'Manager', 'Admin'] },
  { grant: ['CREATE', 'UPDATE', 'DELETE'], to: ['Manager', 'Admin'] }
]);

annotate service.OpenInvoices with @(restrict: [
  { grant: 'READ', to: ['Viewer', 'Manager', 'Admin'] }
]);

annotate service.blockPartner with @(requires: 'Admin');
