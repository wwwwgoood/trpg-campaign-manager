package com.example.demo.service.action;

import java.util.HashMap;
import java.util.Map;

public class ActionExecutionContext {

    private final Long actorId;
    private final Long targetId;

    private final Map<String, Object> results = new HashMap<>();

    public ActionExecutionContext(Long actorId, Long targetId) {
        this.actorId = actorId;
        this.targetId = targetId;
    }

    public Long getActorId() {
        return actorId;
    }

    public Long getTargetId() {
        return targetId;
    }

    public void putResult(String key, Object value) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException(
                "Result key cannot be blank"
            );
        }

        if (results.containsKey(key)) {
            throw new IllegalArgumentException(
                "Duplicate result key: " + key
            );
        }

        results.put(key, value);
    }

    public boolean getBoolean(String key) {
        Object value = requireResult(key);

        if (!(value instanceof Boolean result)) {
            throw new IllegalArgumentException(
                "Result is not boolean: " + key
            );
        }

        return result;
    }

    public int getInteger(String key) {
        Object value = requireResult(key);

        if (!(value instanceof Integer result)) {
            throw new IllegalArgumentException(
                "Result is not integer: " + key
            );
        }

        return result;
    }

    private Object requireResult(String key) {
        if (!results.containsKey(key)) {
            throw new IllegalArgumentException(
                "Unknown result key: " + key
            );
        }

        return results.get(key);
    }

    public Object getResult(String key) {
    return requireResult(key);
}
}