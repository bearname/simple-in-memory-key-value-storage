package com.example.demo.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Log {
    private final String key ;
    private String value;
    private long updatedAt;

    public Log() {
        this.key = "";
    }
}
