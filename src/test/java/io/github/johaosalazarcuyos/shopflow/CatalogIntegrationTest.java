package io.github.johaosalazarcuyos.shopflow;

import io.github.johaosalazarcuyos.shopflow.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CatalogIntegrationTest extends IntegrationTest {

    @Test
    void catalogIsPublicAndSearchable() throws Exception {
        mvc.perform(get("/api/products").param("q", "laptop"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(greaterThanOrEqualTo(2)));

        mvc.perform(get("/api/products").param("category", "monitores")
                        .param("minPrice", "500").param("maxPrice", "1000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].categoryName").value(everyItem(org.hamcrest.Matchers.is("Monitores"))))
                .andExpect(jsonPath("$.content[*].price").value(everyItem(lessThanOrEqualTo(1000.0))));

        mvc.perform(get("/api/categories")).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(5)));
    }

    @Test
    void onlyAdminsCanManageProducts() throws Exception {
        Session customer = registerCustomer();
        mvc.perform(post("/api/admin/products").header("Authorization", customer.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/admin/products").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void cacheIsEvictedOnUpdateAndDeactivation() throws Exception {
        Session admin = loginAdmin();
        Long id = createProduct(admin, "100.00", 10);

        mvc.perform(get("/api/products/{id}", id)).andExpect(jsonPath("$.stock").value(10)); // se cachea

        mvc.perform(patch("/api/admin/products/{id}/stock", id).header("Authorization", admin.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"stock\":3}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/products/{id}", id)).andExpect(jsonPath("$.stock").value(3));

        mvc.perform(delete("/api/admin/products/{id}", id).header("Authorization", admin.bearer()))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/products/{id}", id)).andExpect(status().isNotFound());
    }

    @Test
    void rejectsDuplicateSkuAndInvalidData() throws Exception {
        Session admin = loginAdmin();
        mvc.perform(post("/api/admin/products").header("Authorization", admin.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sku":"LAP-14-R5","name":"Duplicado","price":10,"stock":1,"categoryId":1}"""))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SKU_TAKEN"));

        mvc.perform(post("/api/admin/products").header("Authorization", admin.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sku":"minusculas","name":"","price":-1,"stock":-5,"categoryId":1}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.sku").exists())
                .andExpect(jsonPath("$.errors.price").exists())
                .andExpect(jsonPath("$.errors.stock").exists());
    }
}
