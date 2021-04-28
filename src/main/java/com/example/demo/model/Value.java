package com.example.demo.model;

import lombok.*;

@Data
@Getter
@Setter
@EqualsAndHashCode
@ToString
public class Value {
    private String key = "";
    private String value = "";

    public Value() {
    }

    public Value(String key, String value) {
        this.key = key;
        this.value = value;
    }
}