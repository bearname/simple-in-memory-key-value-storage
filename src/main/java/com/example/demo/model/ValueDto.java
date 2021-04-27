package com.example.demo.model;


import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ValueDto {
    private String value;
    private long updateDate;
}