package com.example.demo.Entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Getter 
@Setter 
@Entity
@Table(
    name = "tool_execution_record",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_tool_execution_id",
            columnNames = "execution_id"
        )
    }
)
public class ToolExecutionRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
        name = "execution_id",
        nullable = false,
        length = 100
    )
    private String executionId;

    @Column(nullable = false)
    private Long campaignId;

    @Column(nullable = false)
    private Long agentId;

    @Column(nullable = false)
    private String toolName;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String arguments;

    @Column(nullable = false)
    private String status;

    @Column(columnDefinition = "TEXT")
    private String result;

    // 构造器、Getter、Setter 略
}