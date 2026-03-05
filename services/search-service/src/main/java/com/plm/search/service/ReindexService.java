package com.plm.search.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.plm.search.document.ItemDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReindexService {

    private final ItemSearchRepository itemRepository;

    @Value("${plm.core.url:http://plm-core-service:8080}")
    private String plmCoreUrl;

    public int reindexAll() {
        RestTemplate rest = new RestTemplate();
        log.info("Fetching all items from {}", plmCoreUrl);

        JsonNode response = rest.getForObject(plmCoreUrl + "/api/items", JsonNode.class);

        List<ItemDocument> docs = new ArrayList<>();
        JsonNode items = response != null && response.has("content") ? response.get("content") : response;

        if (items != null && items.isArray()) {
            for (JsonNode item : items) {
                docs.add(ItemDocument.builder()
                        .id(item.path("id").asText())
                        .itemNumber(item.path("itemNumber").asText())
                        .name(item.path("name").asText())
                        .description(item.path("description").asText())
                        .lifecycleState(item.path("lifecycleState").asText())
                        .createdAt(item.path("createdAt").asText())
                        .build());
            }
        }

        itemRepository.saveAll(docs);
        log.info("Reindexed {} items into Elasticsearch", docs.size());
        return docs.size();
    }
}
