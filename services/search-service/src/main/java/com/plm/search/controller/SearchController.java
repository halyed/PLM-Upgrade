package com.plm.search.controller;

import com.plm.search.document.ItemDocument;
import com.plm.search.service.ReindexService;
import com.plm.search.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;
    private final ReindexService reindexService;

    /**
     * Full-text search across items (name, description, item number).
     * GET /api/search/items?q=motor
     */
    @GetMapping("/items")
    public ResponseEntity<List<ItemDocument>> searchItems(
            @RequestParam(required = false) String q) {
        return ResponseEntity.ok(searchService.searchItems(q));
    }

    /**
     * Filter items by lifecycle state.
     * GET /api/search/items/by-state?state=RELEASED
     */
    @GetMapping("/items/by-state")
    public ResponseEntity<List<ItemDocument>> byState(@RequestParam String state) {
        return ResponseEntity.ok(searchService.filterByState(state));
    }

    /**
     * POST /api/search/reindex
     * Pulls all items from plm-core-service and re-indexes them into Elasticsearch.
     * Call this once after initial data seeding.
     */
    @PostMapping("/reindex")
    public ResponseEntity<Map<String, Object>> reindex() {
        int count = reindexService.reindexAll();
        return ResponseEntity.ok(Map.of("indexed", count, "status", "ok"));
    }
}
