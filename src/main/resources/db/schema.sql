-- Swagger解析器数据库表结构
-- 数据库：swagger_parser
-- 字符集：utf8mb4

-- 创建数据库（如果不存在）
CREATE DATABASE IF NOT EXISTS swagger_parser DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE swagger_parser;

-- Swagger文档信息表
CREATE TABLE IF NOT EXISTS swagger_info (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    title VARCHAR(200) COMMENT 'Swagger文档标题',
    version VARCHAR(50) COMMENT 'Swagger文档版本',
    description TEXT COMMENT 'Swagger文档描述',
    swagger_version VARCHAR(10) COMMENT 'Swagger文档版本（v2/v3）',
    content LONGTEXT COMMENT '文档原始内容（可选，用于备份）',
    source VARCHAR(50) COMMENT '文档来源（file/url/content）',
    source_url VARCHAR(500) COMMENT '文档来源URL（如果通过URL导入）',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_time DATETIME COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Swagger文档信息表';

-- 服务器信息表
CREATE TABLE IF NOT EXISTS server_info (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    swagger_id BIGINT NOT NULL COMMENT 'Swagger文档ID（关联swagger_info表）',
    server_url VARCHAR(500) NOT NULL COMMENT '服务器URL',
    description VARCHAR(500) COMMENT '服务器描述',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    INDEX idx_swagger_id (swagger_id),
    FOREIGN KEY (swagger_id) REFERENCES swagger_info(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='服务器信息表';

-- 接口基础信息表
CREATE TABLE IF NOT EXISTS api_info (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    path VARCHAR(500) NOT NULL COMMENT '接口路径',
    method VARCHAR(10) NOT NULL COMMENT '请求方法（GET/POST/PUT/DELETE等）',
    swagger_id BIGINT NOT NULL COMMENT 'Swagger文档ID（关联swagger_info表）',
    description TEXT COMMENT '接口描述',
    tags VARCHAR(200) COMMENT '接口标签/分组',
    operation_id VARCHAR(200) COMMENT '操作ID（OpenAPI中的operationId）',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_time DATETIME COMMENT '更新时间',
    INDEX idx_path_method (path, method),
    INDEX idx_swagger_id (swagger_id),
    FOREIGN KEY (swagger_id) REFERENCES swagger_info(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='接口基础信息表';

-- 请求参数表
CREATE TABLE IF NOT EXISTS request_param (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    api_id BIGINT NOT NULL COMMENT '关联的接口ID',
    param_name VARCHAR(200) NOT NULL COMMENT '参数名称',
    location VARCHAR(50) NOT NULL COMMENT '参数位置（path/header/query/form/body等）',
    content_type VARCHAR(100) COMMENT 'Content类型（仅用于body类型参数，如application/json、application/xml等）',
    param_type VARCHAR(50) NOT NULL COMMENT '数据类型（String/Number/Boolean/Array/Object等）',
    required BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否必填（true/false）',
    pattern VARCHAR(5000) COMMENT '正则表达式',
    pattern_example VARCHAR(500) COMMENT '正则示例值（根据正则自动生成）',
    example LONGTEXT COMMENT '示例值（对于body类型，保存完整的JSON示例；对于其他类型，保存单个字段的示例值）',
    full_json_example LONGTEXT COMMENT '完整JSON示例（用于body类型，包含所有嵌套对象的完整JSON，与example字段内容相同）',
    description TEXT COMMENT '参数描述',
    hierarchy_path VARCHAR(500) COMMENT '层级路径（用于扁平化，如：user.name、items[0].id）',
    parent_id BIGINT COMMENT '父参数ID（用于构建层级关系）',
    INDEX idx_api_id (api_id),
    INDEX idx_location (location),
    INDEX idx_param_type (param_type),
    FOREIGN KEY (api_id) REFERENCES api_info(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='请求参数表';

-- 响应参数表
CREATE TABLE IF NOT EXISTS response_param (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    api_id BIGINT NOT NULL COMMENT '关联的接口ID',
    status_code VARCHAR(10) NOT NULL COMMENT 'HTTP状态码（200/400/500等）',
    location VARCHAR(50) NOT NULL COMMENT '响应位置（body/header）',
    param_name VARCHAR(200) NOT NULL COMMENT '参数名称',
    param_type VARCHAR(50) NOT NULL COMMENT '数据类型（String/Number/Boolean/Array/Object等）',
    pattern VARCHAR(1000) COMMENT '正则表达式',
    example VARCHAR(5000) COMMENT '示例值',
    description TEXT COMMENT '参数描述',
    hierarchy_path VARCHAR(500) COMMENT '层级路径（用于扁平化，如：data.user.name、items[0].id）',
    parent_id BIGINT COMMENT '父参数ID（用于构建层级关系）',
    INDEX idx_api_id (api_id),
    INDEX idx_status_code (status_code),
    INDEX idx_location (location),
    FOREIGN KEY (api_id) REFERENCES api_info(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='响应参数表';

