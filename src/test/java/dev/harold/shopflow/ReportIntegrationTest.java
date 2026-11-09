package dev.harold.shopflow;

import dev.harold.shopflow.support.IntegrationTest;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReportIntegrationTest extends IntegrationTest {

    @Test
    void salesReportAggregatesPaidOrders() throws Exception {
        Session admin = loginAdmin();
        Long bestSeller = createProduct(admin, "1.00", 500);
        for (int i = 0; i < 2; i++) {
            Session c = registerCustomer();
            addToCart(c, bestSeller, 60);
            pay(c, checkout(c), UUID.randomUUID().toString(), "tok_visa").andExpect(status().isCreated());
        }
        String today = LocalDate.now(ZoneId.of("America/Lima")).toString();

        mvc.perform(get("/api/admin/reports/sales").param("from", today).param("to", today)
                        .header("Authorization", admin.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currency").value("PEN"))
                .andExpect(jsonPath("$.paidOrders").value(greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.revenue").value(greaterThanOrEqualTo(140.0)))
                .andExpect(jsonPath("$.topProducts[*].productId").value(hasItem(bestSeller.intValue())))
                .andExpect(jsonPath("$.ordersByStatus.PAID").value(greaterThanOrEqualTo(2)));
    }

    @Test
    void rejectsInvalidRangeAndNonAdmins() throws Exception {
        Session admin = loginAdmin();
        mvc.perform(get("/api/admin/reports/sales").param("from", "2026-02-01").param("to", "2026-01-01")
                        .header("Authorization", admin.bearer()))
                .andExpect(status().isUnprocessableContent());

        Session customer = registerCustomer();
        mvc.perform(get("/api/admin/reports/sales").param("from", "2026-01-01").param("to", "2026-01-31")
                        .header("Authorization", customer.bearer()))
                .andExpect(status().isForbidden());
    }
}
