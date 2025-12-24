package com.simulator.util;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;

import java.util.regex.Pattern;

/**
 * 正则表达式示例值生成器
 * 根据正则表达式自动生成符合规则的示例值
 * 
 * @author simulator
 * @date 2024
 */
public class RegexExampleGenerator {

    /**
     * 根据正则表达式生成示例值
     * 
     * @param pattern 正则表达式
     * @return 符合正则的示例值，如果无法生成则返回null
     */
    public static String generateExample(String pattern) {
        if (StrUtil.isBlank(pattern)) {
            return null;
        }

        try {
            // 常见正则模式处理
            String example = handleCommonPatterns(pattern);
            if (example != null) {
                return example;
            }

            // 尝试匹配并生成
            return generateByPattern(pattern);
        } catch (Exception e) {
            // 如果生成失败，返回null
            return null;
        }
    }

    /**
     * 处理常见正则模式
     */
    private static String handleCommonPatterns(String pattern) {
        // 数字相关
        if (pattern.matches(".*\\\\d\\{([0-9]+)\\}\\$.*")) {
            // 匹配 \d{4} 这样的模式
            String count = pattern.replaceAll(".*\\\\d\\{([0-9]+)\\}\\$.*", "$1");
            int num = Integer.parseInt(count);
            return RandomUtil.randomNumbers(num);
        }
        
        if (pattern.contains("\\d+")) {
            // 匹配一个或多个数字
            return RandomUtil.randomNumbers(4);
        }

        // 字母相关
        if (pattern.matches(".*[a-zA-Z]\\{([0-9]+)\\}\\$.*")) {
            String count = pattern.replaceAll(".*[a-zA-Z]\\{([0-9]+)\\}\\$.*", "$1");
            int num = Integer.parseInt(count);
            return RandomUtil.randomString("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ", num);
        }

        // 邮箱
        if (pattern.contains("email") || pattern.contains("@")) {
            return "example@test.com";
        }

        // 手机号（中国）
        if (pattern.contains("1[3-9]\\d{9}") || pattern.contains("手机")) {
            return "13800138000";
        }

        // UUID
        if (pattern.contains("uuid") || pattern.contains("UUID")) {
            return java.util.UUID.randomUUID().toString();
        }

        // 日期
        if (pattern.contains("yyyy-MM-dd") || pattern.contains("\\d{4}-\\d{2}-\\d{2}")) {
            return "2024-01-01";
        }

        // 时间
        if (pattern.contains("HH:mm:ss") || pattern.contains("\\d{2}:\\d{2}:\\d{2}")) {
            return "12:00:00";
        }

        return null;
    }

    /**
     * 通过正则模式生成示例值
     */
    private static String generateByPattern(String pattern) {
        try {
            Pattern.compile(pattern);
            
            // 简单模式：纯数字
            if (pattern.matches("^\\^?\\\\d+\\+?\\$?$")) {
                return RandomUtil.randomNumbers(6);
            }

            // 简单模式：纯字母
            if (pattern.matches("^\\^?[a-zA-Z]+\\+?\\$?$")) {
                return RandomUtil.randomString("abcdefghijklmnopqrstuvwxyz", 6);
            }

            // 字母数字组合
            if (pattern.contains("[a-zA-Z0-9]")) {
                return RandomUtil.randomString("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789", 8);
            }

            // 默认返回一个随机字符串
            return RandomUtil.randomString(8);
        } catch (Exception e) {
            return null;
        }
    }
}


