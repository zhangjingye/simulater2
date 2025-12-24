package com.simulator.parser;

import com.simulator.entity.*;
import com.simulator.util.RegexExampleGenerator;
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
            // 使用swagger-parser解析
            OpenAPIV3Parser parser = new OpenAPIV3Parser();
            ParseOptions options = new ParseOptions();
            options.setResolve(true);
            options.setFlatten(true);
            
            SwaggerParseResult parseResult = parser.readContents(content, null, options);
            OpenAPI openAPI = parseResult.getOpenAPI();
            
            if (openAPI == null) {
                throw new RuntimeException("无法解析Swagger文档: " + parseResult.getMessages());
            }
            
            // 判断版本
            String version = determineVersion(openAPI);
            
            // 解析Swagger文档信息
            SwaggerInfo swaggerInfo = parseSwaggerInfo(openAPI, version, content);
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
                    parsePathItem(path, pathItem, openAPI, apiInfos, version);
                }
            }
            
            result.setApiInfos(apiInfos);
            return result;
            
        } catch (Exception e) {
            log.error("解析Swagger文档失败", e);
            throw new RuntimeException("解析Swagger文档失败: " + e.getMessage(), e);
        }
    }

    /**
     * 判断Swagger版本
     */
    private String determineVersion(OpenAPI openAPI) {
        if (openAPI.getOpenapi() != null && openAPI.getOpenapi().startsWith("3")) {
            return "v3";
        }
        return "v2";
    }

    /**
     * 解析Swagger文档信息
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
     * 解析服务器信息
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

