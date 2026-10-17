package dev.canhdominich.junit.catalog;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ProductController.class)
class ProductControllerWebTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    ProductService service;

    @Test
    void returnsAProductAsJson() throws Exception {
        when(service.find(1L)).thenReturn(new Product("Book", 15_000));

        mvc.perform(get("/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Book"))
                .andExpect(jsonPath("$.price").value(15_000));
    }

    @Test
    void mapsAMissingProductTo404() throws Exception {
        when(service.find(7L)).thenThrow(new ProductNotFoundException(7));

        mvc.perform(get("/products/7"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("product 7 not found"));
    }

    @Test
    void rejectsABlankNameWith400() throws Exception {
        mvc.perform(post("/products").contentType(MediaType.APPLICATION_JSON).content("{\"name\": \"\", \"price\": 100}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsANegativePriceWith400() throws Exception {
        mvc.perform(post("/products").contentType(MediaType.APPLICATION_JSON).content("{\"name\": \"Book\", \"price\": -1}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createsAProductWith201() throws Exception {
        when(service.create("Book", 15_000)).thenReturn(new Product("Book", 15_000));

        mvc.perform(post("/products").contentType(MediaType.APPLICATION_JSON).content("{\"name\": \"Book\", \"price\": 15000}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Book"));
    }
}
