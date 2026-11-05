package customer.hub.handlers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sap.cds.Result;
import com.sap.cds.ql.Update;
import com.sap.cds.services.ErrorStatuses;
import com.sap.cds.services.ServiceException;
import com.sap.cds.services.cds.CqnService;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.Before;
import com.sap.cds.services.handler.annotations.On;
import com.sap.cds.services.handler.annotations.ServiceName;
import com.sap.cds.services.handler.annotations.After;
import com.sap.cds.services.persistence.PersistenceService;

import cds.gen.partnerservice.BlockPartnerContext;
import cds.gen.partnerservice.BlockResult;
import cds.gen.partnerservice.PartnerService_;
import cds.gen.partnerservice.Partners;
import cds.gen.partnerservice.Partners_;
import cds.gen.partnerservice.Invoices;
import cds.gen.partnerservice.Invoices_;
import cds.gen.partnerservice.MarkInvoicePaidContext;
import cds.gen.partnerservice.PaidResult;

@Component
@ServiceName(PartnerService_.CDS_NAME)
public class PartnerServiceHandler implements EventHandler {

    private static final String DB_PARTNERS = "dev.jefferson.partnerhub.Partners";

        private static final String DB_INVOICES = "dev.jefferson.partnerhub.Invoices";

    @Autowired
    private PersistenceService db;

    // ANTES de criar um parceiro: validar e normalizar
    @Before(event = CqnService.EVENT_CREATE, entity = Partners_.CDS_NAME)
    public void validateAndNormalize(List<Partners> partners) {
        for (Partners p : partners) {
            String number = p.getPartnerNumber();
            if (number != null && !number.matches("\\d{7,10}")) {
                throw new ServiceException(ErrorStatuses.BAD_REQUEST,
                        "partnerNumber deve ter de 7 a 10 dígitos");
            }
            if (p.getName() != null) {
                p.setName(p.getName().trim());
            }
        }
    }

    // Implementação da ação blockPartner
    @On(event = BlockPartnerContext.CDS_NAME)
    public void blockPartner(BlockPartnerContext context) {
        String number = context.getPartnerNumber();

        Result result = db.run(Update.entity(DB_PARTNERS)
                .where(p -> p.get("partnerNumber").eq(number))
                .data("blocked", true));

        if (result.rowCount() == 0) {
            throw new ServiceException(ErrorStatuses.NOT_FOUND,
                    "Parceiro {} não encontrado", number);
        }

        BlockResult res = BlockResult.create();
        res.setPartnerNumber(number);
        res.setBlocked(true);
        res.setMessage("Parceiro bloqueado com sucesso");
        context.setResult(res);
    }

        // DEPOIS de ler parceiros: marcar os bloqueados no nome (só na resposta)
    @After(event = CqnService.EVENT_READ, entity = Partners_.CDS_NAME)
    public void markBlocked(List<Partners> partners) {
        for (Partners p : partners) {
            if (Boolean.TRUE.equals(p.getBlocked()) && p.getName() != null) {
                p.setName(p.getName() + " (BLOQUEADO)");
            }
        }
    }

        // ANTES de criar uma fatura: o valor não pode ser negativo
    @Before(event = CqnService.EVENT_CREATE, entity = Invoices_.CDS_NAME)
    public void validateInvoices(List<Invoices> invoices) {
        for (Invoices i : invoices) {
            if (i.getAmount() != null && i.getAmount().signum() < 0) {
                throw new ServiceException(ErrorStatuses.BAD_REQUEST,
                        "O valor da fatura não pode ser negativo");
            }
        }
    }

        // Implementação da ação markInvoicePaid
    @On(event = MarkInvoicePaidContext.CDS_NAME)
    public void markInvoicePaid(MarkInvoicePaidContext context) {
        String id = context.getInvoiceID();

        Result result = db.run(Update.entity(DB_INVOICES)
                .where(i -> i.get("ID").eq(id))
                .data("paid", true));

        if (result.rowCount() == 0) {
            throw new ServiceException(ErrorStatuses.NOT_FOUND,
                    "Fatura {} não encontrada", id);
        }

        PaidResult res = PaidResult.create();
        res.setInvoiceID(id);
        res.setPaid(true);
        context.setResult(res);
    }
}
