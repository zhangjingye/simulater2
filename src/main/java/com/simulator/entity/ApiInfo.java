package com.simulator.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 接口基础信息实体类
 * 
 * @author simulator
 * @date 2024
 */
@Entity
@Table(name = "api_info", indexes = {
    @Index(name = "idx_path_method", columnList = "path,method"),
    @Index(name = "idx_swagger_id", columnList = "swagger_id")
})
@Data
public class ApiInfo {

    /**
     * 主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 接口路径
     */
    @Column(name = "path", nullable = false, length = 500)
    private String path;

    /**
     * 请求方法（GET/POST/PUT/DELETE等）
     */
    @Column(name = "method", nullable = false, length = 10)
    private String method;

    /**
     * Swagger文档ID（关联swagger_info表）
     */
    @Column(name = "swagger_id", nullable = false)
    private Long swaggerId;

    /**
     * 接口描述
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * 接口标签/分组
     */
    @Column(name = "tags", length = 200)
    private String tags;

    /**
     * 操作ID（OpenAPI中的operationId）
     */
    @Column(name = "operation_id", length = 200)
    private String operationId;

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

    /**
     * 请求参数列表（一对多关系）
     */
    @OneToMany(mappedBy = "apiInfo", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RequestParam> requestParams;

    /**
     * 响应参数列表（一对多关系）
     */
    @OneToMany(mappedBy = "apiInfo", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ResponseParam> responseParams;

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


