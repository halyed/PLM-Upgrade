package com.plm.reporting.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayOutputStream;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private final RestTemplate restTemplate;

    @Value("${plm.core.url:http://plm-core-service:8080}")
    private String plmCoreUrl;

    /** Returns item counts by lifecycle state and CR counts by status */
    public Map<String, Object> getSummary() {
        Map<String, Object> summary = new LinkedHashMap<>();

        // Items by lifecycle state
        try {
            JsonNode items = restTemplate.getForObject(plmCoreUrl + "/api/items", JsonNode.class);
            JsonNode list = items != null && items.has("content") ? items.get("content") : items;
            Map<String, Long> byState = new LinkedHashMap<>();
            byState.put("DRAFT", 0L); byState.put("IN_REVIEW", 0L);
            byState.put("RELEASED", 0L); byState.put("OBSOLETE", 0L);
            long total = 0;
            if (list != null && list.isArray()) {
                for (JsonNode item : list) {
                    String state = item.path("lifecycleState").asText("DRAFT");
                    byState.merge(state, 1L, Long::sum);
                    total++;
                }
            }
            summary.put("totalItems", total);
            summary.put("itemsByState", byState);
        } catch (Exception e) {
            log.warn("Failed to fetch items for summary: {}", e.getMessage());
            summary.put("totalItems", 0);
            summary.put("itemsByState", Map.of());
        }

        // Change requests by status
        try {
            JsonNode crs = restTemplate.getForObject(plmCoreUrl + "/api/change-requests", JsonNode.class);
            JsonNode list = crs != null && crs.has("content") ? crs.get("content") : crs;
            Map<String, Long> byStatus = new LinkedHashMap<>();
            long total = 0;
            if (list != null && list.isArray()) {
                for (JsonNode cr : list) {
                    String status = cr.path("status").asText("OPEN");
                    byStatus.merge(status, 1L, Long::sum);
                    total++;
                }
            }
            summary.put("totalChangeRequests", total);
            summary.put("changeRequestsByStatus", byStatus);
        } catch (Exception e) {
            log.warn("Failed to fetch CRs for summary: {}", e.getMessage());
            summary.put("totalChangeRequests", 0);
            summary.put("changeRequestsByStatus", Map.of());
        }

        return summary;
    }

    /** Exports BOM for a revision as Excel (.xlsx) bytes */
    public byte[] exportBomAsExcel(Long revisionId) throws Exception {
        JsonNode bom = restTemplate.getForObject(
                plmCoreUrl + "/api/revisions/" + revisionId + "/bom/children", JsonNode.class);

        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("BOM");

            // Header
            CellStyle headerStyle = wb.createCellStyle();
            Font font = wb.createFont();
            font.setBold(true);
            headerStyle.setFont(font);

            Row header = sheet.createRow(0);
            String[] cols = {"#", "Item Number", "Name", "Revision", "Quantity", "Lifecycle State"};
            for (int i = 0; i < cols.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(cols[i]);
                cell.setCellStyle(headerStyle);
            }

            // Rows
            int rowIdx = 1;
            if (bom != null && bom.isArray()) {
                for (JsonNode entry : bom) {
                    JsonNode child = entry.path("childRevision");
                    Row row = sheet.createRow(rowIdx++);
                    row.createCell(0).setCellValue(rowIdx - 1);
                    row.createCell(1).setCellValue(child.path("itemNumber").asText());
                    row.createCell(2).setCellValue(child.path("name").asText());
                    row.createCell(3).setCellValue(child.path("revisionCode").asText());
                    row.createCell(4).setCellValue(entry.path("quantity").asDouble(1));
                    row.createCell(5).setCellValue(child.path("lifecycleState").asText());
                }
            }

            for (int i = 0; i < cols.length; i++) sheet.autoSizeColumn(i);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();
        }
    }

    /** Exports all items as CSV bytes */
    public byte[] exportItemsAsCsv() {
        StringBuilder sb = new StringBuilder("ID,Item Number,Name,Lifecycle State,Description,Created At\n");
        try {
            JsonNode items = restTemplate.getForObject(plmCoreUrl + "/api/items", JsonNode.class);
            JsonNode list = items != null && items.has("content") ? items.get("content") : items;
            if (list != null && list.isArray()) {
                for (JsonNode item : list) {
                    sb.append(csv(item.path("id").asText())).append(",")
                      .append(csv(item.path("itemNumber").asText())).append(",")
                      .append(csv(item.path("name").asText())).append(",")
                      .append(csv(item.path("lifecycleState").asText())).append(",")
                      .append(csv(item.path("description").asText(""))).append(",")
                      .append(csv(item.path("createdAt").asText())).append("\n");
                }
            }
        } catch (Exception e) {
            log.warn("Failed to fetch items for CSV: {}", e.getMessage());
        }
        return sb.toString().getBytes();
    }

    private String csv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
