package com.speedsneakers.productserviceelastic.model.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

/**
 * Modelo de datos del producto
 */
@Data
@Document(indexName = "products", createIndex = true)
public class Product {

    /**
     * Identificador del producto
     */
    @Id
    private String id;

    /**
     * Nombre del producto
     */
    @Field(type = FieldType.Text, name = "name")
    private String name;

    /**
     * Marca del producto
     */
    @Field(type = FieldType.Text, name = "brand")
    private String brand;

    /**
     * Categoría del producto
     */
    @Field(type = FieldType.Text, name = "category")
    private String category;

    /**
     * Descripción corta del producto
     */
    @Field(type = FieldType.Object, name = "short_description", enabled = false)
    private String shortDescription;

    /**
     * Descripción larga del producto
     */
    @Field(type = FieldType.Object, name = "long_description", enabled = false)
    private String longDescription;

    /**
     * Precio del producto
     */
    @Field(type = FieldType.Double, name = "price")
    private Double price;

    /**
     * Imagen del producto
     */
    @Field(type = FieldType.Object, name = "image_url", enabled = false)
    private String imageUrl;
}
