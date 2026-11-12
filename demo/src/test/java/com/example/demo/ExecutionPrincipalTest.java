package com.example.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.example.demo.service.tool.AgentPrincipal;
import com.example.demo.service.tool.ExecutionPrincipal;
import com.example.demo.service.tool.HumanPrincipal;

class ExecutionPrincipalTest {

    @Test
    void shouldRepresentDifferentPrincipalTypes() {

        ExecutionPrincipal human =
                new HumanPrincipal(1L, 10L);

        ExecutionPrincipal agent =
                new AgentPrincipal(1L, 20L);

        assertInstanceOf(HumanPrincipal.class, human);
        assertInstanceOf(AgentPrincipal.class, agent);

        assertEquals(1L, human.campaignId());

        assertEquals(
                20L,
                ((AgentPrincipal) agent).agentId()
        );
    }

    @Test
    void shouldRejectInvalidIds() {

        assertThrows(
                IllegalArgumentException.class,
                () -> new HumanPrincipal(1L, null)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new AgentPrincipal(-1L, 20L)
        );
    }
}