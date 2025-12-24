package com.simulator.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Swagger导入请求DTO
 * 
 * @author simulator
 * @date 2024
 */
@Data
public class SwaggerImportRequest {

    /**
     * Swagger文档内容（JSON或YAML格式）
     */
    @NotBlank(message = "Swagger文档内容不能为空")
    private String content;

    /**
     * 文档类型（json/yaml）
     */
    private String contentType = "json";

    /**
     * Swagger文档URL（如果通过URL导入）
     */
    private String url;
}

