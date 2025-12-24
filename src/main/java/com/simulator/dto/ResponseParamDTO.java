package com.simulator.dto;

import lombok.Data;

/**
 * 响应参数DTO
 * 
 * @author simulator
 * @date 2024
 */
@Data
public class ResponseParamDTO {

    private Long id;
    private Long apiId;
    private String statusCode;
    private String location;
    private String paramName;
    private String paramType;
    private String pattern;
    private String example;
    private String description;
    private String hierarchyPath;
    private Long parentId;
}

