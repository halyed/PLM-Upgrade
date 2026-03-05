package com.plm.search.service;

import com.plm.search.document.ItemDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final ItemSearchRepository itemRepository;

    public List<ItemDocument> searchItems(String query) {
        if (query == null || query.isBlank()) {
            return (List<ItemDocument>) itemRepository.findAll();
        }
        return itemRepository.findByNameContainingOrDescriptionContainingOrItemNumberContaining(
                query, query, query);
    }

    public List<ItemDocument> filterByState(String lifecycleState) {
        return itemRepository.findByLifecycleState(lifecycleState);
    }

    public void indexItem(ItemDocument doc) {
        itemRepository.save(doc);
    }
}
