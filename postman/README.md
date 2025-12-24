# Postman Collection 使用说明

## 文件说明

- `Swagger Parser API.postman_collection.json` - Postman Collection文件，包含所有API接口

## 导入方法

### 方法一：直接导入文件

1. 打开Postman应用
2. 点击左上角的"Import"按钮
3. 选择"File"标签页
4. 选择 `Swagger Parser API.postman_collection.json` 文件
5. 点击"Import"完成导入

### 方法二：通过URL导入

1. 打开Postman应用
2. 点击左上角的"Import"按钮
3. 选择"Link"标签页
4. 输入文件路径（如果文件在Git仓库中）
5. 点击"Continue"完成导入

## 环境变量配置

Collection中已预定义以下环境变量：

- `baseUrl`: API基础URL，默认值为 `http://localhost:8080/api`
- `apiId`: 接口ID，默认值为 `1`

### 修改环境变量

1. 在Postman中点击右上角的"眼睛"图标
2. 选择"Edit"编辑环境变量
3. 修改`baseUrl`为你的实际服务器地址
4. 修改`apiId`为你要查询的接口ID

### 创建环境

1. 点击右上角的"环境"下拉框
2. 选择"Manage Environments"
3. 点击"Add"创建新环境
4. 添加以下变量：
   - `baseUrl`: `http://localhost:8080/api`（开发环境）
   - `baseUrl`: `http://your-server.com/api`（生产环境）

## 接口说明

### 1. 导入Swagger文档

#### 导入Swagger文档（JSON内容）
- **方法**: POST
- **URL**: `{{baseUrl}}/swagger/import`
- **说明**: 通过JSON内容导入Swagger文档

#### 导入Swagger文档（通过URL）
- **方法**: POST
- **URL**: `{{baseUrl}}/swagger/import`
- **说明**: 通过URL自动获取并导入Swagger文档
- **示例URL**: `https://petstore.swagger.io/v2/swagger.json`

#### 导入Swagger文档（YAML内容）
- **方法**: POST
- **URL**: `{{baseUrl}}/swagger/import`
- **说明**: 通过YAML内容导入Swagger文档

#### 导入Swagger文档（文件上传）
- **方法**: POST
- **URL**: `{{baseUrl}}/swagger/import/file`
- **Content-Type**: `multipart/form-data`
- **参数**: 
  - `file`: 文件（支持.json、.yaml、.yml格式）
- **说明**: 通过文件上传的方式导入Swagger文档，最大文件大小10MB
- **使用步骤**:
  1. 在Postman中选择Body标签
  2. 选择form-data类型
  3. 在Key列输入`file`，类型选择`File`
  4. 点击"Select Files"选择要上传的Swagger文件
  5. 发送请求

### 2. 查询接口

#### 查询接口列表（全部）
- **方法**: GET
- **URL**: `{{baseUrl}}/swagger/apis`
- **查询参数**:
  - `page`: 页码（从0开始，默认0）
  - `size`: 每页大小（默认10）
  - `sortBy`: 排序字段（默认createTime）
  - `sortDir`: 排序方向（ASC/DESC，默认DESC）

#### 查询接口列表（按路径）
- **方法**: GET
- **URL**: `{{baseUrl}}/swagger/apis?path=/api/users`
- **说明**: 根据路径模糊查询

#### 查询接口列表（按方法）
- **方法**: GET
- **URL**: `{{baseUrl}}/swagger/apis?method=GET`
- **说明**: 根据请求方法查询

#### 查询接口列表（按标签）
- **方法**: GET
- **URL**: `{{baseUrl}}/swagger/apis?tags=用户管理`
- **说明**: 根据标签查询

#### 获取接口详情
- **方法**: GET
- **URL**: `{{baseUrl}}/swagger/apis/{{apiId}}`
- **说明**: 获取完整的接口详情，包括请求参数、响应参数、头部信息

### 3. 请求参数

#### 查询所有请求参数
- **方法**: GET
- **URL**: `{{baseUrl}}/swagger/apis/{{apiId}}/request-params`

#### 查询请求参数（按位置）
- **方法**: GET
- **URL**: `{{baseUrl}}/swagger/apis/{{apiId}}/request-params?location=query`
- **说明**: location可选值：path/header/query/form/body

#### 查询请求参数（按类型）
- **方法**: GET
- **URL**: `{{baseUrl}}/swagger/apis/{{apiId}}/request-params/type?paramType=String`
- **说明**: paramType可选值：String/Number/Boolean/Array/Object等

### 4. 响应参数

#### 查询所有响应参数
- **方法**: GET
- **URL**: `{{baseUrl}}/swagger/apis/{{apiId}}/response-params`

#### 查询响应参数（按状态码）
- **方法**: GET
- **URL**: `{{baseUrl}}/swagger/apis/{{apiId}}/response-params?statusCode=200`
- **说明**: statusCode可选值：200/400/500等HTTP状态码

## 使用示例

### 示例1：导入Swagger文档并查询

1. 执行"导入Swagger文档（通过URL）"请求
2. 从响应中获取导入的接口数量
3. 执行"查询接口列表（全部）"请求，查看导入的接口
4. 从接口列表中选择一个接口ID
5. 修改`{{apiId}}`变量为该接口ID
6. 执行"获取接口详情"请求，查看完整信息

### 示例2：按条件查询接口

1. 执行"查询接口列表（按方法）"请求，筛选GET方法的接口
2. 执行"查询接口列表（按标签）"请求，筛选特定标签的接口
3. 执行"查询接口列表（按路径）"请求，查找特定路径的接口

### 示例3：查询参数详情

1. 设置`{{apiId}}`为要查询的接口ID
2. 执行"查询所有请求参数"请求，查看所有请求参数
3. 执行"查询请求参数（按位置）"请求，筛选query参数
4. 执行"查询请求参数（按类型）"请求，筛选String类型参数
5. 执行"查询所有响应参数"请求，查看所有响应参数
6. 执行"查询响应参数（按状态码）"请求，查看200状态码的响应

## 响应格式

所有接口返回统一的响应格式：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    // 具体数据
  }
}
```

### 成功响应示例

```json
{
  "code": 200,
  "message": "成功导入10个接口",
  "data": 10
}
```

### 错误响应示例

```json
{
  "code": 500,
  "message": "导入失败: Swagger文档格式错误"
}
```

## 注意事项

1. **baseUrl配置**: 确保`baseUrl`变量指向正确的服务器地址
2. **apiId变量**: 查询接口详情或参数时，需要先设置正确的`apiId`
3. **Content-Type**: 导入接口需要设置`Content-Type: application/json`
4. **Swagger文档格式**: 确保导入的Swagger文档符合OpenAPI3或Swagger2规范
5. **网络连接**: 使用URL导入时，确保服务器可以访问该URL

## 故障排查

### 问题1：请求失败，提示连接错误

**解决方案**:
- 检查`baseUrl`是否正确
- 确认服务器是否已启动
- 检查网络连接

### 问题2：导入失败

**解决方案**:
- 检查Swagger文档格式是否正确
- 确认Content-Type设置为application/json
- 查看响应中的错误信息

### 问题3：查询不到数据

**解决方案**:
- 确认是否已成功导入Swagger文档
- 检查`apiId`是否正确
- 尝试先查询接口列表，确认接口是否存在

## 更新日志

- v1.0.0 (2024): 初始版本，包含所有基础API接口

