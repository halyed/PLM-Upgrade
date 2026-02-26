package com.plm.dto;

public record ConversionMessage(Long documentId, Long revisionId, String filePath, String originalFilename) {}
