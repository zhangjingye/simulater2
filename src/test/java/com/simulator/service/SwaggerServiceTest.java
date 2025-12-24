package com.simulator.service;

import com.simulator.dto.SwaggerImportRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Swagger服务测试类
 * 
 * @author simulator
 * @date 2024
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class SwaggerServiceTest {

    @Autowired
    private SwaggerService swaggerService;

    /**
     * 测试导入Swagger文档
     */
    @Test
    public void testImportSwagger() {
        // 示例Swagger文档（OpenAPI3格式）
        String swaggerContent = """
            {
              "openapi": "3.0.0",
              "info": {
                "title": "测试API",
                "version": "1.0.0"
              },
              "servers": [
                {
                  "url": "http://localhost:8080"
                }
              ],
              "paths": {
                "/api/users": {
                  "get": {
                    "summary": "获取用户列表",
                    "operationId": "getUsers",
                    "parameters": [
                      {
                        "name": "page",
                        "in": "query",
                        "schema": {
                          "type": "integer",
                          "example": 1
                        }
                      }
                    ],
                    "responses": {
                      "200": {
                        "description": "成功",
                        "content": {
                          "application/json": {
                            "schema": {
                              "type": "object",
                              "properties": {
                                "data": {
                                  "type": "array",
                                  "items": {
                                    "type": "object",
                                    "properties": {
                                      "id": {
                                        "type": "integer"
                                      },
                                      "name": {
                                        "type": "string"
                                      }
                                    }
                                  }
                                }
                              }
                            }
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
            """;

        SwaggerImportRequest request = new SwaggerImportRequest();
        request.setContent(swaggerContent);
        request.setContentType("json");

        int count = swaggerService.importSwagger(request);
        assertTrue(count > 0, "应该成功导入至少一个接口");
    }
}

