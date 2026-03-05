package com.plm.integration.connector;

import com.plm.integration.dto.ConnectorStatus;
import com.plm.integration.event.PlmEvent;

/**
 * Contract every external system connector must implement.
 * Each connector receives PLM events and decides what to do with them.
 */
public interface ExternalSystemConnector {

    /** Unique identifier for this connector (e.g. "odoo", "mes", "freecad") */
    String getId();

    /** Human-readable name */
    String getName();

    /** Whether this connector is enabled via configuration */
    boolean isEnabled();

    /** Health check — can the connector reach the external system? */
    ConnectorStatus status();

    /** Handle an inbound PLM event */
    void onPlmEvent(PlmEvent event);
}
