package com.plm.reporting.controller;

import com.plm.reporting.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    /** GET /api/reports/summary — item counts by state + CR counts */
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> summary() {
        return ResponseEntity.ok(reportService.getSummary());
    }

    /** GET /api/reports/items/export?format=csv */
    @GetMapping("/items/export")
    public ResponseEntity<byte[]> exportItems(@RequestParam(defaultValue = "csv") String format) {
        byte[] data = reportService.exportItemsAsCsv();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=items.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(data);
    }

    /** GET /api/reports/bom/{revisionId}/export — Excel BOM export */
    @GetMapping("/bom/{revisionId}/export")
    public ResponseEntity<byte[]> exportBom(@PathVariable Long revisionId) throws Exception {
        byte[] data = reportService.exportBomAsExcel(revisionId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=bom-rev-" + revisionId + ".xlsx")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }
}
