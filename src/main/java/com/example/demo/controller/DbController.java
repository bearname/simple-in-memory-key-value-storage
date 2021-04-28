package com.example.demo.controller;

import com.example.demo.model.Log;
import com.example.demo.model.Value;
import com.example.demo.service.StoreService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping("/api/v1")
public class DbController {
    private final StoreService storeService;

    @Autowired
    public DbController(StoreService storeService) {
        this.storeService = storeService;
    }

    @PostMapping(value = "/set", consumes = {APPLICATION_JSON_VALUE, MediaType.APPLICATION_FORM_URLENCODED_VALUE},
            produces = {APPLICATION_JSON_VALUE, MediaType.APPLICATION_FORM_URLENCODED_VALUE})
    public ResponseEntity<HashMap<String, String>> createTask(@RequestBody Value value) {
        storeService.set(value.getKey(), value.getValue());

        final HashMap<String, String> objectObjectHashMap = new HashMap<>();
        return new ResponseEntity<>(objectObjectHashMap, HttpStatus.OK);
    }

    @GetMapping("/get")
    public ResponseEntity<HashMap<String, String>> getTextDetails(@RequestParam("key") String jobId) {
        final Object processingResult = storeService.get(jobId);

        final HashMap<String, String> objectObjectHashMap = new HashMap<>();

        if (processingResult == null) {
            objectObjectHashMap.put("result", "");
        } else {
            objectObjectHashMap.put("result", String.valueOf(processingResult));
        }

        return new ResponseEntity<>(objectObjectHashMap, HttpStatus.OK);
    }

    @PostMapping(value = "/synchronise", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<String> synchronise(HttpEntity<String> httpEntity) {
        final String body = httpEntity.getBody();
        try {
            ObjectMapper mapper = new ObjectMapper();
            List<Log> transactionLog = mapper.readValue(body, new TypeReference<>() {
            });

            storeService.synchronise(transactionLog);

            return ResponseEntity
                    .ok()
                    .body("true");
        } catch (Exception exception) {
            exception.printStackTrace();
            return ResponseEntity
                    .ok()
                    .body("false");
        }
    }
}
