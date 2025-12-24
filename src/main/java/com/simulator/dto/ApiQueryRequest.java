package com.simulator.dto;

import lombok.Data;

/**
 * 接口查询请求DTO
 * 
 * @author simulator
 * @date 2024
 */
@Data
public class ApiQueryRequest {

    /**
     * 接口路径（模糊查询）
     */
    private String path;

    /**
     * 请求方法
     */
    private String method;

    /**
     * 标签
     */
    private String tags;

    /**
     * 页码（从0开始）
     */
    private Integer page = 0;

    /**
     * 每页大小
     */
    private Integer size = 10;

    /**
     * 排序字段
     */
    private String sortBy = "createTime";

    /**
     * 排序方向（ASC/DESC）
     */
    private String sortDir = "DESC";
}

