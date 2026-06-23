package com.example.butex.config;

import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@TestConfiguration
public class TestRedisConfig {

    @Bean
    @Primary
    StringRedisTemplate stringRedisTemplate() {
        StringRedisTemplate template = Mockito.mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOps = Mockito.mock(ValueOperations.class);
        when(template.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
        return template;
    }
}
