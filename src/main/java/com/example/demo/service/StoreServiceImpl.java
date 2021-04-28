package com.example.demo.service;

import com.example.demo.event.UpdateValueEvent;
import com.example.demo.model.Log;
import com.example.demo.model.ValueDto;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class StoreServiceImpl implements StoreService {
    private final PrintStream logger = System.out;
    private final Map<String, ValueDto> localRepository = new ConcurrentHashMap<>();
    private final ApplicationEventPublisher eventPublisher;
    private final AtomicBoolean isUpdated = new AtomicBoolean(false);

    @Value("${backup}")
    private String backupFile;

    @Autowired
    public StoreServiceImpl(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
        restore();
    }

    @Override
    public Object get(final String key) {
        if (localRepository.containsKey(key)) {
            return localRepository.get(key).getValue();
        }
        return null;
    }

    @Override
    public void set(final String key, final String value) {
        final long updatedAt = System.currentTimeMillis() / 1000;

        final ValueDto dto = new ValueDto(value, updatedAt);
        localRepository.put(key, dto);
        isUpdated.set(true);
        eventPublisher.publishEvent(new UpdateValueEvent(dto, key,  dto));
    }

    @Override
    public void synchronise(List<Log> transactionLog) {
        isUpdated.set(true);
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

    @Override
    @Scheduled(fixedRate = 30000)
    public void backup() {
        if (isUpdated.get() && !localRepository.isEmpty()) {
            final String json = getJsonString();
            final String fileName = getFileName();
            final File backupFile = new File(fileName);
            final String absolutePath = backupFile.getAbsolutePath();
            System.out.println("Backup file " + absolutePath);
            try {
                try (Writer fileWriter = new BufferedWriter(new OutputStreamWriter(
                        new FileOutputStream(backupFile), StandardCharsets.UTF_8))) {
                    fileWriter.write(json);
                    fileWriter.flush();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            isUpdated.set(false);
        }
    }

    private String getFileName() {
        String pattern = "MM-dd-yyyy-HH-mm-ss";
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
        return "backup-" + simpleDateFormat.format(new Date()) + ".json";
    }

    private String getJsonString() {
        StringBuilder builder = new StringBuilder();
        builder.append("[");
        int i = 0;
        final int size = localRepository.size();
        for (var entry : localRepository.entrySet()) {
            final ValueDto value = entry.getValue();
            builder.append("{\"key\":\"").append(entry.getKey())
                    .append("\",\"value\":\"").append(value.getValue())
                    .append("\",\"updatedAt\":").append(value.getUpdateDate())
                    .append("}");
            if (i != size - 1) {
                builder.append(",");
            }
            i++;
        }
        builder.append("]");
        return builder.toString();
    }

    private void restore() {
        try {
            String json = Files.readString(Path.of("backup-04-28-2021-15-59-36.json"), StandardCharsets.US_ASCII);
            ObjectMapper mapper = new ObjectMapper();
            List<Log> transactionLog = mapper.readValue(json, new TypeReference<>() {
            });

            for (Log value : transactionLog) {
                set(value.getKey(), value.getValue());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private ValueDto getValueDto(final String key) {
        if (localRepository.containsKey(key)) {
            return localRepository.get(key);
        }
        return null;
    }
}
