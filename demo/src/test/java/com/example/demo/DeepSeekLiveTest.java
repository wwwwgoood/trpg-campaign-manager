
package com.example.demo;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.example.demo.service.ai.DeepSeekClient;

@SpringBootTest
class DeepSeekLiveTest {

    @Autowired
    private DeepSeekClient deepSeekClient;

    @Test
    @EnabledIfEnvironmentVariable(
            named = "DEEPSEEK_LIVE_TEST",
            matches = "true"
    )
    void shouldCallRealDeepSeek() {

        String result = deepSeekClient.generateText(
                "你是一名TRPG主持人。用两句话描写一名冒险者走进酒馆。"
        );

        assertNotNull(result);
        assertFalse(result.isBlank());

        System.out.println("DeepSeek 回复：");
        System.out.println(result);
    }
}
    