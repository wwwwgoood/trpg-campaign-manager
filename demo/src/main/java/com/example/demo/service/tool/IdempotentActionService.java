
package com.example.demo.service.tool;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.service.action.ActionExecutor;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Service
public class IdempotentActionService {

    private final JdbcTemplate jdbc;
    private final ActionExecutor executor;
    private final ObjectMapper mapper;

    public IdempotentActionService(
            JdbcTemplate jdbc,
            ActionExecutor executor,
            ObjectMapper mapper) {

        this.jdbc = jdbc;
        this.executor = executor;
        this.mapper = mapper;
    }

    @Transactional
    public List<String> executeOnce(
            String executionId,
            AgentPrincipal principal,
            Long actionId,
            Long actorId,
            Long targetId) {

        if (executionId == null || executionId.isBlank()
                || executionId.length() > 100
                || principal == null
                || actionId == null || actionId <= 0
                || actorId == null || actorId <= 0
                || (targetId != null && targetId <= 0)) {

            throw new IllegalArgumentException(
                    "Invalid execution request");
        }

        // 当前工具参数均为Long，可以直接构造稳定表示。
        String arguments = actionId + ":"
                + actorId + ":" + targetId;

        // 1. 尝试取得执行权
        List<Long> inserted = jdbc.query(
                """
                INSERT INTO tool_execution_record
                    (execution_id, campaign_id, agent_id,
                     tool_name, arguments, status)
                VALUES (?, ?, ?, 'execute_action', ?, 'RUNNING')
                ON CONFLICT (execution_id) DO NOTHING
                RETURNING id
                """,
                (rs, rowNum) -> rs.getLong("id"),
                executionId,
                principal.campaignId(),
                principal.agentId(),
                arguments
        );

        // 2. 已经存在：检查是否为同一个逻辑请求
        if (inserted.isEmpty()) {

            return jdbc.queryForObject(
                    """
                    SELECT campaign_id, agent_id,
                           tool_name, arguments,
                           status, result
                    FROM tool_execution_record
                    WHERE execution_id = ?
                    """,
                    (rs, rowNum) -> {

                        boolean sameRequest =
                                principal.campaignId().equals(
                                        rs.getLong("campaign_id"))
                                && principal.agentId().equals(
                                        rs.getLong("agent_id"))
                                && "execute_action".equals(
                                        rs.getString("tool_name"))
                                && arguments.equals(
                                        rs.getString("arguments"));

                        if (!sameRequest) {
                            throw new IllegalArgumentException(
                                    "Execution ID reused with different request");
                        }

                        if (!"SUCCEEDED".equals(
                                rs.getString("status"))) {
                            throw new IllegalStateException(
                                    "Execution is not completed");
                        }

                        return mapper.readValue(
                                rs.getString("result"),
                                new TypeReference<List<String>>() {});
                    },
                    executionId
            );
        }

        // 3. 首次执行：调用已有ActionExecutor
        List<String> result = executor.executeAsAgent(
                principal, actionId, actorId, targetId);

        // 4. 保存结果，与Action处于同一事务
        jdbc.update(
                """
                UPDATE tool_execution_record
                SET status = 'SUCCEEDED',
                    result = ?
                WHERE execution_id = ?
                """,
                mapper.writeValueAsString(result),
                executionId
        );

        return result;
    }
}
