package com.simulator.controller;

import com.simulator.dto.*;
import com.simulator.service.SwaggerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Swagger解析控制器
 * 
 * @author simulator
 * @date 2024
 */
@Slf4j
@RestController
@RequestMapping("/swagger")
@RequiredArgsConstructor
public class SwaggerController {

    private final SwaggerService swaggerService;

    /**
     * 导入Swagger文档（通过JSON/YAML内容或URL）
     * 
     * @param request 导入请求
     * @return 导入结果
     */
    @PostMapping("/import")
    public ApiResponse<Integer> importSwagger(@Valid @RequestBody SwaggerImportRequest request) {
        try {
            int count = swaggerService.importSwagger(request);
            return ApiResponse.success("成功导入" + count + "个接口", count);
        } catch (Exception e) {
            log.error("导入Swagger文档失败", e);
            return ApiResponse.error("导入失败: " + e.getMessage());
        }
    }

    /**
     * 通过文件上传导入Swagger文档
     * 
     * @param file 上传的文件（支持.json和.yaml/.yml格式）
     * @return 导入结果
     */
    @PostMapping("/import/file")
    public ApiResponse<Integer> importSwaggerByFile(@RequestParam("file") MultipartFile file) {
        try {
            // 验证文件是否为空
            if (file == null || file.isEmpty()) {
                return ApiResponse.error(400, "上传的文件不能为空");
            }

            // 获取文件名
            String fileName = file.getOriginalFilename();
            if (fileName == null || fileName.isEmpty()) {
                return ApiResponse.error(400, "文件名不能为空");
            }

            // 验证文件格式
            String contentType = determineContentType(fileName);
            if (contentType == null) {
                return ApiResponse.error(400, "不支持的文件格式，仅支持.json、.yaml、.yml格式");
            }

            // 读取文件内容
            String content = new String(file.getBytes(), StandardCharsets.UTF_8);
            if (content == null || content.trim().isEmpty()) {
                return ApiResponse.error(400, "文件内容不能为空");
            }

            // 构建导入请求
            SwaggerImportRequest request = new SwaggerImportRequest();
            request.setContent(content);
            request.setContentType(contentType);

            // 执行导入
            int count = swaggerService.importSwagger(request);
            return ApiResponse.success("成功导入" + count + "个接口（文件：" + fileName + "）", count);
            
        } catch (Exception e) {
            log.error("通过文件导入Swagger文档失败", e);
            return ApiResponse.error("导入失败: " + e.getMessage());
        }
    }

    /**
     * 根据文件名判断内容类型
     * 
     * @param fileName 文件名
     * @return 内容类型（json/yaml），如果不支持则返回null
     */
    private String determineContentType(String fileName) {
        if (fileName == null) {
            return null;
        }
        
        String lowerFileName = fileName.toLowerCase();
        if (lowerFileName.endsWith(".json")) {
            return "json";
        } else if (lowerFileName.endsWith(".yaml") || lowerFileName.endsWith(".yml")) {
            return "yaml";
        }
        
        return null;
    }

    /**
     * 查询接口列表
     * 
     * @param request 查询请求
     * @return 接口列表（分页）
     */
    @GetMapping("/apis")
    public ApiResponse<Page<ApiInfoDTO>> queryApiList(@Valid ApiQueryRequest request) {
        try {
            Page<ApiInfoDTO> page = swaggerService.queryApiList(request);
            return ApiResponse.success(page);
        } catch (Exception e) {
            log.error("查询接口列表失败", e);
            return ApiResponse.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 获取接口详情
     * 
     * @param apiId 接口ID
     * @return 接口详情
     */
    @GetMapping("/apis/{apiId}")
    public ApiResponse<ApiDetailDTO> getApiDetail(@PathVariable Long apiId) {
        try {
            ApiDetailDTO detail = swaggerService.getApiDetail(apiId);
            return ApiResponse.success(detail);
        } catch (Exception e) {
            log.error("获取接口详情失败", e);
            return ApiResponse.error("获取详情失败: " + e.getMessage());
        }
    }

    /**
     * 根据位置查询请求参数
     * 
     * @param apiId 接口ID
     * @param location 参数位置（path/header/query/form/body）
     * @return 请求参数列表
     */
    @GetMapping("/apis/{apiId}/request-params")
    public ApiResponse<List<RequestParamDTO>> getRequestParamsByLocation(
            @PathVariable Long apiId,
            @RequestParam(required = false) String location) {
        try {
            List<RequestParamDTO> params;
            if (location != null && !location.isEmpty()) {
                params = swaggerService.getRequestParamsByLocation(apiId, location);
            } else {
                // 如果没有指定location，返回所有请求参数
                ApiDetailDTO detail = swaggerService.getApiDetail(apiId);
                params = detail.getRequestParams();
            }
            return ApiResponse.success(params);
        } catch (Exception e) {
            log.error("查询请求参数失败", e);
            return ApiResponse.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 根据类型查询请求参数
     * 
     * @param apiId 接口ID
     * @param paramType 参数类型
     * @return 请求参数列表
     */
    @GetMapping("/apis/{apiId}/request-params/type")
    public ApiResponse<List<RequestParamDTO>> getRequestParamsByType(
            @PathVariable Long apiId,
            @RequestParam String paramType) {
        try {
            List<RequestParamDTO> params = swaggerService.getRequestParamsByType(apiId, paramType);
            return ApiResponse.success(params);
        } catch (Exception e) {
            log.error("查询请求参数失败", e);
            return ApiResponse.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 根据状态码查询响应参数
     * 
     * @param apiId 接口ID
     * @param statusCode 状态码
     * @param location 响应位置（body/header），可选
     * @return 响应参数列表
     */
    @GetMapping("/apis/{apiId}/response-params")
    public ApiResponse<List<ResponseParamDTO>> getResponseParams(
            @PathVariable Long apiId,
            @RequestParam(required = false) String statusCode,
            @RequestParam(required = false) String location) {
        try {
            List<ResponseParamDTO> params;
            if (statusCode != null && !statusCode.isEmpty() && location != null && !location.isEmpty()) {
                // 同时指定状态码和位置
                params = swaggerService.getResponseParamsByStatusCodeAndLocation(apiId, statusCode, location);
            } else if (statusCode != null && !statusCode.isEmpty()) {
                // 只指定状态码
                params = swaggerService.getResponseParamsByStatusCode(apiId, statusCode);
            } else if (location != null && !location.isEmpty()) {
                // 只指定位置
                params = swaggerService.getResponseParamsByLocation(apiId, location);
            } else {
                // 都没有指定，返回所有响应参数
                ApiDetailDTO detail = swaggerService.getApiDetail(apiId);
                params = detail.getResponseParams();
            }
            return ApiResponse.success(params);
        } catch (Exception e) {
            log.error("查询响应参数失败", e);
            return ApiResponse.error("查询失败: " + e.getMessage());
        }
    }
}

