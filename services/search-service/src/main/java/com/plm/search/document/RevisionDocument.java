package com.plm.search.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Document(indexName = "plm-revisions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RevisionDocument {

    @Id
    private String id;

    @Field(type = FieldType.Keyword)
    private String itemId;

    @Field(type = FieldType.Keyword)
    private String itemNumber;

    @Field(type = FieldType.Keyword)
    private String revisionCode;

    @Field(type = FieldType.Keyword)
    private String status;

    @Field(type = FieldType.Date)
    private String createdAt;
}
