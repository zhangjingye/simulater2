package com.simulator.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 正则表达式示例值生成器测试类
 * 
 * @author simulator
 * @date 2024
 */
public class RegexExampleGeneratorTest {

    @Test
    public void testGenerateExampleForDigits() {
        String pattern = "^\\d{4}$";
        String example = RegexExampleGenerator.generateExample(pattern);
        assertNotNull(example, "应该生成示例值");
        assertEquals(4, example.length(), "应该是4位数字");
        assertTrue(example.matches("\\d{4}"), "应该符合正则表达式");
    }

    @Test
    public void testGenerateExampleForEmail() {
        String pattern = ".*email.*";
        String example = RegexExampleGenerator.generateExample(pattern);
        assertNotNull(example, "应该生成示例值");
        assertTrue(example.contains("@"), "应该是邮箱格式");
    }

    @Test
    public void testGenerateExampleForPhone() {
        String pattern = ".*1[3-9]\\d{9}.*";
        String example = RegexExampleGenerator.generateExample(pattern);
        assertNotNull(example, "应该生成示例值");
    }

    @Test
    public void testGenerateExampleForBlank() {
        String example = RegexExampleGenerator.generateExample("");
        assertNull(example, "空字符串应该返回null");
        
        example = RegexExampleGenerator.generateExample(null);
        assertNull(example, "null应该返回null");
    }
}

