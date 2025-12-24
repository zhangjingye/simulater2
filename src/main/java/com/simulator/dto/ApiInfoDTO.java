package com.simulator.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 接口信息DTO
 * 
 * @author simulator
 * @date 2024
 */
@Data
public class ApiInfoDTO {

    private Long id;
    private String path;
    private String method;
    private Long swaggerId;
    private String description;
    private String tags;
    private String operationId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

