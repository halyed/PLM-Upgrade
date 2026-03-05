package com.plm.controller;

import com.plm.dto.ItemResponse;
import com.plm.dto.ChangeRequestResponse;
import com.plm.service.ItemService;
import com.plm.service.ChangeRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final ItemService itemService;
    private final ChangeRequestService changeRequestService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> search(@RequestParam String q) {
        if (q == null || q.isBlank()) {
            return ResponseEntity.ok(Map.of("items", List.of(), "changeRequests", List.of()));
        }
        List<ItemResponse> items = itemService.search(q);
        List<ChangeRequestResponse> crs = changeRequestService.getAll().stream()
                .filter(cr -> cr.getTitle().toLowerCase().contains(q.toLowerCase())
                        || (cr.getDescription() != null && cr.getDescription().toLowerCase().contains(q.toLowerCase())))
                .toList();
        return ResponseEntity.ok(Map.of("items", items, "changeRequests", crs));
    }
}
