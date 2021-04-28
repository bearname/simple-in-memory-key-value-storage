package com.example.demo.service;

import com.example.demo.event.UpdateValueEvent;
import com.example.demo.model.Log;
import com.example.demo.model.ValueDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.io.PrintStream;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class StoreServiceImpl implements StoreService {
    private final PrintStream logger = System.out;
    private final Map<String, ValueDto> localRepository = new ConcurrentHashMap<>();
    private final ApplicationEventPublisher eventPublisher;

    @Autowired
    public StoreServiceImpl(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
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
        localRepository.put(key, new ValueDto(value, updatedAt));

        final Log source = new Log(key, dto.getValue(), dto.getUpdateDate());
        eventPublisher.publishEvent(new UpdateValueEvent(source, source));
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

    private ValueDto getValueDto(final String key) {
        if (localRepository.containsKey(key)) {
            return localRepository.get(key);
        }
        return null;
    }
}
