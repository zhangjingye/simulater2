# Swagger解析器项目

基于JDK17和SpringBoot 3.x开发的Swagger文档解析系统，支持解析Swagger2和OpenAPI3规范的接口文档，将结构化的接口数据扁平化处理后存储到关系型数据库，并提供RESTful API供前端UI获取数据用于参数填充。

## 项目特性

- ✅ 支持Swagger2（v2）和OpenAPI3（v3）格式
- ✅ 完整解析接口信息（路径、方法、服务器、描述等）
- ✅ 扁平化处理嵌套的请求/响应参数结构
- ✅ 自动生成正则表达式示例值
- ✅ 支持JSON和YAML格式的Swagger文档
- ✅ 支持通过URL或文件内容导入
- ✅ 提供完整的RESTful API接口
- ✅ 支持分页、排序、条件查询

## 技术栈

- **JDK**: 17
- **SpringBoot**: 3.2.0
- **数据库**: MySQL 8.0+
- **ORM**: Spring Data JPA
- **解析库**: swagger-parser-v3 2.1.16
- **工具类**: Hutool 5.8.23
- **构建工具**: Maven

## 项目结构

```
src/main/java/com/simulator/
├── SwaggerParserApplication.java    # 主启动类
├── controller/                      # 控制器层
│   └── SwaggerController.java
├── service/                         # 服务层
│   └── SwaggerService.java
├── repository/                      # 数据访问层
│   ├── ApiInfoRepository.java
│   ├── RequestParamRepository.java
│   ├── ResponseParamRepository.java
│   ├── HeaderInfoRepository.java
│   └── ServerInfoRepository.java
├── entity/                          # 实体类
│   ├── ApiInfo.java
│   ├── RequestParam.java
│   ├── ResponseParam.java
│   ├── HeaderInfo.java
│   └── ServerInfo.java
├── dto/                             # 数据传输对象
│   ├── SwaggerImportRequest.java
│   ├── ApiQueryRequest.java
│   ├── ApiResponse.java
│   ├── ApiDetailDTO.java
│   └── ...
├── parser/                          # 解析器
│   └── SwaggerParser.java
├── util/                            # 工具类
│   └── RegexExampleGenerator.java
└── exception/                       # 异常处理
    └── GlobalExceptionHandler.java
```

## 环境要求

- JDK 17+
- Maven 3.6+
- MySQL 8.0+
- IDE（推荐IntelliJ IDEA或Eclipse）

## 快速开始

### 1. 数据库配置

创建MySQL数据库并执行初始化脚本：

```sql
-- 执行 src/main/resources/db/schema.sql
```

或修改 `application.yml` 中的数据库连接信息：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/swagger_parser?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: your_password
```

### 2. 编译项目

```bash
mvn clean compile
```

### 3. 运行项目

```bash
mvn spring-boot:run
```

或使用IDE直接运行 `SwaggerParserApplication.java`

### 4. 验证运行

访问：http://localhost:8080/api/swagger/apis

## API接口文档

### 1. 导入Swagger文档

**接口地址**: `POST /api/swagger/import`

**请求体**:
```json
{
  "content": "{swagger文档的JSON或YAML内容}",
  "contentType": "json",
  "url": "可选，如果提供URL则从URL获取内容"
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "成功导入10个接口",
  "data": 10
}
```

### 2. 查询接口列表

**接口地址**: `GET /api/swagger/apis`

**查询参数**:
- `path`: 接口路径（模糊查询）
- `method`: 请求方法
- `tags`: 标签
- `page`: 页码（从0开始，默认0）
- `size`: 每页大小（默认10）
- `sortBy`: 排序字段（默认createTime）
- `sortDir`: 排序方向（ASC/DESC，默认DESC）

**响应示例**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "content": [
      {
        "id": 1,
        "path": "/api/users",
        "method": "GET",
        "serverUrl": "http://localhost:8080",
        "description": "获取用户列表",
        "tags": "用户管理"
      }
    ],
    "totalElements": 10,
    "totalPages": 1
  }
}
```

### 3. 获取接口详情

**接口地址**: `GET /api/swagger/apis/{apiId}`

**响应示例**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "apiInfo": {
      "id": 1,
      "path": "/api/users",
      "method": "GET",
      "description": "获取用户列表"
    },
    "requestParams": [
      {
        "paramName": "page",
        "location": "query",
        "paramType": "Integer",
        "required": false,
        "example": "1"
      }
    ],
    "responseParams": [
      {
        "statusCode": "200",
        "paramName": "data",
        "paramType": "Array",
        "hierarchyPath": "data"
      }
    ],
    "headerInfos": []
  }
}
```

### 4. 根据位置查询请求参数

**接口地址**: `GET /api/swagger/apis/{apiId}/request-params?location=query`

**查询参数**:
- `location`: 参数位置（path/header/query/form/body），可选，不传则返回所有

### 5. 根据类型查询请求参数

**接口地址**: `GET /api/swagger/apis/{apiId}/request-params/type?paramType=String`

### 6. 根据状态码查询响应参数

**接口地址**: `GET /api/swagger/apis/{apiId}/response-params?statusCode=200`

## 数据库表结构

### api_info（接口基础信息表）
- 存储接口的路径、方法、服务器、描述等基本信息

### request_param（请求参数表）
- 存储请求参数，支持扁平化存储嵌套结构
- 包含参数位置、类型、是否必填、正则表达式、示例值等

### response_param（响应参数表）
- 存储响应参数，按状态码分类
- 支持扁平化存储嵌套结构

### header_info（头部信息表）
- 存储请求头和响应头信息

### server_info（服务器信息表）
- 存储Swagger文档中的服务器信息

详细表结构请参考 `src/main/resources/db/schema.sql`

## 核心功能说明

### 1. Swagger解析

- 使用 `swagger-parser-v3` 库解析Swagger2和OpenAPI3文档
- 支持JSON和YAML格式
- 解析内容包括：
  - 服务器信息
  - 接口路径和方法
  - 请求参数（路径、查询、头部、表单、请求体）
  - 响应参数（按状态码分类）
  - 请求头和响应头

### 2. 数据扁平化

- 将嵌套的对象和数组结构扁平化为一维结构
- 使用 `hierarchy_path` 字段保存层级关系（如：`user.name`、`items[0].id`）
- 使用 `parent_id` 字段构建父子关系

### 3. 正则表达式处理

- 自动识别参数中的正则表达式
- 使用 `RegexExampleGenerator` 工具类生成符合正则的示例值
- 支持常见正则模式（数字、字母、邮箱、手机号等）

### 4. 数据校验

- 使用JPA注解进行数据校验
- 参数类型、必填属性等自动验证
- 全局异常处理，返回友好错误信息

## 测试

### 单元测试示例

创建测试类 `SwaggerServiceTest.java`:

```java
@SpringBootTest
class SwaggerServiceTest {
    
    @Autowired
    private SwaggerService swaggerService;
    
    @Test
    void testImportSwagger() {
        SwaggerImportRequest request = new SwaggerImportRequest();
        request.setContent("{...swagger json...}");
        request.setContentType("json");
        
        int count = swaggerService.importSwagger(request);
        assertTrue(count > 0);
    }
}
```

## 注意事项

1. **数据库字符集**: 确保MySQL使用utf8mb4字符集，支持存储emoji和特殊字符
2. **时区设置**: 数据库连接URL中已设置时区为Asia/Shanghai，可根据实际情况调整
3. **JPA自动建表**: 项目配置了 `ddl-auto: update`，首次运行会自动创建表结构
4. **Swagger文档格式**: 确保导入的Swagger文档格式正确，符合Swagger2或OpenAPI3规范

## 常见问题

### Q: 导入Swagger文档失败？
A: 检查Swagger文档格式是否正确，确保是有效的JSON或YAML格式。

### Q: 数据库连接失败？
A: 检查 `application.yml` 中的数据库配置，确保MySQL服务已启动，数据库已创建。

### Q: 正则表达式示例值生成失败？
A: 某些复杂的正则表达式可能无法自动生成示例值，此时 `patternExample` 字段为null，可以使用 `example` 字段的值。

## 开发规范

- 遵循阿里巴巴Java开发规范
- 代码包含详细注释
- 使用Lombok简化代码
- 统一异常处理
- RESTful API设计规范

## 许可证

MIT License

## 联系方式

如有问题或建议，请联系项目维护者。

