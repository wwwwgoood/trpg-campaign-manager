
package com.example.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.example.demo.service.action.ActionExecutor;
import com.example.demo.service.tool.AgentPrincipal;
import com.example.demo.service.tool.IdempotentActionService;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:postgresql://localhost:5432/trpg_campaign_manager_test",
    "spring.jpa.hibernate.ddl-auto=update"
})
class IdempotentActionIntegrationTest {

    @Autowired
    private IdempotentActionService service;

    @Autowired
    private JdbcTemplate jdbc;

    @MockitoBean
    private ActionExecutor executor;

    @Test
    void shouldExecuteOnceAndRollbackOnFailure() throws Exception {

        // 测试专用表：模拟一次Action造成的数据库副作用
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS idempotency_test_counter (
                id INT PRIMARY KEY,
                value INT NOT NULL
            )
            """);

        jdbc.update("""
            INSERT INTO idempotency_test_counter (id, value)
            VALUES (1, 0)
            ON CONFLICT (id) DO UPDATE SET value = 0
            """);

        AgentPrincipal principal = new AgentPrincipal(1L, 2L);

        AtomicBoolean shouldFail = new AtomicBoolean(false);

        // 使用真实JdbcTemplate修改数据库。
        // 因为在外层事务内执行，所以该修改能够被回滚。
        when(executor.executeAsAgent(
                eq(principal), anyLong(), anyLong(), isNull()
        )).thenAnswer(invocation -> {

            jdbc.update("""
                UPDATE idempotency_test_counter
                SET value = value + 1
                WHERE id = 1
                """);

            if (shouldFail.get()) {
                throw new IllegalStateException("Simulated action failure");
            }

            return List.of("Action succeeded");
        });

        String prefix = "test-idempotency-" + UUID.randomUUID();
        String firstId = prefix + "-first";
        String failedId = prefix + "-failed";
        String concurrentId = prefix + "-concurrent";

        ExecutorService pool = Executors.newFixedThreadPool(2);

        try {
            // 场景一：第一次执行，随后重复提交
            List<String> first = service.executeOnce(
                    firstId, principal, 4L, 17L, null);

            List<String> repeated = service.executeOnce(
                    firstId, principal, 4L, 17L, null);

            assertEquals(first, repeated);
            assertEquals(1, counter());

            // 场景二：相同ID不能绑定不同参数
            assertThrows(IllegalArgumentException.class, () ->
                    service.executeOnce(
                            firstId, principal, 5L, 17L, null));

            assertEquals(1, counter());

            // 场景三：Action执行中失败，整体回滚
            shouldFail.set(true);

            assertThrows(IllegalStateException.class, () ->
                    service.executeOnce(
                            failedId, principal, 4L, 17L, null));

            assertEquals(1, counter());

            Integer failedRecordCount = jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM tool_execution_record
                WHERE execution_id = ?
                """, Integer.class, failedId);

            assertEquals(0, failedRecordCount);

            // 场景四：两个线程同时提交同一个executionId
            shouldFail.set(false);

            CountDownLatch ready = new CountDownLatch(2);
            CountDownLatch start = new CountDownLatch(1);

            Callable<List<String>> task = () -> {
                ready.countDown();
                start.await();

                return service.executeOnce(
                        concurrentId, principal, 4L, 17L, null);
            };

            Future<List<String>> a = pool.submit(task);
            Future<List<String>> b = pool.submit(task);

            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();

            assertEquals(
                    a.get(10, TimeUnit.SECONDS),
                    b.get(10, TimeUnit.SECONDS));

            // 初次执行一次 + 并发执行一次
            assertEquals(2, counter());

            // ActionExecutor 实际只被调用3次：
            // 初次成功、一次失败、并发成功一次。
            verify(executor, times(3)).executeAsAgent(
                    eq(principal), anyLong(), anyLong(), isNull());

        } finally {
            pool.shutdownNow();

            jdbc.update("""
                DELETE FROM tool_execution_record
                WHERE execution_id IN (?, ?, ?)
                """, firstId, failedId, concurrentId);
        }
    }

    private int counter() {
        return jdbc.queryForObject("""
            SELECT value
            FROM idempotency_test_counter
            WHERE id = 1
            """, Integer.class);
    }
}
