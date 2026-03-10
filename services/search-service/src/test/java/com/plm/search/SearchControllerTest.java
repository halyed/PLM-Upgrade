package com.plm.search;

import com.plm.search.document.ItemDocument;
import com.plm.search.service.ItemSearchRepository;
import com.plm.search.service.ReindexService;
import com.plm.search.service.SearchService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SearchControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean JwtDecoder jwtDecoder;
    @MockBean SearchService searchService;
    @MockBean ReindexService reindexService;
    @MockBean ItemSearchRepository itemSearchRepository;

    @Test
    void searchItems_returnsResults() throws Exception {
        ItemDocument doc = ItemDocument.builder()
                .id("1").itemNumber("PLM-001").name("Motor Controller")
                .description("Main controller").lifecycleState("RELEASED").build();
        when(searchService.searchItems(anyString())).thenReturn(List.of(doc));

        mockMvc.perform(get("/api/search/items?q=motor").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].itemNumber").value("PLM-001"))
                .andExpect(jsonPath("$[0].lifecycleState").value("RELEASED"));
    }

    @Test
    void filterByState_returnsResults() throws Exception {
        ItemDocument doc = ItemDocument.builder()
                .id("2").itemNumber("PLM-002").name("BMS")
                .description("Battery management").lifecycleState("DRAFT").build();
        when(searchService.filterByState("DRAFT")).thenReturn(List.of(doc));

        mockMvc.perform(get("/api/search/items/by-state?state=DRAFT").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].lifecycleState").value("DRAFT"));
    }

    @Test
    void searchItems_emptyQuery_returnsAll() throws Exception {
        when(searchService.searchItems("")).thenReturn(List.of());

        mockMvc.perform(get("/api/search/items?q=").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}
