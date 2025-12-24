package com.simulator.dto;

import lombok.Data;

/**
 * 请求参数DTO
 * 
 * @author simulator
 * @date 2024
 */
@Data
public class RequestParamDTO {

    private Long id;
    private Long apiId;
    private String paramName;
    private String location;
    private String contentType;
    private String paramType;
    private Boolean required;
    private String pattern;
    private String patternExample;
    private String example;
    private String fullJsonExample;
    private String description;
    private String hierarchyPath;
    private Long parentId;
}

