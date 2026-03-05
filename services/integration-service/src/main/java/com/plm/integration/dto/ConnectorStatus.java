package com.plm.integration.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ConnectorStatus {

    private String id;
    private String name;
    private String state;   // "UP" | "DOWN"
    private String detail;

    public static ConnectorStatus up(String id, String name) {
        return new ConnectorStatus(id, name, "UP", null);
    }

    public static ConnectorStatus down(String id, String name, String reason) {
        return new ConnectorStatus(id, name, "DOWN", reason);
    }

    public boolean isUp() { return "UP".equals(state); }
}
