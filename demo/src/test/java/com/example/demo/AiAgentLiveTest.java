package com.example.demo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.example.demo.service.ai.AiAgentService;

@SpringBootTest
class AiAgentLiveTest {

    @Autowired
    private AiAgentService aiAgentService;

    @Test
    @EnabledIfEnvironmentVariable(
            named = "DEEPSEEK_LIVE_TEST",
            matches = "true"
    )
    void testAgentLoop() {

        String result = aiAgentService.chat(
                "请实际掷一次1d20+5，告诉我结果。"
        );

        System.out.println(result);
    }
}
