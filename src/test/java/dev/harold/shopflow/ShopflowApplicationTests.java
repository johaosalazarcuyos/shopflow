package dev.harold.shopflow;

import dev.harold.shopflow.support.IntegrationTest;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ShopflowApplicationTests extends IntegrationTest {

    @Test
    void contextLoadsAndFlywayRan() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
        Integer migrations = jdbc.queryForObject("select count(*) from flyway_schema_history where success", Integer.class);
        org.assertj.core.api.Assertions.assertThat(migrations).isEqualTo(2);
    }

    @Test
    void exposesOpenApiAndCustomMetrics() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andExpect(jsonPath("$.info.title").value("ShopFlow API"));
        mvc.perform(get("/actuator/metrics/shopflow.orders.placed").header("Authorization", loginAdmin().bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("shopflow.orders.placed"));
    }
}
