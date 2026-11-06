package customer.hub;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PartnerServiceTest {

    private static final Pattern ID_PATTERN = Pattern.compile("\"ID\":\"([^\"]+)\"");

    @Value("${local.server.port}")
    private int port;

    private final HttpClient client = HttpClient.newHttpClient();

    // ---------- utilitários ----------

    // Chama o serviço. user == null significa "sem credenciais".
    // Nos usuários de teste, a senha é igual ao nome.
    private HttpResponse<String> call(String method, String path, String user, String body)
            throws Exception {
        HttpRequest.Builder builder = HttpRequest
                .newBuilder(URI.create("http://localhost:" + port
                        + "/odata/v4/PartnerService" + path))
                .header("Content-Type", "application/json");
        if (user != null) {
            String token = Base64.getEncoder()
                    .encodeToString((user + ":" + user).getBytes(StandardCharsets.UTF_8));
            builder.header("Authorization", "Basic " + token);
        }
        HttpRequest.BodyPublisher publisher = (body == null)
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body);
        return client.send(builder.method(method, publisher).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    // Número de parceiro válido (10 dígitos) e diferente a cada teste
    private String uniqueNumber() {
        return String.valueOf(1_000_000_000L + (System.nanoTime() % 900_000_000L));
    }

    // Todos os "ID" encontrados no JSON, na ordem em que aparecem
    private List<String> ids(String json) {
        List<String> result = new ArrayList<>();
        Matcher m = ID_PATTERN.matcher(json);
        while (m.find()) {
            result.add(m.group(1));
        }
        return result;
    }

    // Cria um parceiro como manager e devolve o corpo da resposta
    private String createPartner(String number, String name) throws Exception {
        HttpResponse<String> r = call("POST", "/Partners", "manager",
                "{\"partnerNumber\":\"" + number + "\",\"name\":\"" + name + "\"}");
        assertThat(r.statusCode()).isEqualTo(201);
        return r.body();
    }

    // ---------- autenticação e autorização ----------

    @Test
    void anonymousIsRejected() throws Exception {
        assertThat(call("GET", "/Partners", null, null).statusCode()).isEqualTo(401);
    }

    @Test
    void viewerCanRead() throws Exception {
        HttpResponse<String> r = call("GET", "/Partners", "viewer", null);
        assertThat(r.statusCode()).isEqualTo(200);
        assertThat(r.body()).contains("ACME Ltda");
    }

    @Test
    void viewerCannotCreate() throws Exception {
        HttpResponse<String> r = call("POST", "/Partners", "viewer",
                "{\"partnerNumber\":\"1000030\",\"name\":\"Por Viewer\"}");
        assertThat(r.statusCode()).isEqualTo(403);
    }

    @Test
    void managerCreatesAndIsRecordedAsCreator() throws Exception {
        String body = createPartner(uniqueNumber(), "Criado por Manager");
        assertThat(body).contains("\"createdBy\":\"manager\"");
    }

    @Test
    void onlyAdminCanDelete() throws Exception {
        String id = ids(createPartner(uniqueNumber(), "Para apagar")).get(0);
        assertThat(call("DELETE", "/Partners(" + id + ")", "manager", null).statusCode())
                .isEqualTo(403);
        assertThat(call("DELETE", "/Partners(" + id + ")", "admin", null).statusCode())
                .isEqualTo(204);
    }

    // ---------- regras de negócio (handlers) ----------

    @Test
    void invalidPartnerNumberIsRejectedByHandler() throws Exception {
        HttpResponse<String> r = call("POST", "/Partners", "manager",
                "{\"partnerNumber\":\"12\",\"name\":\"Teste\"}");
        assertThat(r.statusCode()).isEqualTo(400);
        assertThat(r.body()).contains("7 a 10 dígitos");
    }

    @Test
    void nameIsTrimmed() throws Exception {
        String body = createPartner(uniqueNumber(), "   Teste Ltda   ");
        assertThat(body).contains("\"name\":\"Teste Ltda\"");
    }

    @Test
    void blockPartnerRequiresAdminAndBlocks() throws Exception {
        String number = uniqueNumber();
        String id = ids(createPartner(number, "Para bloquear")).get(0);
        String payload = "{\"partnerNumber\":\"" + number + "\"}";

        assertThat(call("POST", "/blockPartner", "manager", payload).statusCode())
                .isEqualTo(403);

        HttpResponse<String> ok = call("POST", "/blockPartner", "admin", payload);
        assertThat(ok.statusCode()).isEqualTo(200);
        assertThat(ok.body()).contains("\"blocked\":true");

        HttpResponse<String> partner = call("GET", "/Partners(" + id + ")", "viewer", null);
        assertThat(partner.body()).contains("\"blocked\":true");
    }

    @Test
    void blockUnknownPartnerReturns404() throws Exception {
        HttpResponse<String> r = call("POST", "/blockPartner", "admin",
                "{\"partnerNumber\":\"9999999\"}");
        assertThat(r.statusCode()).isEqualTo(404);
    }

    // ---------- modelo (composição e visão) ----------

    @Test
    void deletingPartnerDeletesItsInvoices() throws Exception {
        String payload = "{\"partnerNumber\":\"" + uniqueNumber() + "\",\"name\":\"Com fatura\","
                + "\"invoices\":[{\"amount\":750.00,\"currency\":\"BRL\","
                + "\"dueDate\":\"2026-12-20\"}]}";
        HttpResponse<String> created = call("POST", "/Partners", "manager", payload);
        assertThat(created.statusCode()).isEqualTo(201);

        List<String> all = ids(created.body());
        String partnerId = all.get(0);   // o primeiro ID é o do parceiro
        String invoiceId = all.get(1);   // o segundo é o da fatura aninhada

        assertThat(call("GET", "/Invoices(" + invoiceId + ")", "viewer", null).statusCode())
                .isEqualTo(200);
        assertThat(call("DELETE", "/Partners(" + partnerId + ")", "admin", null).statusCode())
                .isEqualTo(204);
        assertThat(call("GET", "/Invoices(" + invoiceId + ")", "viewer", null).statusCode())
                .isEqualTo(404);
    }

    @Test
    void openInvoicesExcludesPaidOnes() throws Exception {
        HttpResponse<String> r = call("GET", "/OpenInvoices", "viewer", null);
        assertThat(r.statusCode()).isEqualTo(200);
        assertThat(r.body()).contains("aaaaaaaa-aaaa");      // fatura em aberto
        assertThat(r.body()).doesNotContain("bbbbbbbb-bbbb"); // fatura paga
    }
}
