
package com.example.demo;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.example.demo.service.ai.DeepSeekDiceService;

@SpringBootTest
class DeepSeekDiceLiveTest {

    @Autowired
    private DeepSeekDiceService deepSeekDiceService;

    @Test
    @EnabledIfEnvironmentVariable(
            named = "DEEPSEEK_LIVE_TEST",
            matches = "true"
    )
    void shouldCallRealDiceTool() {

        String result = deepSeekDiceService.ask(
                "请使用roll_dice掷一个1d20+5，"
                + "明确告诉我骰子本身的点数、加值和总结果。"
        );

        assertNotNull(result);
        assertFalse(result.isBlank());

        System.out.println("DeepSeek 工具调用结果：");
        System.out.println(result);
    }
}
