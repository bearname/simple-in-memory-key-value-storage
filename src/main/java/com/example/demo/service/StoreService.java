package com.example.demo.service;

import com.example.demo.model.Log;

import java.util.List;

public interface StoreService {
    Object get(final String key);

    void set(String key, String value);

    void synchronise(List<Log> transactionLog);
}