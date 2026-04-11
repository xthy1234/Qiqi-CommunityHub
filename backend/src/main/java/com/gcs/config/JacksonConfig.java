package com.gcs.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Configuration
public class JacksonConfig {
    
    private static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
    private static final String DATE_PATTERN = "yyyy-MM-dd";
    
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        

        JavaTimeModule javaTimeModule = new JavaTimeModule();
        

        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN);
        javaTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(dateTimeFormatter));
        javaTimeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(dateTimeFormatter));
        
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(DATE_PATTERN);
        javaTimeModule.addSerializer(LocalDate.class, new LocalDateSerializer(dateFormatter));
        javaTimeModule.addDeserializer(LocalDate.class, new JsonDeserializer<LocalDate>() {
            @Override
            public LocalDate deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                if (p.getCurrentToken() == null) {
                    return null;
                }
                
                switch (p.getCurrentToken()) {
                    case VALUE_NUMBER_INT:
                        long timestamp = p.getLongValue();
                        return Instant.ofEpochMilli(timestamp)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate();
                    
                    case VALUE_STRING:
                        String value = p.getText();
                        if (value == null || value.isEmpty()) {
                            return null;
                        }
                        
                        try {
                            long ts = Long.parseLong(value);
                            return Instant.ofEpochMilli(ts)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate();
                        } catch (NumberFormatException e) {
                            try {
                                return LocalDate.parse(value, dateFormatter);
                            } catch (DateTimeParseException ex) {
                                throw new IOException("无法解析日期: " + value, ex);
                            }
                        }
                    
                    default:
                        throw new IOException("不支持的日期格式");
                }
            }
        });
        
        objectMapper.registerModule(javaTimeModule);
        
        return objectMapper;
    }
}
