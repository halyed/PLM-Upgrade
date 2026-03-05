package com.plm.search.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Document(indexName = "plm-items")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemDocument {

    @Id
    private String id;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String itemNumber;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String name;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String description;

    @Field(type = FieldType.Keyword)
    private String lifecycleState;

    @Field(type = FieldType.Date)
    private String createdAt;
}
