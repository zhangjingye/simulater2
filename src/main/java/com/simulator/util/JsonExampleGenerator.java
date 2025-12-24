package com.simulator.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.*;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * JSON示例生成器
 * 根据OpenAPI Schema递归生成完整的JSON示例
 * 
 * @author simulator
 * @date 2024
 */
@Slf4j
public class JsonExampleGenerator {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final int MAX_DEPTH = 20; // 最大递归深度，防止无限递归（增加到20以支持深层嵌套）

    /**
     * 根据Schema生成完整的JSON示例
     * 
     * @param schema Schema对象
     * @return JSON字符串
     */
    public static String generateJsonExample(Schema schema) {
        return generateJsonExample(schema, null);
    }

    /**
     * 根据Schema生成完整的JSON示例（支持$ref引用）
     * 
     * @param schema Schema对象
     * @param openAPI OpenAPI对象（用于解析$ref引用）
     * @return JSON字符串
     */
    public static String generateJsonExample(Schema schema, OpenAPI openAPI) {
        if (schema == null) {
            log.warn("Schema为null，无法生成JSON示例");
            return null;
        }

        try {
            Object example = generateExampleObject(schema, 0, openAPI);
            if (example == null) {
                log.warn("生成的示例对象为null");
                return null;
            }
            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(example);
            log.debug("成功生成JSON示例，长度: {}", json != null ? json.length() : 0);
            return json;
        } catch (Exception e) {
            log.error("生成JSON示例失败，Schema类型: {}", schema.getClass().getSimpleName(), e);
            return null;
        }
    }

    /**
     * 递归生成示例对象
     * 
     * @param schema Schema对象
     * @param depth 当前递归深度
     * @param openAPI OpenAPI对象（用于解析$ref引用）
     * @return 示例对象
     */
    private static Object generateExampleObject(Schema schema, int depth, OpenAPI openAPI) {
        if (depth > MAX_DEPTH) {
            log.warn("达到最大递归深度: {}", MAX_DEPTH);
            return null; // 防止无限递归
        }

        if (schema == null) {
            log.warn("Schema为null");
            return null;
        }

        // 检查是否有$ref引用
        if (schema.get$ref() != null && openAPI != null) {
            String ref = schema.get$ref();
            log.debug("发现$ref引用: {}", ref);
            
            // 解析$ref引用，格式如: #/components/schemas/OBWriteDomesticConsent4
            if (ref.startsWith("#/components/schemas/")) {
                String schemaName = ref.substring("#/components/schemas/".length());
                if (openAPI.getComponents() != null && openAPI.getComponents().getSchemas() != null) {
                    Schema refSchema = openAPI.getComponents().getSchemas().get(schemaName);
                    if (refSchema != null) {
                        log.debug("解析$ref引用成功，Schema名称: {}", schemaName);
                        // 递归处理引用的Schema
                        return generateExampleObject(refSchema, depth, openAPI);
                    } else {
                        log.warn("无法找到$ref引用的Schema: {}", schemaName);
                    }
                }
            }
        }

        // 记录Schema类型，用于调试
        String schemaType = schema.getClass().getSimpleName();
        String schemaName = schema.getName();
        log.debug("处理Schema: type={}, name={}, depth={}", schemaType, schemaName, depth);

        // 对于对象类型，总是生成完整的对象结构，忽略Schema的example（因为example可能是简单字符串）
        // 这样可以确保生成完整的嵌套对象结构
        if (schema instanceof ObjectSchema) {
            ObjectSchema objectSchema = (ObjectSchema) schema;
            log.debug("识别为ObjectSchema，properties数量: {}", 
                objectSchema.getProperties() != null ? objectSchema.getProperties().size() : 0);
            return generateObjectExample(objectSchema, depth, openAPI);
        } else if (schema instanceof ArraySchema) {
            return generateArrayExample((ArraySchema) schema, depth, openAPI);
        } else if (schema instanceof StringSchema) {
            return generateStringExample((StringSchema) schema);
        } else if (schema instanceof IntegerSchema) {
            return generateIntegerExample((IntegerSchema) schema);
        } else if (schema instanceof NumberSchema) {
            return generateNumberExample((NumberSchema) schema);
        } else if (schema instanceof BooleanSchema) {
            return generateBooleanExample((BooleanSchema) schema);
        } else if (schema instanceof DateSchema) {
            return "2024-01-01";
        } else if (schema instanceof DateTimeSchema) {
            return "2024-01-01T00:00:00Z";
        }
        
        // 检查type属性
        String type = schema.getType();
        if (type != null) {
            log.debug("Schema type属性: {}", type);
            // 根据type字符串生成
            switch (type) {
                case "string":
                    if (schema instanceof StringSchema) {
                        return generateStringExample((StringSchema) schema);
                    }
                    return "string";
                case "integer":
                    if (schema instanceof IntegerSchema) {
                        return generateIntegerExample((IntegerSchema) schema);
                    }
                    return 1;
                case "number":
                    if (schema instanceof NumberSchema) {
                        return generateNumberExample((NumberSchema) schema);
                    }
                    return 1.0;
                case "boolean":
                    if (schema instanceof BooleanSchema) {
                        return generateBooleanExample((BooleanSchema) schema);
                    }
                    return true;
                case "array":
                    if (schema instanceof ArraySchema) {
                        return generateArrayExample((ArraySchema) schema, depth, openAPI);
                    }
                    return Collections.emptyList();
                case "object":
                    // 即使不是ObjectSchema实例，如果type是object，尝试获取properties
                    if (schema.getProperties() != null && !schema.getProperties().isEmpty()) {
                        log.debug("Schema type是object且有properties，数量: {}", schema.getProperties().size());
                        // 创建一个临时的ObjectSchema来处理
                        ObjectSchema tempSchema = new ObjectSchema();
                        tempSchema.setProperties(schema.getProperties());
                        tempSchema.setRequired(schema.getRequired());
                        return generateObjectExample(tempSchema, depth, openAPI);
                    }
                    return Collections.emptyMap();
                default:
                    log.warn("未知的Schema type: {}", type);
                    return "example";
            }
        }

        // 如果既没有类型实例，也没有type属性，检查是否有properties（可能是通过$ref解析后的Schema）
        if (schema.getProperties() != null && !schema.getProperties().isEmpty()) {
            log.debug("Schema没有type但有properties，数量: {}", schema.getProperties().size());
            ObjectSchema tempSchema = new ObjectSchema();
            tempSchema.setProperties(schema.getProperties());
            tempSchema.setRequired(schema.getRequired());
            return generateObjectExample(tempSchema, depth, openAPI);
        }

        log.warn("无法识别Schema类型: {}, 返回默认值", schemaType);
        return "example";
    }

    /**
     * 生成对象示例
     */
    private static Map<String, Object> generateObjectExample(ObjectSchema objectSchema, int depth, OpenAPI openAPI) {
        Map<String, Object> example = new LinkedHashMap<>();
        
        if (objectSchema.getProperties() == null || objectSchema.getProperties().isEmpty()) {
            return example;
        }

        for (Map.Entry<String, Schema> entry : objectSchema.getProperties().entrySet()) {
            String propertyName = entry.getKey();
            Schema propertySchema = entry.getValue();
            
            // 生成所有字段的示例（移除深度限制，确保生成完整的对象结构）
            Object value = generateExampleObject(propertySchema, depth + 1, openAPI);
            if (value != null) {
                example.put(propertyName, value);
            }
        }

        return example;
    }

    /**
     * 生成数组示例
     */
    private static List<Object> generateArrayExample(ArraySchema arraySchema, int depth, OpenAPI openAPI) {
        List<Object> example = new ArrayList<>();
        
        if (arraySchema.getItems() != null) {
            Object itemExample = generateExampleObject(arraySchema.getItems(), depth + 1, openAPI);
            if (itemExample != null) {
                // 添加一个元素作为示例
                example.add(itemExample);
            }
        } else {
            example.add("example");
        }

        return example;
    }

    /**
     * 生成字符串示例
     */
    private static String generateStringExample(StringSchema stringSchema) {
        // 如果有枚举值，使用第一个
        List<String> enumValues = stringSchema.getEnum();
        if (enumValues != null && !enumValues.isEmpty()) {
            return enumValues.get(0);
        }
        
        // 如果有正则表达式，尝试生成
        if (stringSchema.getPattern() != null) {
            String patternExample = RegexExampleGenerator.generateExample(stringSchema.getPattern());
            if (patternExample != null) {
                return patternExample;
            }
        }
        
        // 如果有format，根据format生成
        String format = stringSchema.getFormat();
        if (format != null) {
            switch (format) {
                case "email":
                    return "example@test.com";
                case "uri":
                    return "https://example.com";
                case "uuid":
                    return UUID.randomUUID().toString();
                case "date":
                    return "2024-01-01";
                case "date-time":
                    return "2024-01-01T00:00:00Z";
                case "ipv4":
                    return "192.168.1.1";
                case "ipv6":
                    return "2001:0db8:85a3:0000:0000:8a2e:0370:7334";
                default:
                    return "string";
            }
        }
        
        return "string";
    }

    /**
     * 生成整数示例
     */
    private static Integer generateIntegerExample(IntegerSchema integerSchema) {
        List<Number> enumValues = integerSchema.getEnum();
        if (enumValues != null && !enumValues.isEmpty()) {
            Number firstEnum = enumValues.get(0);
            return firstEnum.intValue();
        }
        
        java.math.BigDecimal minimum = integerSchema.getMinimum();
        java.math.BigDecimal maximum = integerSchema.getMaximum();
        
        if (minimum != null && maximum != null) {
            return minimum.add(maximum).divide(new java.math.BigDecimal("2")).intValue();
        } else if (minimum != null) {
            return minimum.intValue();
        } else if (maximum != null) {
            return Math.min(maximum.intValue(), 100);
        }
        
        return 1;
    }

    /**
     * 生成数字示例
     */
    private static Double generateNumberExample(NumberSchema numberSchema) {
        List<java.math.BigDecimal> enumValues = numberSchema.getEnum();
        if (enumValues != null && !enumValues.isEmpty()) {
            java.math.BigDecimal firstEnum = enumValues.get(0);
            return firstEnum.doubleValue();
        }
        
        java.math.BigDecimal minimum = numberSchema.getMinimum();
        java.math.BigDecimal maximum = numberSchema.getMaximum();
        
        if (minimum != null && maximum != null) {
            return minimum.add(maximum).divide(new java.math.BigDecimal("2")).doubleValue();
        } else if (minimum != null) {
            return minimum.doubleValue();
        } else if (maximum != null) {
            return Math.min(maximum.doubleValue(), 100.0);
        }
        
        return 1.0;
    }

    /**
     * 生成布尔示例
     */
    private static Boolean generateBooleanExample(BooleanSchema booleanSchema) {
        return true;
    }
}

