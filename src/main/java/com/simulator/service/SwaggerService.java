package com.simulator.service;

import com.simulator.dto.*;
import com.simulator.entity.*;
import com.simulator.parser.SwaggerParser;
import com.simulator.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Swagger服务类
 * 
 * @author simulator
 * @date 2024
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SwaggerService {

    private final SwaggerParser swaggerParser;
    private final SwaggerInfoRepository swaggerInfoRepository;
    private final ApiInfoRepository apiInfoRepository;
    private final RequestParamRepository requestParamRepository;
    private final ResponseParamRepository responseParamRepository;
    private final ServerInfoRepository serverInfoRepository;

    /**
     * 导入Swagger文档
     * 
     * @param request 导入请求
     * @return 导入的接口数量
     */
    @Transactional(rollbackFor = Exception.class)
    public int importSwagger(SwaggerImportRequest request) {
        try {
            String content = request.getContent();
            
            // 如果提供了URL，从URL获取内容
            if (request.getUrl() != null && !request.getUrl().isEmpty()) {
                content = fetchContentFromUrl(request.getUrl());
            }
            
            // 解析Swagger文档
            SwaggerParser.ParseResult parseResult = swaggerParser.parse(content, request.getContentType());
            
            // 1. 保存Swagger文档信息
            SwaggerInfo swaggerInfo = parseResult.getSwaggerInfo();
            if (request.getUrl() != null && !request.getUrl().isEmpty()) {
                swaggerInfo.setSource("url");
                swaggerInfo.setSourceUrl(request.getUrl());
            } else {
                swaggerInfo.setSource("file");
            }
            SwaggerInfo savedSwaggerInfo = swaggerInfoRepository.save(swaggerInfo);
            Long swaggerId = savedSwaggerInfo.getId();
            
            // 2. 保存服务器信息（关联swaggerId）
            for (ServerInfo server : parseResult.getServers()) {
                server.setSwaggerId(swaggerId);
                serverInfoRepository.save(server);
            }
            
            // 3. 保存接口信息（关联swaggerId）
            int count = 0;
            for (ApiInfo apiInfo : parseResult.getApiInfos()) {
                // 设置swaggerId
                apiInfo.setSwaggerId(swaggerId);
                
                // 查找是否已存在相同swaggerId、路径和方法的接口
                apiInfoRepository.findByPathAndMethod(apiInfo.getPath(), apiInfo.getMethod())
                    .filter(existing -> existing.getSwaggerId().equals(swaggerId))
                    .ifPresentOrElse(
                        existing -> {
                            // 更新现有接口
                            Long apiId = existing.getId();
                            // 删除旧的关联数据
                            requestParamRepository.deleteByApiId(apiId);
                            responseParamRepository.deleteByApiId(apiId);
                            
                            // 更新接口信息
                            existing.setDescription(apiInfo.getDescription());
                            existing.setTags(apiInfo.getTags());
                            existing.setOperationId(apiInfo.getOperationId());
                            apiInfoRepository.save(existing);
                            
                            // 保存新的关联数据
                            saveApiRelatedData(existing.getId(), apiInfo);
                        },
                        () -> {
                            // 保存新接口
                            // 临时保存关联数据，避免级联保存时apiId为null
                            List<RequestParam> tempRequestParams = apiInfo.getRequestParams();
                            List<ResponseParam> tempResponseParams = apiInfo.getResponseParams();
                            
                            // 清空关联列表，避免级联保存
                            apiInfo.setRequestParams(null);
                            apiInfo.setResponseParams(null);
                            
                            // 先保存ApiInfo，获取ID
                            ApiInfo saved = apiInfoRepository.save(apiInfo);
                            
                            // 恢复关联数据并设置apiId
                            if (tempRequestParams != null) {
                                apiInfo.setRequestParams(tempRequestParams);
                            }
                            if (tempResponseParams != null) {
                                apiInfo.setResponseParams(tempResponseParams);
                            }
                            
                            // 保存关联数据
                            saveApiRelatedData(saved.getId(), apiInfo);
                        }
                    );
                count++;
            }
            
            log.info("成功导入Swagger文档：{}，共{}个接口", savedSwaggerInfo.getTitle(), count);
            return count;
            
        } catch (Exception e) {
            log.error("导入Swagger文档失败", e);
            throw new RuntimeException("导入Swagger文档失败: " + e.getMessage(), e);
        }
    }

    /**
     * 保存接口关联数据（请求参数、响应参数、头部信息）
     */
    private void saveApiRelatedData(Long apiId, ApiInfo apiInfo) {
        // 保存请求参数
        if (apiInfo.getRequestParams() != null) {
            for (RequestParam param : apiInfo.getRequestParams()) {
                param.setApiId(apiId);
                requestParamRepository.save(param);
            }
        }
        
        // 保存响应参数
        if (apiInfo.getResponseParams() != null) {
            for (ResponseParam param : apiInfo.getResponseParams()) {
                param.setApiId(apiId);
                responseParamRepository.save(param);
            }
        }
    }

    /**
     * 从URL获取内容
     */
    private String fetchContentFromUrl(String url) {
        try {
            return cn.hutool.http.HttpUtil.get(url);
        } catch (Exception e) {
            throw new RuntimeException("从URL获取Swagger文档失败: " + e.getMessage(), e);
        }
    }

    /**
     * 查询接口列表
     * 
     * @param request 查询请求
     * @return 分页结果
     */
    public Page<ApiInfoDTO> queryApiList(ApiQueryRequest request) {
        // 构建分页和排序
        Sort sort = Sort.by(
            "DESC".equalsIgnoreCase(request.getSortDir()) 
                ? Sort.Direction.DESC 
                : Sort.Direction.ASC,
            request.getSortBy()
        );
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), sort);
        
        Page<ApiInfo> page;
        
        // 根据条件查询
        if (request.getPath() != null && !request.getPath().isEmpty()) {
            page = apiInfoRepository.findByPathContaining(request.getPath(), pageable);
        } else if (request.getMethod() != null && !request.getMethod().isEmpty()) {
            page = apiInfoRepository.findByMethod(request.getMethod(), pageable);
        } else if (request.getTags() != null && !request.getTags().isEmpty()) {
            page = apiInfoRepository.findByTagsContaining(request.getTags(), pageable);
        } else {
            page = apiInfoRepository.findAll(pageable);
        }
        
        return page.map(this::convertToDTO);
    }

    /**
     * 获取接口详情
     * 
     * @param apiId 接口ID
     * @return 接口详情
     */
    public ApiDetailDTO getApiDetail(Long apiId) {
        ApiInfo apiInfo = apiInfoRepository.findById(apiId)
            .orElseThrow(() -> new RuntimeException("接口不存在: " + apiId));
        
        ApiDetailDTO detail = new ApiDetailDTO();
        detail.setApiInfo(convertToDTO(apiInfo));
        
        // 查询请求参数
        List<RequestParam> requestParams = requestParamRepository.findByApiId(apiId);
        detail.setRequestParams(requestParams.stream()
            .map(this::convertRequestParamToDTO)
            .collect(Collectors.toList()));
        
        // 查询响应参数
        List<ResponseParam> responseParams = responseParamRepository.findByApiId(apiId);
        detail.setResponseParams(responseParams.stream()
            .map(this::convertResponseParamToDTO)
            .collect(Collectors.toList()));
        
        return detail;
    }

    /**
     * 根据位置查询请求参数
     * 
     * @param apiId 接口ID
     * @param location 参数位置
     * @return 请求参数列表
     */
    public List<RequestParamDTO> getRequestParamsByLocation(Long apiId, String location) {
        List<RequestParam> params = requestParamRepository.findByApiIdAndLocation(apiId, location);
        return params.stream()
            .map(this::convertRequestParamToDTO)
            .collect(Collectors.toList());
    }

    /**
     * 根据类型查询请求参数
     * 
     * @param apiId 接口ID
     * @param paramType 参数类型
     * @return 请求参数列表
     */
    public List<RequestParamDTO> getRequestParamsByType(Long apiId, String paramType) {
        List<RequestParam> params = requestParamRepository.findByApiIdAndParamType(apiId, paramType);
        return params.stream()
            .map(this::convertRequestParamToDTO)
            .collect(Collectors.toList());
    }

    /**
     * 根据状态码查询响应参数
     * 
     * @param apiId 接口ID
     * @param statusCode 状态码
     * @return 响应参数列表
     */
    public List<ResponseParamDTO> getResponseParamsByStatusCode(Long apiId, String statusCode) {
        List<ResponseParam> params = responseParamRepository.findByApiIdAndStatusCode(apiId, statusCode);
        return params.stream()
            .map(this::convertResponseParamToDTO)
            .collect(Collectors.toList());
    }

    /**
     * 根据位置查询响应参数
     * 
     * @param apiId 接口ID
     * @param location 响应位置（body/header）
     * @return 响应参数列表
     */
    public List<ResponseParamDTO> getResponseParamsByLocation(Long apiId, String location) {
        List<ResponseParam> params = responseParamRepository.findByApiIdAndLocation(apiId, location);
        return params.stream()
            .map(this::convertResponseParamToDTO)
            .collect(Collectors.toList());
    }

    /**
     * 根据状态码和位置查询响应参数
     * 
     * @param apiId 接口ID
     * @param statusCode 状态码
     * @param location 响应位置（body/header）
     * @return 响应参数列表
     */
    public List<ResponseParamDTO> getResponseParamsByStatusCodeAndLocation(Long apiId, String statusCode, String location) {
        List<ResponseParam> params = responseParamRepository.findByApiIdAndStatusCodeAndLocation(apiId, statusCode, location);
        return params.stream()
            .map(this::convertResponseParamToDTO)
            .collect(Collectors.toList());
    }

    /**
     * 转换ApiInfo为DTO
     */
    private ApiInfoDTO convertToDTO(ApiInfo apiInfo) {
        ApiInfoDTO dto = new ApiInfoDTO();
        dto.setId(apiInfo.getId());
        dto.setPath(apiInfo.getPath());
        dto.setMethod(apiInfo.getMethod());
        dto.setSwaggerId(apiInfo.getSwaggerId());
        dto.setDescription(apiInfo.getDescription());
        dto.setTags(apiInfo.getTags());
        dto.setOperationId(apiInfo.getOperationId());
        dto.setCreateTime(apiInfo.getCreateTime());
        dto.setUpdateTime(apiInfo.getUpdateTime());
        return dto;
    }

    /**
     * 转换RequestParam为DTO
     */
    private RequestParamDTO convertRequestParamToDTO(RequestParam param) {
        RequestParamDTO dto = new RequestParamDTO();
        dto.setId(param.getId());
        dto.setApiId(param.getApiId());
        dto.setParamName(param.getParamName());
        dto.setLocation(param.getLocation());
        dto.setContentType(param.getContentType());
        dto.setParamType(param.getParamType());
        dto.setRequired(param.getRequired());
        dto.setPattern(param.getPattern());
        dto.setPatternExample(param.getPatternExample());
        dto.setExample(param.getExample());
        dto.setFullJsonExample(param.getFullJsonExample());
        dto.setDescription(param.getDescription());
        dto.setHierarchyPath(param.getHierarchyPath());
        dto.setParentId(param.getParentId());
        return dto;
    }

    /**
     * 转换ResponseParam为DTO
     */
    private ResponseParamDTO convertResponseParamToDTO(ResponseParam param) {
        ResponseParamDTO dto = new ResponseParamDTO();
        dto.setId(param.getId());
        dto.setApiId(param.getApiId());
        dto.setStatusCode(param.getStatusCode());
        dto.setLocation(param.getLocation());
        dto.setParamName(param.getParamName());
        dto.setParamType(param.getParamType());
        dto.setPattern(param.getPattern());
        dto.setExample(param.getExample());
        dto.setDescription(param.getDescription());
        dto.setHierarchyPath(param.getHierarchyPath());
        dto.setParentId(param.getParentId());
        return dto;
    }
}

