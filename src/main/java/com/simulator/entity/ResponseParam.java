package com.simulator.entity;

import jakarta.persistence.*;
import lombok.Data;

/**
 * 响应参数实体类
 * 
 * @author simulator
 * @date 2024
 */
@Entity
@Table(name = "response_param", indexes = {
    @Index(name = "idx_api_id", columnList = "api_id"),
    @Index(name = "idx_status_code", columnList = "status_code"),
    @Index(name = "idx_location", columnList = "location")
})
@Data
public class ResponseParam {

    /**
     * 主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 关联的接口ID
     */
    @Column(name = "api_id", nullable = false)
    private Long apiId;

    /**
     * HTTP状态码（200/400/500等）
     */
    @Column(name = "status_code", nullable = false, length = 10)
    private String statusCode;

    /**
     * 参数名称
     */
    @Column(name = "param_name", nullable = false, length = 200)
    private String paramName;

    /**
     * 响应位置（body/header）
     */
    @Column(name = "location", nullable = false, length = 50)
    private String location;

    /**
     * 数据类型（String/Number/Boolean/Array/Object等）
     */
    @Column(name = "param_type", nullable = false, length = 50)
    private String paramType;

    /**
     * 正则表达式
     */
    @Column(name = "pattern", length = 1000)
    private String pattern;

    /**
     * 示例值
     */
    @Column(name = "example", columnDefinition = "LONGTEXT")
    private String example;

    /**
     * 参数描述
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * 层级路径（用于扁平化，如：data.user.name、items[0].id）
     */
    @Column(name = "hierarchy_path", length = 500)
    private String hierarchyPath;

    /**
     * 父参数ID（用于构建层级关系）
     */
    @Column(name = "parent_id")
    private Long parentId;

    /**
     * 关联的接口信息
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "api_id", insertable = false, updatable = false)
    private ApiInfo apiInfo;
}


