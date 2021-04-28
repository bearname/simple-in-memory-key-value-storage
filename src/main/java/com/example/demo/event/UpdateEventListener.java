package com.example.demo.event;

import com.example.demo.model.Log;
import com.example.demo.model.ValueDto;
import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static java.net.http.HttpResponse.BodyHandlers.ofString;

@Component
public class UpdateEventListener implements ApplicationListener<UpdateValueEvent> {
    @Value("${nodes}")
    private String nodes;

    @Value("${events.needed:100}")
    private int eventsNeeded;
    private long lastSynchronisedAt = 0;

    private List<String> nodeList = new ArrayList<>();
    private final Map<String, ValueDto> transactionLog = new ConcurrentHashMap<>();
    private final HttpClient httpClient = HttpClient.newBuilder().build();

    @Override
    public void onApplicationEvent(UpdateValueEvent event) {
        if (nodeList.isEmpty()) {
            if (this.nodes != null) {
        	    final String[] nodes = this.nodes.split(",");
	            this.nodeList = Arrays.asList(nodes);
            } else {
                this.nodeList = new ArrayList<>();
            }
        }

        transactionLog.put(event.getKey(), new ValueDto(event.getDto().getValue(), event.getDto().getUpdateDate()));
    }

    @Scheduled(fixedRate = 5000)
    public void trySynchronise() {
        if (synchroniseNeeded()) {
            synchroniseNode();
        }
    }

    private void synchroniseNode() {
        List<Log> foo = new ArrayList<>();

        for (Map.Entry<String, ValueDto> entry : transactionLog.entrySet()) {
            final String key = entry.getKey();
            final String value = entry.getValue().getValue();
            final long updateDate = entry.getValue().getUpdateDate();
            foo.add(new Log(key, value, updateDate));
        }
        String json = new Gson().toJson(foo);

        StringBuilder builder = new StringBuilder();

        for (String node : this.nodeList) {
            try {
                final String url = "http://" + node + "/api/v1/synchronise";
                final String x = new Date() + " Notify" + url;
                builder.append(x).append("\n");
//                for (int i = 0; i < 3; i++) {
//                    if (!sendPost(json, url)) {
//                        Thread.sleep(300);
//                    }
//                }
                sendPost(httpClient, json, url);
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }
        System.out.println(builder.toString());
        lastSynchronisedAt = System.currentTimeMillis();

        this.transactionLog.clear();
    }

    private boolean synchroniseNeeded() {
        return transactionLog.size() > eventsNeeded || (!transactionLog.isEmpty() && (lastSynchronisedAt == 0 || (System.currentTimeMillis() - lastSynchronisedAt > 5000)));
    }

    private boolean sendPost(final String json, final String urlString) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json; utf-8");
            con.setRequestProperty("Accept", "application/json");
            con.setDoOutput(true);
            try(OutputStream os = con.getOutputStream()) {
                byte[] input = json.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
            try(BufferedReader br = new BufferedReader(
                    new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                return Boolean.getBoolean(response.toString());
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void sendPost(HttpClient httpClient, String json, String url) throws URISyntaxException {
        final var request = HttpRequest.newBuilder(new URI(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        try {
            final var response = httpClient.send(request, ofString());
            System.out.println(response.body());
        } catch (Exception exception) {
            exception.printStackTrace();
        }
//                .thenAccept(System.out::println);
//        final var response = httpClient.sendAsync(request, ofString());
//        response.thenApply(HttpResponse::body)
//                .thenAccept(System.out::println);
    }
}
