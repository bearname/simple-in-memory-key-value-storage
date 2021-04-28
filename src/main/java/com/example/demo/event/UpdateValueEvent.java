package com.example.demo.event;

import com.example.demo.model.Log;
import org.springframework.context.ApplicationEvent;

public class UpdateValueEvent extends ApplicationEvent {

    private final Log log;

    public UpdateValueEvent(Object source, Log log) {
        super(source);
        this.log = log;
    }

    public Log getLog() {
        return log;
    }
}
