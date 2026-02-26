package com.plm.entity;

public enum ConversionStatus {
    N_A,        // not a CAD file
    PENDING,    // queued for conversion
    CONVERTING, // in progress
    DONE,       // converted successfully
    FAILED      // conversion failed
}
