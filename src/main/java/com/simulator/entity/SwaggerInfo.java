package com.simulator.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Swagger文档信息实体类
 * 
 * @author simulator
 * @date 2024
 */
@Entity
@Table(name = "swagger_info")
@Data
public class SwaggerInfo {

    /**
     * 主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Swagger文档标题
     */
    @Column(name = "title", length = 200)
    private String title;

    /**
     * Swagger文档版本
     */
    @Column(name = "version", length = 50)
    private String version;

    /**
     * Swagger文档描述
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * Swagger文档版本（v2/v3）
     */
    @Column(name = "swagger_version", length = 10)
    private String swaggerVersion;

    /**
     * 文档原始内容（可选，用于备份）
     */
    @Column(name = "content", columnDefinition = "LONGTEXT")
    private String content;

    /**
     * 文档来源（file/url/content）
     */
    @Column(name = "source", length = 50)
    private String source;

    /**
     * 文档来源URL（如果通过URL导入）
     */
    @Column(name = "source_url", length = 500)
    private String sourceUrl;

    /**
     * 创建时间
     */
    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @Column(name = "update_time")
    private LocalDateTime updateTime;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
        updateTime = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updateTime = LocalDateTime.now();
    }
}

