package com.example.demo.event;

import com.example.demo.model.ValueDto;
import org.springframework.context.ApplicationEvent;

public class UpdateValueEvent extends ApplicationEvent {

    private final String key;
    private final ValueDto dto;

    public UpdateValueEvent(Object source, String key, ValueDto dto) {
        super(source);
        this.key = key;
        this.dto = dto;
    }

    public String getKey() {
        return key;
    }

    public ValueDto getDto() {
        return dto;
    }
}

