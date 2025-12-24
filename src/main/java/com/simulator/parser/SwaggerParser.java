package com.simulator.parser;

import com.simulator.entity.*;
import com.simulator.util.RegexExampleGenerator;
import com.simulator.util.Swagger2JsonExampleGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.models.*;
import io.swagger.models.parameters.*;
import io.swagger.models.properties.*;
import io.swagger.util.Json;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.*;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Swagger解析器
 * 支持Swagger2和OpenAPI3格式
 * 
 * @author simulator
 * @date 2024
 */
@Slf4j
@Component
public class SwaggerParser {

    /**
     * 解析Swagger文档
     * 
     * @param content Swagger文档内容（JSON或YAML）
     * @param contentType 内容类型（json/yaml）
     * @return 解析结果，包含所有接口信息
     */
    public ParseResult parse(String content, String contentType) {
        ParseResult result = new ParseResult();
        
        try {
            // 检测文档版本
            String version = detectSwaggerVersion(content);
            log.info("检测到Swagger版本: {}", version);
            
            if ("v2".equals(version)) {
                // 使用Swagger 2.0原生解析
                return parseSwagger2(content);
            } else {
                // 使用OpenAPI 3.0解析
                return parseOpenAPI3(content);
            }
            
        } catch (Exception e) {
            log.error("解析Swagger文档失败", e);
            throw new RuntimeException("解析Swagger文档失败: " + e.getMessage(), e);
        }
    }

    /**
     * 检测Swagger文档版本
     */
    private String detectSwaggerVersion(String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new RuntimeException("Swagger文档内容为空");
        }
        
        // 检查是否包含 "swagger": "2.0"
        if (content.contains("\"swagger\":\"2.0\"") || 
            content.contains("\"swagger\": \"2.0\"") ||
            content.contains("swagger: '2.0'") ||
            content.contains("swagger: \"2.0\"")) {
            return "v2";
        }
        
        // 检查是否包含 "openapi": "3"
        if (content.contains("\"openapi\":\"3") || 
            content.contains("\"openapi\": \"3") ||
            content.contains("openapi: '3") ||
            content.contains("openapi: \"3")) {
            return "v3";
        }
        
        // 默认尝试OpenAPI 3.0解析
        return "v3";
    }

    /**
     * 解析OpenAPI 3.0文档
     */
    private ParseResult parseOpenAPI3(String content) {
        ParseResult result = new ParseResult();
        
        // 使用swagger-parser解析
        OpenAPIV3Parser parser = new OpenAPIV3Parser();
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        options.setFlatten(true);
        
        SwaggerParseResult parseResult = parser.readContents(content, null, options);
        OpenAPI openAPI = parseResult.getOpenAPI();
        
        if (openAPI == null) {
            throw new RuntimeException("无法解析OpenAPI文档: " + parseResult.getMessages());
        }
        
        // 解析Swagger文档信息
        SwaggerInfo swaggerInfo = parseSwaggerInfo(openAPI, "v3", content);
        result.setSwaggerInfo(swaggerInfo);
        
        // 解析服务器信息
        List<ServerInfo> servers = parseServers(openAPI);
        result.setServers(servers);
        
        // 解析接口信息
        List<ApiInfo> apiInfos = new ArrayList<>();
        if (openAPI.getPaths() != null) {
            for (Map.Entry<String, PathItem> pathEntry : openAPI.getPaths().entrySet()) {
                String path = pathEntry.getKey();
                PathItem pathItem = pathEntry.getValue();
                
                // 解析各个HTTP方法的接口
                parsePathItem(path, pathItem, openAPI, apiInfos, "v3");
            }
        }
        
        result.setApiInfos(apiInfos);
        return result;
    }

    /**
     * 解析Swagger 2.0文档
     */
    private ParseResult parseSwagger2(String content) {
        ParseResult result = new ParseResult();
        
        try {
            // 使用 swagger-parser 解析（支持JSON和YAML格式）
            io.swagger.models.Swagger swagger = null;
            
            try {
                // 方法1: 使用 swagger-parser 解析（推荐方法，支持JSON和YAML）
                io.swagger.parser.SwaggerParser swaggerParser = new io.swagger.parser.SwaggerParser();
                swagger = swaggerParser.parse(content);
                log.debug("使用 SwaggerParser 成功解析 Swagger 2.0 文档");
            } catch (Exception e1) {
                log.warn("使用 SwaggerParser 解析失败: {}", e1.getMessage(), e1);
                
                try {
                    // 方法2: 使用 swagger-models 的 Json.mapper() 解析（仅支持JSON）
                    ObjectMapper mapper = Json.mapper();
                    swagger = mapper.readValue(content, io.swagger.models.Swagger.class);
                    log.debug("使用 Json.mapper() 成功解析 Swagger 2.0 文档（JSON格式）");
                } catch (Exception e2) {
                    log.warn("使用 Json.mapper() 解析失败: {}", e2.getMessage(), e2);
                    
                    try {
                        // 方法3: 使用标准的 ObjectMapper（仅支持JSON）
                        ObjectMapper objectMapper = new ObjectMapper();
                        swagger = objectMapper.readValue(content, io.swagger.models.Swagger.class);
                        log.debug("使用 ObjectMapper 成功解析 Swagger 2.0 文档（JSON格式）");
                    } catch (Exception e3) {
                        log.error("所有解析方法都失败。SwaggerParser 错误: {}, Json.mapper() 错误: {}, ObjectMapper 错误: {}", 
                            e1.getMessage(), e2.getMessage(), e3.getMessage(), e3);
                        throw new RuntimeException("无法解析Swagger 2.0文档。SwaggerParser 错误: " + e1.getMessage() + 
                            "，Json.mapper() 错误: " + e2.getMessage() + 
                            "，ObjectMapper 错误: " + e3.getMessage() + 
                            (e3.getCause() != null ? "，原因: " + e3.getCause().getMessage() : ""), e3);
                    }
                }
            }
            
            if (swagger == null) {
                throw new RuntimeException("无法解析Swagger 2.0文档：解析器返回null");
            }
            
            log.info("成功解析 Swagger 2.0 文档，版本: {}, 标题: {}", 
                swagger.getSwagger(), 
                swagger.getInfo() != null ? swagger.getInfo().getTitle() : "未知");
            
            // 解析Swagger文档信息
            SwaggerInfo swaggerInfo = parseSwagger2Info(swagger, content);
            result.setSwaggerInfo(swaggerInfo);
            
            // 解析服务器信息
            List<ServerInfo> servers = parseSwagger2Servers(swagger);
            result.setServers(servers);
            
            // 解析接口信息
            List<ApiInfo> apiInfos = new ArrayList<>();
            if (swagger.getPaths() != null) {
                for (Map.Entry<String, Path> pathEntry : swagger.getPaths().entrySet()) {
                    String path = pathEntry.getKey();
                    Path pathItem = pathEntry.getValue();
                    
                    // 解析各个HTTP方法的接口
                    parseSwagger2PathItem(path, pathItem, swagger, apiInfos);
                }
            }
            
            result.setApiInfos(apiInfos);
            return result;
            
        } catch (Exception e) {
            log.error("解析Swagger 2.0文档失败", e);
            throw new RuntimeException("解析Swagger 2.0文档失败: " + e.getMessage(), e);
        }
    }

    /**
     * 解析Swagger文档信息（OpenAPI 3.0）
     */
    private SwaggerInfo parseSwaggerInfo(OpenAPI openAPI, String swaggerVersion, String content) {
        SwaggerInfo swaggerInfo = new SwaggerInfo();
        
        Info info = openAPI.getInfo();
        if (info != null) {
            swaggerInfo.setTitle(info.getTitle());
            swaggerInfo.setVersion(info.getVersion());
            swaggerInfo.setDescription(info.getDescription());
        }
        
        swaggerInfo.setSwaggerVersion(swaggerVersion);
        swaggerInfo.setContent(content);
        swaggerInfo.setSource("content");
        
        return swaggerInfo;
    }

    /**
     * 解析Swagger 2.0文档信息
     */
    private SwaggerInfo parseSwagger2Info(io.swagger.models.Swagger swagger, String content) {
        SwaggerInfo swaggerInfo = new SwaggerInfo();
        
        io.swagger.models.Info info = swagger.getInfo();
        if (info != null) {
            swaggerInfo.setTitle(info.getTitle());
            swaggerInfo.setVersion(info.getVersion());
            swaggerInfo.setDescription(info.getDescription());
        }
        
        swaggerInfo.setSwaggerVersion("v2");
        swaggerInfo.setContent(content);
        swaggerInfo.setSource("content");
        
        return swaggerInfo;
    }

    /**
     * 解析服务器信息（OpenAPI 3.0）
     */
    private List<ServerInfo> parseServers(OpenAPI openAPI) {
        List<ServerInfo> servers = new ArrayList<>();
        
        if (openAPI.getServers() != null && !openAPI.getServers().isEmpty()) {
            for (io.swagger.v3.oas.models.servers.Server server : openAPI.getServers()) {
                ServerInfo serverInfo = new ServerInfo();
                serverInfo.setServerUrl(server.getUrl());
                serverInfo.setDescription(server.getDescription());
                servers.add(serverInfo);
            }
        } else {
            // 如果没有servers，创建一个默认的
            ServerInfo defaultServer = new ServerInfo();
            defaultServer.setServerUrl("http://localhost:8080");
            defaultServer.setDescription("默认服务器");
            servers.add(defaultServer);
        }
        
        return servers;
    }

    /**
     * 解析Swagger 2.0服务器信息（host, basePath, schemes）
     */
    private List<ServerInfo> parseSwagger2Servers(io.swagger.models.Swagger swagger) {
        List<ServerInfo> servers = new ArrayList<>();
        
        String host = swagger.getHost();
        String basePath = swagger.getBasePath();
        List<Scheme> schemes = swagger.getSchemes();
        
        // 构建服务器URL
        if (host != null && !host.isEmpty()) {
            String scheme = (schemes != null && !schemes.isEmpty()) ? schemes.get(0).toValue() : "https";
            String url = scheme + "://" + host + (basePath != null ? basePath : "");
            
            ServerInfo serverInfo = new ServerInfo();
            serverInfo.setServerUrl(url);
            serverInfo.setDescription("Swagger 2.0服务器");
            servers.add(serverInfo);
            
            // 如果有多个schemes，为每个scheme创建一个服务器
            if (schemes != null && schemes.size() > 1) {
                for (int i = 1; i < schemes.size(); i++) {
                    ServerInfo additionalServer = new ServerInfo();
                    additionalServer.setServerUrl(schemes.get(i).toValue() + "://" + host + (basePath != null ? basePath : ""));
                    additionalServer.setDescription("Swagger 2.0服务器 (" + schemes.get(i).toValue() + ")");
                    servers.add(additionalServer);
                }
            }
        } else {
            // 如果没有host，创建一个默认的
            ServerInfo defaultServer = new ServerInfo();
            defaultServer.setServerUrl("http://localhost:8080" + (basePath != null ? basePath : ""));
            defaultServer.setDescription("默认服务器");
            servers.add(defaultServer);
        }
        
        return servers;
    }

    /**
     * 解析PathItem（包含多个HTTP方法）
     */
    private void parsePathItem(String path, PathItem pathItem, OpenAPI openAPI, 
                               List<ApiInfo> apiInfos, String version) {
        // GET方法
        if (pathItem.getGet() != null) {
            apiInfos.add(parseOperation(path, "GET", pathItem.getGet(), openAPI, version));
        }
        
        // POST方法
        if (pathItem.getPost() != null) {
            apiInfos.add(parseOperation(path, "POST", pathItem.getPost(), openAPI, version));
        }
        
        // PUT方法
        if (pathItem.getPut() != null) {
            apiInfos.add(parseOperation(path, "PUT", pathItem.getPut(), openAPI, version));
        }
        
        // DELETE方法
        if (pathItem.getDelete() != null) {
            apiInfos.add(parseOperation(path, "DELETE", pathItem.getDelete(), openAPI, version));
        }
        
        // PATCH方法
        if (pathItem.getPatch() != null) {
            apiInfos.add(parseOperation(path, "PATCH", pathItem.getPatch(), openAPI, version));
        }
    }

    /**
     * 解析单个Operation
     */
    private ApiInfo parseOperation(String path, String method, Operation operation, 
                                   OpenAPI openAPI, String version) {
        ApiInfo apiInfo = new ApiInfo();
        apiInfo.setPath(path);
        apiInfo.setMethod(method);
        apiInfo.setDescription(operation.getDescription());
        apiInfo.setOperationId(operation.getOperationId());
        
        // 解析标签
        if (operation.getTags() != null && !operation.getTags().isEmpty()) {
            apiInfo.setTags(String.join(",", operation.getTags()));
        }
        
        // 解析请求参数
        List<RequestParam> requestParams = new ArrayList<>();
        if (operation.getParameters() != null) {
            for (Parameter parameter : operation.getParameters()) {
                requestParams.addAll(parseParameter(parameter, null, ""));
            }
        }
        
        // 解析请求体
        if (operation.getRequestBody() != null) {
            requestParams.addAll(parseRequestBody(operation.getRequestBody(), "", openAPI));
        }
        
        apiInfo.setRequestParams(requestParams);
        
        // 解析响应
        List<ResponseParam> responseParams = new ArrayList<>();
        if (operation.getResponses() != null) {
            for (Map.Entry<String, ApiResponse> responseEntry : operation.getResponses().entrySet()) {
                String statusCode = responseEntry.getKey();
                ApiResponse apiResponse = responseEntry.getValue();
                responseParams.addAll(parseApiResponse(statusCode, apiResponse, openAPI, ""));
            }
        }
        apiInfo.setResponseParams(responseParams);
        
        // 解析响应头，保存到responseParams中
        if (operation.getResponses() != null) {
            for (Map.Entry<String, ApiResponse> responseEntry : operation.getResponses().entrySet()) {
                String statusCode = responseEntry.getKey();
                ApiResponse apiResponse = responseEntry.getValue();
                if (apiResponse.getHeaders() != null) {
                    for (Map.Entry<String, io.swagger.v3.oas.models.headers.Header> headerEntry : 
                         apiResponse.getHeaders().entrySet()) {
                        io.swagger.v3.oas.models.headers.Header header = headerEntry.getValue();
                        ResponseParam responseParam = new ResponseParam();
                        responseParam.setStatusCode(statusCode);
                        responseParam.setLocation("header");
                        responseParam.setParamName(headerEntry.getKey());
                        responseParam.setDescription(header.getDescription());
                        
                        // 优先使用Header本身的example
                        if (header.getExample() != null) {
                            responseParam.setExample(String.valueOf(header.getExample()));
                        }
                        
                        if (header.getSchema() != null) {
                            responseParam.setParamType(getSchemaType(header.getSchema()));
                            
                            // 提取正则表达式
                            String pattern = getPatternFromSchema(header.getSchema());
                            if (pattern != null) {
                                responseParam.setPattern(pattern);
                            }
                            
                            // 如果Header没有example，则从Schema获取
                            if (responseParam.getExample() == null) {
                                responseParam.setExample(getExampleFromSchema(header.getSchema()));
                            }
                            
                            // 如果仍然没有example但有pattern，根据pattern生成example
                            if (responseParam.getExample() == null && responseParam.getPattern() != null) {
                                String generatedExample = RegexExampleGenerator.generateExample(responseParam.getPattern());
                                if (generatedExample != null) {
                                    responseParam.setExample(generatedExample);
                                }
                            }
                        } else {
                            responseParam.setParamType("String");
                        }
                        
                        responseParams.add(responseParam);
                    }
                }
            }
        }
        
        apiInfo.setResponseParams(responseParams);
        
        return apiInfo;
    }

    /**
     * 解析Parameter（路径参数、查询参数等）
     */
    private List<RequestParam> parseParameter(Parameter parameter, Long parentId, String parentPath) {
        List<RequestParam> params = new ArrayList<>();
        
        RequestParam requestParam = new RequestParam();
        requestParam.setParamName(parameter.getName());
        requestParam.setLocation(parameter.getIn());
        requestParam.setRequired(parameter.getRequired() != null && parameter.getRequired());
        requestParam.setDescription(parameter.getDescription());
        requestParam.setParentId(parentId);
        
        String currentPath = parentPath.isEmpty() ? parameter.getName() : parentPath + "." + parameter.getName();
        requestParam.setHierarchyPath(currentPath);
        
        // 优先使用Parameter本身的example，如果没有则使用Schema的example
        if (parameter.getExample() != null) {
            requestParam.setExample(String.valueOf(parameter.getExample()));
        }
        
        if (parameter.getSchema() != null) {
            requestParam.setParamType(getSchemaType(parameter.getSchema()));
            // 如果Parameter没有example，则从Schema获取
            if (requestParam.getExample() == null) {
                requestParam.setExample(getExampleFromSchema(parameter.getSchema()));
            }
            requestParam.setPattern(getPatternFromSchema(parameter.getSchema()));
            
            // 如果有正则表达式，生成示例值
            if (requestParam.getPattern() != null) {
                requestParam.setPatternExample(RegexExampleGenerator.generateExample(requestParam.getPattern()));
            }
            
            // 如果是数组或对象，递归解析
            if (parameter.getSchema() instanceof ArraySchema) {
                ArraySchema arraySchema = (ArraySchema) parameter.getSchema();
                if (arraySchema.getItems() != null) {
                    // 数组元素类型
                    requestParam.setParamType("Array<" + getSchemaType(arraySchema.getItems()) + ">");
                }
            } else if (parameter.getSchema() instanceof ObjectSchema) {
                // 对象类型，需要扁平化处理
                ObjectSchema objectSchema = (ObjectSchema) parameter.getSchema();
                if (objectSchema.getProperties() != null) {
                    for (Map.Entry<String, Schema> propEntry : objectSchema.getProperties().entrySet()) {
                        Parameter nestedParam = new Parameter();
                        nestedParam.setName(propEntry.getKey());
                        nestedParam.setIn(parameter.getIn());
                        nestedParam.setSchema(propEntry.getValue());
                        params.addAll(parseParameter(nestedParam, null, currentPath));
                    }
                }
            }
        }
        
        params.add(requestParam);
        return params;
    }

    /**
     * 解析RequestBody
     */
    private List<RequestParam> parseRequestBody(RequestBody requestBody, String parentPath, OpenAPI openAPI) {
        List<RequestParam> params = new ArrayList<>();
        
        if (requestBody.getContent() != null) {
            for (Map.Entry<String, MediaType> contentEntry : requestBody.getContent().entrySet()) {
                String contentType = contentEntry.getKey(); // 如 application/json
                MediaType mediaType = contentEntry.getValue();
                if (mediaType.getSchema() != null) {
                    // 生成完整的JSON示例（传入OpenAPI以支持$ref引用解析）
                    String fullJsonExample = com.simulator.util.JsonExampleGenerator.generateJsonExample(
                        mediaType.getSchema(), openAPI);
                    
                    // 创建body参数，包含完整JSON示例
                    RequestParam bodyParam = new RequestParam();
                    bodyParam.setParamName("body");
                    bodyParam.setLocation("body");
                    bodyParam.setContentType(contentType);
                    bodyParam.setParamType(getSchemaType(mediaType.getSchema()));
                    bodyParam.setHierarchyPath(parentPath);
                    bodyParam.setFullJsonExample(fullJsonExample);
                    bodyParam.setDescription(requestBody.getDescription());
                    
                    // 总是将生成的完整JSON示例保存到example字段，供UI使用
                    if (fullJsonExample != null && !fullJsonExample.trim().isEmpty()) {
                        // 将生成的完整JSON示例保存到example字段，供UI使用
                        bodyParam.setExample(fullJsonExample);
                        log.debug("保存完整JSON示例到example字段，长度: {}", fullJsonExample.length());
                    } else {
                        // 如果生成失败，记录日志并使用MediaType的example作为备选
                        log.warn("生成完整JSON示例失败，使用MediaType的example作为备选");
                        if (mediaType.getExample() != null) {
                            bodyParam.setExample(String.valueOf(mediaType.getExample()));
                        }
                    }
                    
                    params.add(bodyParam);
                    
                    // 继续扁平化解析，用于参数填充
                    params.addAll(parseSchema(mediaType.getSchema(), null, parentPath, "body", mediaType, contentType));
                }
            }
        }
        
        return params;
    }

    /**
     * 解析Schema（递归处理嵌套结构）
     */
    private List<RequestParam> parseSchema(Schema schema, Long parentId, String parentPath, String location) {
        return parseSchema(schema, parentId, parentPath, location, null, null);
    }

    /**
     * 解析Schema（递归处理嵌套结构），支持MediaType的example
     */
    private List<RequestParam> parseSchema(Schema schema, Long parentId, String parentPath, String location, MediaType mediaType) {
        return parseSchema(schema, parentId, parentPath, location, mediaType, null);
    }

    /**
     * 解析Schema（递归处理嵌套结构），支持MediaType的example和contentType
     */
    private List<RequestParam> parseSchema(Schema schema, Long parentId, String parentPath, String location, MediaType mediaType, String contentType) {
        List<RequestParam> params = new ArrayList<>();
        
        if (schema instanceof ObjectSchema) {
            ObjectSchema objectSchema = (ObjectSchema) schema;
            if (objectSchema.getProperties() != null) {
                for (Map.Entry<String, Schema> propEntry : objectSchema.getProperties().entrySet()) {
                    String propName = propEntry.getKey();
                    Schema propSchema = propEntry.getValue();
                    String currentPath = parentPath.isEmpty() ? propName : parentPath + "." + propName;
                    
                    RequestParam param = createRequestParamFromSchema(propName, propSchema, location, 
                                                                     currentPath, parentId);
                    // 设置contentType（如果是body类型）
                    if (contentType != null && "body".equals(location)) {
                        param.setContentType(contentType);
                    }
                    params.add(param);
                    
                    // 递归处理嵌套对象
                    if (propSchema instanceof ObjectSchema || propSchema instanceof ArraySchema) {
                        params.addAll(parseSchema(propSchema, null, currentPath, location, null, contentType));
                    }
                }
            }
        } else if (schema instanceof ArraySchema) {
            ArraySchema arraySchema = (ArraySchema) schema;
            if (arraySchema.getItems() != null) {
                String currentPath = parentPath.isEmpty() ? "items" : parentPath + "[0]";
                params.addAll(parseSchema(arraySchema.getItems(), null, currentPath, location, null, contentType));
            }
        } else {
            // 基本类型
            // 如果parentPath为空且location是body，说明已经在parseRequestBody中创建了body参数，不需要重复创建
            if (!(parentPath.isEmpty() && "body".equals(location))) {
                RequestParam param = createRequestParamFromSchema("body", schema, location, parentPath, parentId);
                // 设置contentType（如果是body类型）
                if (contentType != null && "body".equals(location)) {
                    param.setContentType(contentType);
                }
                // 如果MediaType有example且当前参数没有example，则使用MediaType的example
                if (mediaType != null && mediaType.getExample() != null && param.getExample() == null) {
                    param.setExample(String.valueOf(mediaType.getExample()));
                }
                params.add(param);
            }
        }
        
        return params;
    }

    /**
     * 从Schema创建RequestParam
     */
    private RequestParam createRequestParamFromSchema(String name, Schema schema, String location, 
                                                      String hierarchyPath, Long parentId) {
        RequestParam param = new RequestParam();
        param.setParamName(name);
        param.setLocation(location);
        param.setParamType(getSchemaType(schema));
        param.setHierarchyPath(hierarchyPath);
        param.setParentId(parentId);
        param.setExample(getExampleFromSchema(schema));
        param.setPattern(getPatternFromSchema(schema));
        param.setDescription(schema.getDescription());
        
        if (param.getPattern() != null) {
            param.setPatternExample(RegexExampleGenerator.generateExample(param.getPattern()));
        }
        
        // 检查是否必填（需要从父Schema获取required列表）
        if (schema instanceof StringSchema) {
            StringSchema stringSchema = (StringSchema) schema;
            param.setRequired(stringSchema.getRequired() != null && !stringSchema.getRequired().isEmpty());
        }
        
        return param;
    }

    /**
     * 解析ApiResponse（只解析body部分，header部分在parseOperation中单独处理）
     */
    private List<ResponseParam> parseApiResponse(String statusCode, ApiResponse apiResponse, 
                                                  OpenAPI openAPI, String parentPath) {
        List<ResponseParam> params = new ArrayList<>();
        
        if (apiResponse.getContent() != null) {
            for (Map.Entry<String, MediaType> contentEntry : apiResponse.getContent().entrySet()) {
                String contentType = contentEntry.getKey(); // 如 application/json
                MediaType mediaType = contentEntry.getValue();
                if (mediaType.getSchema() != null) {
                    // 生成完整的JSON示例（传入OpenAPI以支持$ref引用解析）
                    String fullJsonExample = com.simulator.util.JsonExampleGenerator.generateJsonExample(
                        mediaType.getSchema(), openAPI);
                    
                    // 创建response body参数，包含完整JSON示例
                    ResponseParam bodyParam = new ResponseParam();
                    bodyParam.setStatusCode(statusCode);
                    bodyParam.setLocation("body");
                    bodyParam.setParamName("body");
                    bodyParam.setParamType(getSchemaType(mediaType.getSchema()));
                    bodyParam.setHierarchyPath(parentPath);
                    bodyParam.setDescription(apiResponse.getDescription());
                    
                    // 总是将生成的完整JSON示例保存到example字段，供UI使用
                    if (fullJsonExample != null && !fullJsonExample.trim().isEmpty()) {
                        // 将生成的完整JSON示例保存到example字段，供UI使用
                        bodyParam.setExample(fullJsonExample);
                        log.debug("保存response body完整JSON示例到example字段，长度: {}", fullJsonExample.length());
                    } else {
                        // 如果生成失败，尝试使用MediaType的example
                        Object mediaExample = null;
                        if (mediaType.getExample() != null) {
                            mediaExample = mediaType.getExample();
                        } else if (mediaType.getExamples() != null && !mediaType.getExamples().isEmpty()) {
                            // 如果有examples，取第一个example的value
                            io.swagger.v3.oas.models.examples.Example firstExample = 
                                mediaType.getExamples().values().iterator().next();
                            if (firstExample != null && firstExample.getValue() != null) {
                                mediaExample = firstExample.getValue();
                            }
                        }
                        if (mediaExample != null) {
                            bodyParam.setExample(String.valueOf(mediaExample));
                        }
                        log.warn("生成response body完整JSON示例失败，使用MediaType的example作为备选");
                    }
                    
                    params.add(bodyParam);
                    
                    // MediaType的example和examples都需要处理
                    // 优先使用example，如果没有则尝试从examples中获取第一个
                    Object mediaExample = null;
                    if (mediaType.getExample() != null) {
                        mediaExample = mediaType.getExample();
                    } else if (mediaType.getExamples() != null && !mediaType.getExamples().isEmpty()) {
                        // 如果有examples，取第一个example的value
                        io.swagger.v3.oas.models.examples.Example firstExample = 
                            mediaType.getExamples().values().iterator().next();
                        if (firstExample != null && firstExample.getValue() != null) {
                            mediaExample = firstExample.getValue();
                        }
                    }
                    
                    // 创建一个临时的MediaType来传递example
                    MediaType tempMediaType = null;
                    if (mediaExample != null) {
                        tempMediaType = new MediaType();
                        tempMediaType.setExample(mediaExample);
                    }
                    
                    // 继续扁平化解析，用于参数填充
                    params.addAll(parseResponseSchema(mediaType.getSchema(), statusCode, parentPath, null, tempMediaType));
                }
            }
        }
        
        // 如果response没有content，至少创建一个基本的ResponseParam记录（确保所有状态码都能保存）
        if (params.isEmpty()) {
            ResponseParam basicParam = new ResponseParam();
            basicParam.setStatusCode(statusCode);
            basicParam.setLocation("body");
            basicParam.setParamName("body");
            basicParam.setParamType("String");
            basicParam.setDescription(apiResponse.getDescription());
            basicParam.setExample(null); // 没有content时example为null
            params.add(basicParam);
            log.debug("为状态码 {} 创建基本的ResponseParam记录（无content，description: {}）", 
                statusCode, apiResponse.getDescription());
        }
        
        return params;
    }

    /**
     * 解析响应Schema（递归处理嵌套结构）
     */
    private List<ResponseParam> parseResponseSchema(Schema schema, String statusCode, 
                                                    String parentPath, Long parentId) {
        return parseResponseSchema(schema, statusCode, parentPath, parentId, null);
    }

    /**
     * 解析响应Schema（递归处理嵌套结构），支持MediaType的example
     */
    private List<ResponseParam> parseResponseSchema(Schema schema, String statusCode, 
                                                    String parentPath, Long parentId, MediaType mediaType) {
        List<ResponseParam> params = new ArrayList<>();
        
        if (schema instanceof ObjectSchema) {
            ObjectSchema objectSchema = (ObjectSchema) schema;
            if (objectSchema.getProperties() != null) {
                for (Map.Entry<String, Schema> propEntry : objectSchema.getProperties().entrySet()) {
                    String propName = propEntry.getKey();
                    Schema propSchema = propEntry.getValue();
                    String currentPath = parentPath.isEmpty() ? propName : parentPath + "." + propName;
                    
                    ResponseParam param = createResponseParamFromSchema(propName, propSchema, statusCode, 
                                                                       currentPath, parentId);
                    // 如果MediaType有example且当前参数没有example，则使用MediaType的example
                    if (mediaType != null && mediaType.getExample() != null && param.getExample() == null) {
                        param.setExample(String.valueOf(mediaType.getExample()));
                    }
                    params.add(param);
                    
                    // 递归处理嵌套对象和数组，继续传递mediaType
                    if (propSchema instanceof ObjectSchema || propSchema instanceof ArraySchema) {
                        params.addAll(parseResponseSchema(propSchema, statusCode, currentPath, null, mediaType));
                    }
                }
            }
        } else if (schema instanceof ArraySchema) {
            ArraySchema arraySchema = (ArraySchema) schema;
            if (arraySchema.getItems() != null) {
                String currentPath = parentPath.isEmpty() ? "items" : parentPath + "[0]";
                params.addAll(parseResponseSchema(arraySchema.getItems(), statusCode, currentPath, null, mediaType));
            }
        } else {
            // 基本类型
            // 如果parentPath为空，说明已经在parseApiResponse中创建了body参数，不需要重复创建
            if (!parentPath.isEmpty()) {
                ResponseParam param = createResponseParamFromSchema("body", schema, statusCode, parentPath, parentId);
                // 如果MediaType有example且当前参数没有example，则使用MediaType的example
                if (mediaType != null && mediaType.getExample() != null && param.getExample() == null) {
                    param.setExample(String.valueOf(mediaType.getExample()));
                }
                params.add(param);
            }
        }
        
        return params;
    }

    /**
     * 从Schema创建ResponseParam
     */
    private ResponseParam createResponseParamFromSchema(String name, Schema schema, String statusCode, 
                                                       String hierarchyPath, Long parentId) {
        ResponseParam param = new ResponseParam();
        param.setParamName(name);
        param.setStatusCode(statusCode);
        param.setLocation("body"); // 默认是body
        param.setParamType(getSchemaType(schema));
        param.setHierarchyPath(hierarchyPath);
        param.setParentId(parentId);
        param.setDescription(schema.getDescription());
        
        // 提取正则表达式
        String pattern = getPatternFromSchema(schema);
        if (pattern != null) {
            param.setPattern(pattern);
        }
        
        // 提取example
        param.setExample(getExampleFromSchema(schema));
        
        // 如果仍然没有example但有pattern，根据pattern生成example
        if (param.getExample() == null && param.getPattern() != null) {
            String generatedExample = RegexExampleGenerator.generateExample(param.getPattern());
            if (generatedExample != null) {
                param.setExample(generatedExample);
            }
        }
        
        return param;
    }

    /**
     * 获取Schema类型
     */
    private String getSchemaType(Schema schema) {
        if (schema == null) {
            return "String";
        }
        
        if (schema instanceof StringSchema) {
            return "String";
        } else if (schema instanceof IntegerSchema) {
            return "Integer";
        } else if (schema instanceof NumberSchema) {
            return "Number";
        } else if (schema instanceof BooleanSchema) {
            return "Boolean";
        } else if (schema instanceof ArraySchema) {
            ArraySchema arraySchema = (ArraySchema) schema;
            if (arraySchema.getItems() != null) {
                return "Array<" + getSchemaType(arraySchema.getItems()) + ">";
            }
            return "Array";
        } else if (schema instanceof ObjectSchema) {
            return "Object";
        } else if (schema instanceof DateSchema) {
            return "Date";
        } else if (schema instanceof DateTimeSchema) {
            return "DateTime";
        } else if (schema.getType() != null) {
            return schema.getType();
        }
        
        return "String";
    }

    /**
     * 从Schema获取示例值
     */
    private String getExampleFromSchema(Schema schema) {
        if (schema == null) {
            return null;
        }
        
        if (schema.getExample() != null) {
            return String.valueOf(schema.getExample());
        }
        
        // 根据类型生成默认示例
        String type = getSchemaType(schema);
        switch (type) {
            case "Integer":
                return "1";
            case "Number":
                return "1.0";
            case "Boolean":
                return "true";
            case "Date":
                return "2024-01-01";
            case "DateTime":
                return "2024-01-01T00:00:00Z";
            default:
                return "example";
        }
    }

    /**
     * 从Schema获取正则表达式
     */
    private String getPatternFromSchema(Schema schema) {
        if (schema instanceof StringSchema) {
            StringSchema stringSchema = (StringSchema) schema;
            return stringSchema.getPattern();
        }
        return null;
    }

    /**
     * 解析Swagger 2.0 PathItem（包含多个HTTP方法）
     */
    private void parseSwagger2PathItem(String path, Path pathItem, io.swagger.models.Swagger swagger, 
                                       List<ApiInfo> apiInfos) {
        // GET方法
        if (pathItem.getGet() != null) {
            apiInfos.add(parseSwagger2Operation(path, "GET", pathItem.getGet(), swagger));
        }
        
        // POST方法
        if (pathItem.getPost() != null) {
            apiInfos.add(parseSwagger2Operation(path, "POST", pathItem.getPost(), swagger));
        }
        
        // PUT方法
        if (pathItem.getPut() != null) {
            apiInfos.add(parseSwagger2Operation(path, "PUT", pathItem.getPut(), swagger));
        }
        
        // DELETE方法
        if (pathItem.getDelete() != null) {
            apiInfos.add(parseSwagger2Operation(path, "DELETE", pathItem.getDelete(), swagger));
        }
        
        // PATCH方法
        if (pathItem.getPatch() != null) {
            apiInfos.add(parseSwagger2Operation(path, "PATCH", pathItem.getPatch(), swagger));
        }
    }

    /**
     * 解析Swagger 2.0单个Operation
     */
    private ApiInfo parseSwagger2Operation(String path, String method, io.swagger.models.Operation operation, 
                                           io.swagger.models.Swagger swagger) {
        ApiInfo apiInfo = new ApiInfo();
        apiInfo.setPath(path);
        apiInfo.setMethod(method);
        apiInfo.setDescription(operation.getDescription());
        apiInfo.setOperationId(operation.getOperationId());
        
        // 解析标签
        if (operation.getTags() != null && !operation.getTags().isEmpty()) {
            apiInfo.setTags(String.join(",", operation.getTags()));
        }
        
        // 解析请求参数
        List<RequestParam> requestParams = new ArrayList<>();
        if (operation.getParameters() != null) {
            for (io.swagger.models.parameters.Parameter parameter : operation.getParameters()) {
                requestParams.addAll(parseSwagger2Parameter(parameter, null, "", swagger));
            }
        }
        
        apiInfo.setRequestParams(requestParams);
        
        // 解析响应
        List<ResponseParam> responseParams = new ArrayList<>();
        if (operation.getResponses() != null) {
            for (Map.Entry<String, Response> responseEntry : operation.getResponses().entrySet()) {
                String statusCode = responseEntry.getKey();
                Response response = responseEntry.getValue();
                responseParams.addAll(parseSwagger2Response(statusCode, response, swagger, ""));
            }
        }
        apiInfo.setResponseParams(responseParams);
        
        return apiInfo;
    }

    /**
     * 解析Swagger 2.0 Parameter（路径参数、查询参数等）
     */
    private List<RequestParam> parseSwagger2Parameter(io.swagger.models.parameters.Parameter parameter, 
                                                      Long parentId, String parentPath, 
                                                      io.swagger.models.Swagger swagger) {
        List<RequestParam> params = new ArrayList<>();
        
        RequestParam requestParam = new RequestParam();
        requestParam.setParamName(parameter.getName());
        requestParam.setLocation(parameter.getIn());
        Boolean required = parameter.getRequired();
        requestParam.setRequired(required != null && required);
        requestParam.setDescription(parameter.getDescription());
        requestParam.setParentId(parentId);
        
        String currentPath = parentPath.isEmpty() ? parameter.getName() : parentPath + "." + parameter.getName();
        requestParam.setHierarchyPath(currentPath);
        
        // 处理不同类型的参数
        if (parameter instanceof BodyParameter) {
            BodyParameter bodyParam = (BodyParameter) parameter;
            Model schema = bodyParam.getSchema();
            if (schema != null) {
                // 生成完整的JSON示例
                String fullJsonExample = generateSwagger2JsonExample(schema, swagger);
                requestParam.setLocation("body");
                requestParam.setParamType(getSwagger2ModelType(schema));
                requestParam.setFullJsonExample(fullJsonExample);
                
                // 总是将生成的完整JSON示例保存到example字段，供UI使用（与OpenAPI 3.0逻辑一致）
                if (fullJsonExample != null && !fullJsonExample.trim().isEmpty()) {
                    requestParam.setExample(fullJsonExample);
                    log.debug("保存Swagger 2.0 request body完整JSON示例到example字段，长度: {}", fullJsonExample.length());
                } else {
                    // 如果生成失败，尝试使用其他备选方案
                    log.warn("生成Swagger 2.0 request body完整JSON示例失败，使用备选方案");
                    // 可以尝试从schema的example获取，但Swagger 2.0的Model可能没有直接的example字段
                    // 这里保持原有逻辑，使用getSwagger2PropertyExample作为备选
                }
                
                // 扁平化解析schema（处理RefModel和ModelImpl）
                if (schema instanceof RefModel) {
                    // 处理$ref引用
                    RefModel refModel = (RefModel) schema;
                    String ref = refModel.getSimpleRef();
                    log.debug("BodyParameter使用RefModel引用: {}", ref);
                    if (swagger.getDefinitions() != null) {
                        Model refModelDef = swagger.getDefinitions().get(ref);
                        if (refModelDef instanceof ModelImpl) {
                            params.addAll(parseSwagger2Model((ModelImpl) refModelDef, null, "", "body", swagger));
                        }
                    }
                } else if (schema instanceof ModelImpl) {
                    params.addAll(parseSwagger2Model((ModelImpl) schema, null, "", "body", swagger));
                }
            }
        } else if (parameter instanceof AbstractSerializableParameter) {
            AbstractSerializableParameter<?> serializableParam = (AbstractSerializableParameter<?>) parameter;
            Property property = serializableParam.getItems();
            
            // 根据property或parameter的类型来设置paramType
            if (property != null) {
                requestParam.setParamType(getSwagger2PropertyType(property));
            } else {
                // 如果property为null，根据parameter的类型判断
                String type = serializableParam.getType();
                if (type != null) {
                    requestParam.setParamType(type.substring(0, 1).toUpperCase() + type.substring(1));
                } else {
                    requestParam.setParamType("String");
                }
            }
            
            // 处理example
            if (serializableParam.getExample() != null) {
                requestParam.setExample(String.valueOf(serializableParam.getExample()));
            } else if (property != null) {
                requestParam.setExample(getSwagger2PropertyExample(property));
            }
            
            // 处理pattern
            if (property instanceof StringProperty) {
                StringProperty stringProp = (StringProperty) property;
                if (stringProp.getPattern() != null) {
                    requestParam.setPattern(stringProp.getPattern());
                    requestParam.setPatternExample(RegexExampleGenerator.generateExample(stringProp.getPattern()));
                }
            }
        }
        
        params.add(requestParam);
        return params;
    }

    /**
     * 解析Swagger 2.0 Response
     */
    private List<ResponseParam> parseSwagger2Response(String statusCode, Response response, 
                                                      io.swagger.models.Swagger swagger, String parentPath) {
        List<ResponseParam> params = new ArrayList<>();
        
        // 解析response body
        if (response.getSchema() != null) {
            Property schema = response.getSchema();
            
            // 生成完整的JSON示例
            String fullJsonExample = Swagger2JsonExampleGenerator.generateJsonExample(schema, swagger);
            
            ResponseParam bodyParam = new ResponseParam();
            bodyParam.setStatusCode(statusCode);
            bodyParam.setLocation("body");
            bodyParam.setParamName("body");
            bodyParam.setParamType(getSwagger2PropertyType(schema));
            bodyParam.setHierarchyPath(parentPath);
            bodyParam.setDescription(response.getDescription());
            
            // 总是将生成的完整JSON示例保存到example字段，供UI使用（与OpenAPI 3.0逻辑一致）
            if (fullJsonExample != null && !fullJsonExample.trim().isEmpty()) {
                bodyParam.setExample(fullJsonExample);
                log.debug("保存Swagger 2.0 response body完整JSON示例到example字段，长度: {}", fullJsonExample.length());
            } else {
                // 如果生成失败，尝试使用Response的examples字段作为备选（与OpenAPI 3.0逻辑一致）
                Object responseExample = null;
                if (response.getExamples() != null && !response.getExamples().isEmpty()) {
                    // 从examples Map中获取第一个example值
                    responseExample = response.getExamples().values().iterator().next();
                }
                if (responseExample != null) {
                    bodyParam.setExample(String.valueOf(responseExample));
                    log.debug("使用Swagger 2.0 Response的examples字段作为备选");
                } else {
                    // 最后使用getSwagger2PropertyExample作为备选
                    bodyParam.setExample(getSwagger2PropertyExample(schema));
                    log.warn("生成Swagger 2.0 response body完整JSON示例失败，使用getSwagger2PropertyExample作为备选");
                }
            }
            
            params.add(bodyParam);
            
            // 扁平化解析schema
            if (schema instanceof ObjectProperty) {
                params.addAll(parseSwagger2ObjectProperty((ObjectProperty) schema, statusCode, "", null, swagger));
            } else if (schema instanceof ArrayProperty) {
                ArrayProperty arrayProp = (ArrayProperty) schema;
                if (arrayProp.getItems() instanceof ObjectProperty) {
                    params.addAll(parseSwagger2ObjectProperty((ObjectProperty) arrayProp.getItems(), 
                                                              statusCode, "[0]", null, swagger));
                }
            } else if (schema instanceof RefProperty) {
                // 处理$ref引用
                RefProperty refProp = (RefProperty) schema;
                String ref = refProp.getSimpleRef();
                if (swagger.getDefinitions() != null) {
                    Model model = swagger.getDefinitions().get(ref);
                    if (model instanceof ModelImpl) {
                        // 注意：parseSwagger2Model用于RequestParam，这里应该使用parseSwagger2ResponseModel
                        params.addAll(parseSwagger2ResponseModel((ModelImpl) model, statusCode, "", null, swagger));
                    }
                }
            }
        }
        
        // 解析response headers
        if (response.getHeaders() != null) {
            for (Map.Entry<String, Property> headerEntry : response.getHeaders().entrySet()) {
                ResponseParam headerParam = new ResponseParam();
                headerParam.setStatusCode(statusCode);
                headerParam.setLocation("header");
                headerParam.setParamName(headerEntry.getKey());
                headerParam.setParamType(getSwagger2PropertyType(headerEntry.getValue()));
                headerParam.setExample(getSwagger2PropertyExample(headerEntry.getValue()));
                
                if (headerEntry.getValue() instanceof StringProperty) {
                    StringProperty stringProp = (StringProperty) headerEntry.getValue();
                    if (stringProp.getPattern() != null) {
                        headerParam.setPattern(stringProp.getPattern());
                        headerParam.setExample(RegexExampleGenerator.generateExample(stringProp.getPattern()));
                    }
                }
                
                params.add(headerParam);
            }
        }
        
        // 如果response没有schema也没有headers，至少创建一个基本的ResponseParam记录（确保所有状态码都能保存）
        if (params.isEmpty()) {
            ResponseParam basicParam = new ResponseParam();
            basicParam.setStatusCode(statusCode);
            basicParam.setLocation("body");
            basicParam.setParamName("body");
            basicParam.setParamType("String");
            basicParam.setDescription(response.getDescription());
            basicParam.setExample(null); // 没有schema时example为null
            params.add(basicParam);
            log.debug("为状态码 {} 创建基本的ResponseParam记录（无schema，description: {}）", 
                statusCode, response.getDescription());
        }
        
        return params;
    }

    /**
     * 解析Swagger 2.0 Model
     */
    private List<RequestParam> parseSwagger2Model(ModelImpl model, Long parentId, String parentPath, 
                                                  String location, io.swagger.models.Swagger swagger) {
        List<RequestParam> params = new ArrayList<>();
        
        if (model.getProperties() != null) {
            for (Map.Entry<String, Property> propEntry : model.getProperties().entrySet()) {
                String propName = propEntry.getKey();
                Property property = propEntry.getValue();
                String currentPath = parentPath.isEmpty() ? propName : parentPath + "." + propName;
                
                RequestParam param = new RequestParam();
                param.setParamName(propName);
                param.setLocation(location);
                param.setParamType(getSwagger2PropertyType(property));
                param.setHierarchyPath(currentPath);
                param.setParentId(parentId);
                param.setDescription(property.getDescription());
                param.setExample(getSwagger2PropertyExample(property));
                
                if (property instanceof StringProperty) {
                    StringProperty stringProp = (StringProperty) property;
                    if (stringProp.getPattern() != null) {
                        param.setPattern(stringProp.getPattern());
                        param.setPatternExample(RegexExampleGenerator.generateExample(stringProp.getPattern()));
                    }
                }
                
                params.add(param);
                
                // 递归处理嵌套对象 - 对于ObjectProperty，需要递归处理其properties
                if (property instanceof ObjectProperty) {
                    ObjectProperty objectProp = (ObjectProperty) property;
                    if (objectProp.getProperties() != null) {
                        for (Map.Entry<String, Property> nestedEntry : objectProp.getProperties().entrySet()) {
                            String nestedName = nestedEntry.getKey();
                            Property nestedProperty = nestedEntry.getValue();
                            String nestedPath = currentPath + "." + nestedName;
                            
                            RequestParam nestedParam = new RequestParam();
                            nestedParam.setParamName(nestedName);
                            nestedParam.setLocation(location);
                            nestedParam.setParamType(getSwagger2PropertyType(nestedProperty));
                            nestedParam.setHierarchyPath(nestedPath);
                            nestedParam.setParentId(null);
                            nestedParam.setDescription(nestedProperty.getDescription());
                            nestedParam.setExample(getSwagger2PropertyExample(nestedProperty));
                            
                            params.add(nestedParam);
                            
                            // 如果嵌套属性也是对象或引用，继续递归（通过parseSwagger2Model）
                            if (nestedProperty instanceof RefProperty) {
                                RefProperty nestedRefProp = (RefProperty) nestedProperty;
                                String nestedRef = nestedRefProp.getSimpleRef();
                                if (swagger.getDefinitions() != null) {
                                    Model nestedModel = swagger.getDefinitions().get(nestedRef);
                                    if (nestedModel instanceof ModelImpl) {
                                        params.addAll(parseSwagger2Model((ModelImpl) nestedModel, null, nestedPath, location, swagger));
                                    }
                                }
                            }
                        }
                    }
                } else if (property instanceof ArrayProperty) {
                    ArrayProperty arrayProp = (ArrayProperty) property;
                    if (arrayProp.getItems() instanceof ObjectProperty) {
                        // 对于数组中的对象，需要处理其属性
                        ObjectProperty itemsObjectProp = (ObjectProperty) arrayProp.getItems();
                        if (itemsObjectProp.getProperties() != null) {
                            for (Map.Entry<String, Property> nestedEntry : itemsObjectProp.getProperties().entrySet()) {
                                String nestedName = nestedEntry.getKey();
                                Property nestedProperty = nestedEntry.getValue();
                                String nestedPath = currentPath + "[0]." + nestedName;
                                
                                RequestParam nestedParam = new RequestParam();
                                nestedParam.setParamName(nestedName);
                                nestedParam.setLocation(location);
                                nestedParam.setParamType(getSwagger2PropertyType(nestedProperty));
                                nestedParam.setHierarchyPath(nestedPath);
                                nestedParam.setParentId(null);
                                nestedParam.setDescription(nestedProperty.getDescription());
                                nestedParam.setExample(getSwagger2PropertyExample(nestedProperty));
                                
                                params.add(nestedParam);
                            }
                        }
                    }
                } else if (property instanceof RefProperty) {
                    RefProperty refProp = (RefProperty) property;
                    String ref = refProp.getSimpleRef();
                    if (swagger.getDefinitions() != null) {
                        Model refModel = swagger.getDefinitions().get(ref);
                        if (refModel instanceof ModelImpl) {
                            params.addAll(parseSwagger2Model((ModelImpl) refModel, null, currentPath, location, swagger));
                        }
                    }
                }
            }
        }
        
        return params;
    }

    /**
     * 解析Swagger 2.0 ObjectProperty
     */
    private List<ResponseParam> parseSwagger2ObjectProperty(ObjectProperty objectProperty, String statusCode, 
                                                            String parentPath, Long parentId, 
                                                            io.swagger.models.Swagger swagger) {
        List<ResponseParam> params = new ArrayList<>();
        
        if (objectProperty.getProperties() != null) {
            for (Map.Entry<String, Property> propEntry : objectProperty.getProperties().entrySet()) {
                String propName = propEntry.getKey();
                Property property = propEntry.getValue();
                String currentPath = parentPath.isEmpty() ? propName : parentPath + "." + propName;
                
                ResponseParam param = new ResponseParam();
                param.setStatusCode(statusCode);
                param.setLocation("body");
                param.setParamName(propName);
                param.setParamType(getSwagger2PropertyType(property));
                param.setHierarchyPath(currentPath);
                param.setParentId(parentId);
                param.setDescription(property.getDescription());
                param.setExample(getSwagger2PropertyExample(property));
                
                if (property instanceof StringProperty) {
                    StringProperty stringProp = (StringProperty) property;
                    if (stringProp.getPattern() != null) {
                        param.setPattern(stringProp.getPattern());
                        param.setExample(RegexExampleGenerator.generateExample(stringProp.getPattern()));
                    }
                }
                
                params.add(param);
                
                // 递归处理嵌套对象
                if (property instanceof ObjectProperty) {
                    params.addAll(parseSwagger2ObjectProperty((ObjectProperty) property, statusCode, currentPath, null, swagger));
                } else if (property instanceof ArrayProperty) {
                    ArrayProperty arrayProp = (ArrayProperty) property;
                    if (arrayProp.getItems() instanceof ObjectProperty) {
                        params.addAll(parseSwagger2ObjectProperty((ObjectProperty) arrayProp.getItems(), 
                                                                  statusCode, currentPath + "[0]", null, swagger));
                    }
                } else if (property instanceof RefProperty) {
                    RefProperty refProp = (RefProperty) property;
                    String ref = refProp.getSimpleRef();
                    if (swagger.getDefinitions() != null) {
                        Model model = swagger.getDefinitions().get(ref);
                        if (model instanceof ModelImpl) {
                            params.addAll(parseSwagger2ResponseModel((ModelImpl) model, statusCode, currentPath, null, swagger));
                        }
                    }
                }
            }
        }
        
        return params;
    }

    /**
     * 解析Swagger 2.0 Response Model
     */
    private List<ResponseParam> parseSwagger2ResponseModel(ModelImpl model, String statusCode, 
                                                           String parentPath, Long parentId, 
                                                           io.swagger.models.Swagger swagger) {
        List<ResponseParam> params = new ArrayList<>();
        
        if (model.getProperties() != null) {
            for (Map.Entry<String, Property> propEntry : model.getProperties().entrySet()) {
                String propName = propEntry.getKey();
                Property property = propEntry.getValue();
                String currentPath = parentPath.isEmpty() ? propName : parentPath + "." + propName;
                
                ResponseParam param = new ResponseParam();
                param.setStatusCode(statusCode);
                param.setLocation("body");
                param.setParamName(propName);
                param.setParamType(getSwagger2PropertyType(property));
                param.setHierarchyPath(currentPath);
                param.setParentId(parentId);
                param.setDescription(property.getDescription());
                param.setExample(getSwagger2PropertyExample(property));
                
                if (property instanceof StringProperty) {
                    StringProperty stringProp = (StringProperty) property;
                    if (stringProp.getPattern() != null) {
                        param.setPattern(stringProp.getPattern());
                        param.setExample(RegexExampleGenerator.generateExample(stringProp.getPattern()));
                    }
                }
                
                params.add(param);
                
                // 递归处理嵌套对象
                if (property instanceof ObjectProperty) {
                    params.addAll(parseSwagger2ObjectProperty((ObjectProperty) property, statusCode, currentPath, null, swagger));
                } else if (property instanceof ArrayProperty) {
                    ArrayProperty arrayProp = (ArrayProperty) property;
                    if (arrayProp.getItems() instanceof ObjectProperty) {
                        params.addAll(parseSwagger2ObjectProperty((ObjectProperty) arrayProp.getItems(), 
                                                                  statusCode, currentPath + "[0]", null, swagger));
                    }
                } else if (property instanceof RefProperty) {
                    RefProperty refProp = (RefProperty) property;
                    String ref = refProp.getSimpleRef();
                    if (swagger.getDefinitions() != null) {
                        Model refModel = swagger.getDefinitions().get(ref);
                        if (refModel instanceof ModelImpl) {
                            params.addAll(parseSwagger2ResponseModel((ModelImpl) refModel, statusCode, currentPath, null, swagger));
                        }
                    }
                }
            }
        }
        
        return params;
    }

    /**
     * 获取Swagger 2.0 Property类型
     */
    private String getSwagger2PropertyType(Property property) {
        if (property == null) {
            return "String";
        }
        
        if (property instanceof StringProperty) {
            return "String";
        } else if (property instanceof IntegerProperty) {
            return "Integer";
        } else if (property instanceof LongProperty) {
            return "Long";
        } else if (property instanceof FloatProperty) {
            return "Float";
        } else if (property instanceof DoubleProperty) {
            return "Double";
        } else if (property instanceof BooleanProperty) {
            return "Boolean";
        } else if (property instanceof ArrayProperty) {
            ArrayProperty arrayProp = (ArrayProperty) property;
            if (arrayProp.getItems() != null) {
                return "Array<" + getSwagger2PropertyType(arrayProp.getItems()) + ">";
            }
            return "Array";
        } else if (property instanceof ObjectProperty) {
            return "Object";
        } else if (property instanceof RefProperty) {
            return "Object";
        } else if (property instanceof DateProperty) {
            return "Date";
        } else if (property instanceof DateTimeProperty) {
            return "DateTime";
        }
        
        return property.getType();
    }

    /**
     * 获取Swagger 2.0 Model类型
     */
    private String getSwagger2ModelType(Model model) {
        if (model instanceof ModelImpl) {
            ModelImpl modelImpl = (ModelImpl) model;
            if (modelImpl.getType() != null) {
                return modelImpl.getType();
            }
        }
        return "Object";
    }

    /**
     * 获取Swagger 2.0 Property示例值
     */
    private String getSwagger2PropertyExample(Property property) {
        if (property == null) {
            return null;
        }
        
        // 根据类型生成默认示例
        if (property instanceof StringProperty) {
            StringProperty stringProp = (StringProperty) property;
            if (stringProp.getEnum() != null && !stringProp.getEnum().isEmpty()) {
                return stringProp.getEnum().get(0);
            }
            return "string";
        } else if (property instanceof IntegerProperty) {
            return "1";
        } else if (property instanceof LongProperty) {
            return "1";
        } else if (property instanceof FloatProperty) {
            return "1.0";
        } else if (property instanceof DoubleProperty) {
            return "1.0";
        } else if (property instanceof BooleanProperty) {
            return "true";
        } else if (property instanceof DateProperty) {
            return "2024-01-01";
        } else if (property instanceof DateTimeProperty) {
            return "2024-01-01T00:00:00Z";
        }
        
        return "example";
    }

    /**
     * 生成Swagger 2.0 Model的JSON示例
     */
    private String generateSwagger2JsonExample(Model model, io.swagger.models.Swagger swagger) {
        return Swagger2JsonExampleGenerator.generateJsonExampleFromModel(model, swagger);
    }

    /**
     * 解析结果类
     */
    public static class ParseResult {
        private SwaggerInfo swaggerInfo;
        private List<ServerInfo> servers;
        private List<ApiInfo> apiInfos;

        public SwaggerInfo getSwaggerInfo() {
            return swaggerInfo;
        }

        public void setSwaggerInfo(SwaggerInfo swaggerInfo) {
            this.swaggerInfo = swaggerInfo;
        }

        public List<ServerInfo> getServers() {
            return servers;
        }

        public void setServers(List<ServerInfo> servers) {
            this.servers = servers;
        }

        public List<ApiInfo> getApiInfos() {
            return apiInfos;
        }

        public void setApiInfos(List<ApiInfo> apiInfos) {
            this.apiInfos = apiInfos;
        }
    }
}

