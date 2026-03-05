package com.plm.search.service;

import com.plm.search.document.ItemDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

public interface ItemSearchRepository extends ElasticsearchRepository<ItemDocument, String> {

    List<ItemDocument> findByNameContainingOrDescriptionContainingOrItemNumberContaining(
            String name, String description, String itemNumber);

    List<ItemDocument> findByLifecycleState(String lifecycleState);
}
