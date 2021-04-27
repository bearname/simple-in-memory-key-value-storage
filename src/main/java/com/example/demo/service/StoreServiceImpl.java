package com.example.demo.service;

import com.example.demo.model.Log;
import com.example.demo.model.ValueDto;
import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.PrintStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static java.net.http.HttpResponse.BodyHandlers.ofString;

@Service
public class StoreServiceImpl implements StoreService {
    private final PrintStream logger = System.out;
    private final Map<String, ValueDto> localRepository = new ConcurrentHashMap<>();
    private final Map<String, ValueDto> transactionLog = new ConcurrentHashMap<>();

    @Value("${nodes}")
    private String nodes;

    private List<String> nodeList = new ArrayList<>();

    @Override
    public Object get(final String key) {
        System.out.println(nodes);

        if (localRepository.containsKey(key)) {
            return localRepository.get(key).getValue();
        }
        return null;
    }

    @Override
    public int size() {
        return localRepository.size();
    }

    private ValueDto getValueDto(final String key) {
        if (localRepository.containsKey(key)) {
            return localRepository.get(key);
        }
        return null;
    }

    @Override
    public void set(final String key, final String value) {
        final long updatedAt = System.currentTimeMillis() / 1000;

        transactionLog.put(key, new ValueDto(value, updatedAt));
        localRepository.put(key, new ValueDto(value, updatedAt));

        notifyNodes();
    }

    @Override
    public void synchronise(List<Log> transactionLog) {
        for (Log log : transactionLog) {
            final String key = log.getKey();
            final ValueDto localValue = getValueDto(key);
            if (localValue == null) {
                localRepository.put(key, new ValueDto(log.getValue(), log.getUpdatedAt()));
            } else {
                if (log.getUpdatedAt() - localValue.getUpdateDate() > 2) {
                    localRepository.put(key, new ValueDto(log.getValue(), log.getUpdatedAt()));

                    logger.println(new Date() + " Updated key " + key + ". New value: " + log.getValue());
                }
            }
        }
    }

    private void notifyNodes() {
        if (nodeList.isEmpty()) {
            final String[] nodes = this.nodes.split(",");
            this.nodeList = Arrays.asList(nodes);
        }
        List<Log> foo = new ArrayList<>();
        for (Map.Entry<String, ValueDto> entry : transactionLog.entrySet()) {
            final String key = entry.getKey();
            final String value = entry.getValue().getValue();
            final long updateDate = entry.getValue().getUpdateDate();
            foo.add(new Log(key, value, updateDate));
        }
        HttpClient httpClient = HttpClient.newBuilder().build();
        String json = new Gson().toJson(foo);

        for (String node : this.nodeList) {
            try {
                final String url = "http://" + node + "/api/v1/synchronise";
                System.out.println(new Date() + " Notify" + url);
                HttpRequest request = HttpRequest.newBuilder(new URI(url))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json))
                        .build();

                httpClient.sendAsync(request, ofString())
                        .thenApply(HttpResponse::body)
                        .thenAccept(System.out::println);
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }

        this.transactionLog.clear();
    }
}
