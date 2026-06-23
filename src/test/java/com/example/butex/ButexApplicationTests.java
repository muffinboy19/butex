package com.example.butex;

import com.example.butex.config.TestRedisConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestRedisConfig.class)
class ButexApplicationTests {

    @Test
    void contextLoads() {
    }

}
