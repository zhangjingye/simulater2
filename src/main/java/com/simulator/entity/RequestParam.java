package com.simulator.entity;

import jakarta.persistence.*;
import lombok.Data;

/**
 * 请求参数实体类
 * 
 * @author simulator
 * @date 2024
 */
@Entity
@Table(name = "request_param", indexes = {
    @Index(name = "idx_api_id", columnList = "api_id"),
    @Index(name = "idx_location", columnList = "location"),
    @Index(name = "idx_param_type", columnList = "param_type")
})
@Data
public class RequestParam {

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
     * 参数名称
     */
    @Column(name = "param_name", nullable = false, length = 200)
    private String paramName;

    /**
     * 参数位置（path/header/query/form/body等）
     */
    @Column(name = "location", nullable = false, length = 50)
    private String location;

    /**
     * Content类型（仅用于body类型参数，如application/json、application/xml等）
     */
    @Column(name = "content_type", length = 100)
    private String contentType;

    /**
     * 数据类型（String/Number/Boolean/Array/Object等）
     */
    @Column(name = "param_type", nullable = false, length = 50)
    private String paramType;

    /**
     * 是否必填（true/false）
     */
    @Column(name = "required", nullable = false)
    private Boolean required = false;

    /**
     * 正则表达式
     */
    @Column(name = "pattern", length = 5000)
    private String pattern;

    /**
     * 正则示例值（根据正则自动生成）
     */
    @Column(name = "pattern_example", length = 500)
    private String patternExample;

    /**
     * 示例值（对于body类型，保存完整的JSON示例；对于其他类型，保存单个字段的示例值）
     */
    @Column(name = "example", columnDefinition = "LONGTEXT")
    private String example;

    /**
     * 完整JSON示例（用于body类型，包含所有嵌套对象的完整JSON）
     */
    @Column(name = "full_json_example", columnDefinition = "LONGTEXT")
    private String fullJsonExample;

    /**
     * 参数描述
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * 层级路径（用于扁平化，如：user.name、items[0].id）
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


