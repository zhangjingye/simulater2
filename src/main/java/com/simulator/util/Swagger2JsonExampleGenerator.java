package com.simulator.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.models.Model;
import io.swagger.models.ModelImpl;
import io.swagger.models.RefModel;
import io.swagger.models.Swagger;
import io.swagger.models.properties.*;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * Swagger 2.0 JSON示例生成器
 * 根据Swagger 2.0 Schema递归生成完整的JSON示例
 * 
 * @author simulator
 * @date 2024
 */
@Slf4j
public class Swagger2JsonExampleGenerator {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final int MAX_DEPTH = 20; // 最大递归深度，防止无限递归

    /**
     * 根据Property生成完整的JSON示例
     * 
     * @param property Property对象
     * @return JSON字符串
     */
    public static String generateJsonExample(Property property) {
        return generateJsonExample(property, null);
    }

    /**
     * 根据Property生成完整的JSON示例（支持$ref引用）
     * 
     * @param property Property对象
     * @param swagger Swagger对象（用于解析$ref引用）
     * @return JSON字符串
     */
    public static String generateJsonExample(Property property, Swagger swagger) {
        if (property == null) {
            log.warn("Property为null，无法生成JSON示例");
            return null;
        }

        try {
            Object example = generateExampleObject(property, 0, swagger);
            if (example == null) {
                log.warn("生成的示例对象为null");
                return null;
            }
            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(example);
            log.debug("成功生成JSON示例，长度: {}", json != null ? json.length() : 0);
            return json;
        } catch (Exception e) {
            log.error("生成JSON示例失败，Property类型: {}", property.getClass().getSimpleName(), e);
            return null;
        }
    }

    /**
     * 递归生成示例对象
     * 
     * @param property Property对象
     * @param depth 当前递归深度
     * @param swagger Swagger对象（用于解析$ref引用）
     * @return 示例对象
     */
    private static Object generateExampleObject(Property property, int depth, Swagger swagger) {
        if (depth > MAX_DEPTH) {
            log.warn("达到最大递归深度: {}", MAX_DEPTH);
            return null; // 防止无限递归
        }

        if (property == null) {
            log.warn("Property为null");
            return null;
        }

        // 检查是否是RefProperty（$ref引用）
        if (property instanceof RefProperty) {
            RefProperty refProperty = (RefProperty) property;
            String ref = refProperty.getSimpleRef();
            log.debug("发现$ref引用: {}", ref);
            
            // 解析$ref引用，格式如: ATMDefinitionMeta
            if (swagger != null && swagger.getDefinitions() != null) {
                Model model = swagger.getDefinitions().get(ref);
                if (model != null) {
                    log.debug("解析$ref引用成功，Model名称: {}", ref);
                    // 将Model转换为Property来递归处理
                    return generateExampleFromModel(model, depth, swagger);
                } else {
                    log.warn("无法找到$ref引用的Model: {}", ref);
                }
            }
        }

        // 记录Property类型，用于调试
        String propertyType = property.getClass().getSimpleName();
        log.debug("处理Property: type={}, depth={}", propertyType, depth);

        // 根据Property类型生成示例
        if (property instanceof ObjectProperty) {
            return generateObjectExample((ObjectProperty) property, depth, swagger);
        } else if (property instanceof ArrayProperty) {
            return generateArrayExample((ArrayProperty) property, depth, swagger);
        } else if (property instanceof StringProperty) {
            return generateStringExample((StringProperty) property);
        } else if (property instanceof IntegerProperty) {
            return generateIntegerExample((IntegerProperty) property);
        } else if (property instanceof LongProperty) {
            return generateLongExample((LongProperty) property);
        } else if (property instanceof FloatProperty) {
            return generateFloatExample((FloatProperty) property);
        } else if (property instanceof DoubleProperty) {
            return generateDoubleExample((DoubleProperty) property);
        } else if (property instanceof BooleanProperty) {
            return generateBooleanExample((BooleanProperty) property);
        } else if (property instanceof DateProperty) {
            return "2024-01-01";
        } else if (property instanceof DateTimeProperty) {
            return "2024-01-01T00:00:00Z";
        }
        
        // 检查type属性
        String type = property.getType();
        if (type != null) {
            log.debug("Property type属性: {}", type);
            // 根据type字符串生成
            switch (type) {
                case "string":
                    if (property instanceof StringProperty) {
                        return generateStringExample((StringProperty) property);
                    }
                    return "string";
                case "integer":
                    if (property instanceof IntegerProperty) {
                        return generateIntegerExample((IntegerProperty) property);
                    }
                    return 1;
                case "number":
                    if (property instanceof DecimalProperty) {
                        return generateDoubleExample((DoubleProperty) property);
                    }
                    return 1.0;
                case "boolean":
                    if (property instanceof BooleanProperty) {
                        return generateBooleanExample((BooleanProperty) property);
                    }
                    return true;
                case "array":
                    if (property instanceof ArrayProperty) {
                        return generateArrayExample((ArrayProperty) property, depth, swagger);
                    }
                    return Collections.emptyList();
                case "object":
                    if (property instanceof ObjectProperty) {
                        return generateObjectExample((ObjectProperty) property, depth, swagger);
                    }
                    return Collections.emptyMap();
                default:
                    log.warn("未知的Property type: {}", type);
                    return "example";
            }
        }

        log.warn("无法识别Property类型: {}, 返回默认值", propertyType);
        return "example";
    }

    /**
     * 根据Model生成完整的JSON示例
     * 
     * @param model Model对象
     * @param swagger Swagger对象（用于解析$ref引用）
     * @return JSON字符串
     */
    public static String generateJsonExampleFromModel(Model model, Swagger swagger) {
        if (model == null) {
            log.warn("Model为null，无法生成JSON示例");
            return null;
        }

        try {
            Object example = generateExampleFromModel(model, 0, swagger);
            if (example == null) {
                log.warn("生成的示例对象为null");
                return null;
            }
            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(example);
            log.debug("成功生成Model JSON示例，长度: {}", json != null ? json.length() : 0);
            return json;
        } catch (Exception e) {
            log.error("生成Model JSON示例失败，Model类型: {}", model.getClass().getSimpleName(), e);
            return null;
        }
    }

    /**
     * 从Model生成示例对象
     */
    private static Object generateExampleFromModel(Model model, int depth, Swagger swagger) {
        // 处理RefModel（$ref引用）
        if (model instanceof RefModel) {
            RefModel refModel = (RefModel) model;
            String ref = refModel.getSimpleRef();
            log.debug("发现RefModel引用: {}", ref);
            
            // 解析$ref引用
            if (swagger != null && swagger.getDefinitions() != null) {
                Model refModelDef = swagger.getDefinitions().get(ref);
                if (refModelDef != null) {
                    log.debug("解析RefModel引用成功，Model名称: {}", ref);
                    // 递归处理引用的Model
                    return generateExampleFromModel(refModelDef, depth, swagger);
                } else {
                    log.warn("无法找到RefModel引用的Model: {}", ref);
                }
            }
            return Collections.emptyMap();
        }
        
        // 处理ModelImpl（直接定义的模型）
        if (model instanceof ModelImpl) {
            ModelImpl modelImpl = (ModelImpl) model;
            Map<String, Property> properties = modelImpl.getProperties();
            if (properties != null && !properties.isEmpty()) {
                Map<String, Object> example = new LinkedHashMap<>();
                for (Map.Entry<String, Property> entry : properties.entrySet()) {
                    String propertyName = entry.getKey();
                    Property property = entry.getValue();
                    Object value = generateExampleObject(property, depth + 1, swagger);
                    if (value != null) {
                        example.put(propertyName, value);
                    }
                }
                return example;
            }
        }
        return Collections.emptyMap();
    }

    /**
     * 生成对象示例
     */
    private static Map<String, Object> generateObjectExample(ObjectProperty objectProperty, int depth, Swagger swagger) {
        Map<String, Object> example = new LinkedHashMap<>();
        
        Map<String, Property> properties = objectProperty.getProperties();
        if (properties == null || properties.isEmpty()) {
            return example;
        }

        for (Map.Entry<String, Property> entry : properties.entrySet()) {
            String propertyName = entry.getKey();
            Property property = entry.getValue();
            
            Object value = generateExampleObject(property, depth + 1, swagger);
            if (value != null) {
                example.put(propertyName, value);
            }
        }

        return example;
    }

    /**
     * 生成数组示例
     */
    private static List<Object> generateArrayExample(ArrayProperty arrayProperty, int depth, Swagger swagger) {
        List<Object> example = new ArrayList<>();
        
        Property items = arrayProperty.getItems();
        if (items != null) {
            Object itemExample = generateExampleObject(items, depth + 1, swagger);
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
    private static String generateStringExample(StringProperty stringProperty) {
        // 如果有枚举值，使用第一个
        List<String> enumValues = stringProperty.getEnum();
        if (enumValues != null && !enumValues.isEmpty()) {
            return enumValues.get(0);
        }
        
        // 如果有正则表达式，尝试生成
        if (stringProperty.getPattern() != null) {
            String patternExample = RegexExampleGenerator.generateExample(stringProperty.getPattern());
            if (patternExample != null) {
                return patternExample;
            }
        }
        
        // 如果有format，根据format生成
        String format = stringProperty.getFormat();
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
    private static Integer generateIntegerExample(IntegerProperty integerProperty) {
        java.math.BigDecimal minimum = integerProperty.getMinimum();
        java.math.BigDecimal maximum = integerProperty.getMaximum();
        
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
     * 生成长整数示例
     */
    private static Long generateLongExample(LongProperty longProperty) {
        java.math.BigDecimal minimum = longProperty.getMinimum();
        java.math.BigDecimal maximum = longProperty.getMaximum();
        
        if (minimum != null && maximum != null) {
            return minimum.add(maximum).divide(new java.math.BigDecimal("2")).longValue();
        } else if (minimum != null) {
            return minimum.longValue();
        } else if (maximum != null) {
            return Math.min(maximum.longValue(), 100L);
        }
        
        return 1L;
    }

    /**
     * 生成浮点数示例
     */
    private static Float generateFloatExample(FloatProperty floatProperty) {
        java.math.BigDecimal minimum = floatProperty.getMinimum();
        java.math.BigDecimal maximum = floatProperty.getMaximum();
        
        if (minimum != null && maximum != null) {
            return minimum.add(maximum).divide(new java.math.BigDecimal("2")).floatValue();
        } else if (minimum != null) {
            return minimum.floatValue();
        } else if (maximum != null) {
            return Math.min(maximum.floatValue(), 100.0f);
        }
        
        return 1.0f;
    }

    /**
     * 生成双精度数示例
     */
    private static Double generateDoubleExample(DoubleProperty doubleProperty) {
        java.math.BigDecimal minimum = doubleProperty.getMinimum();
        java.math.BigDecimal maximum = doubleProperty.getMaximum();
        
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
    private static Boolean generateBooleanExample(BooleanProperty booleanProperty) {
        return true;
    }
}

