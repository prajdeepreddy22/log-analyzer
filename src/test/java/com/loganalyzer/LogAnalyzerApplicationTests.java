package com.loganalyzer;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "aeip.redis.required=false")
class LogAnalyzerApplicationTests {

    @Test
    void contextLoads() {
    }

}
