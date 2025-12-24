package com.simulator.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 服务器信息实体类
 * 
 * @author simulator
 * @date 2024
 */
@Entity
@Table(name = "server_info", indexes = {
    @Index(name = "idx_swagger_id", columnList = "swagger_id")
})
@Data
public class ServerInfo {

    /**
     * 主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Swagger文档ID（关联swagger_info表）
     */
    @Column(name = "swagger_id", nullable = false)
    private Long swaggerId;

    /**
     * 服务器URL
     */
    @Column(name = "server_url", nullable = false, length = 500)
    private String serverUrl;

    /**
     * 服务器描述
     */
    @Column(name = "description", length = 500)
    private String description;

    /**
     * 创建时间
     */
    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
    }
}


