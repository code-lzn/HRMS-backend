package com.limou.hrms.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 敏感数据脱敏工具类单元测试 — 覆盖四种脱敏方法及边界情况
 */
class SensitiveDataUtilTest {

    // ==================== 身份证号 ====================

    /** 标准 18 位身份证：保留前 4 位 + 后 4 位，中间 10 位替换为星号 */
    @Test
    void maskIdCard_normal_shouldKeepFirst4AndLast4() {
        assertEquals("3301**********1234", SensitiveDataUtil.maskIdCard("330100199001011234"));
    }

    /** null 输入 → 返回 null */
    @Test
    void maskIdCard_null_shouldReturnNull() {
        assertNull(SensitiveDataUtil.maskIdCard(null));
    }

    /** 长度不足 8 位 → 原样返回 */
    @Test
    void maskIdCard_tooShort_shouldReturnOriginal() {
        assertEquals("330100", SensitiveDataUtil.maskIdCard("330100"));
    }

    // ==================== 手机号 ====================

    /** 标准 11 位手机号：保留前 3 位 + 后 4 位 */
    @Test
    void maskPhone_normal_shouldKeepFirst3AndLast4() {
        assertEquals("138****5678", SensitiveDataUtil.maskPhone("13812345678"));
    }

    /** null 输入 → 返回 null */
    @Test
    void maskPhone_null_shouldReturnNull() {
        assertNull(SensitiveDataUtil.maskPhone(null));
    }

    /** 长度不足 7 位 → 原样返回 */
    @Test
    void maskPhone_tooShort_shouldReturnOriginal() {
        assertEquals("123456", SensitiveDataUtil.maskPhone("123456"));
    }

    // ==================== 银行卡号 ====================

    /** 标准银行卡号：仅保留后 4 位，其余填充星号 */
    @Test
    void maskBankCard_normal_shouldKeepLast4() {
        String result = SensitiveDataUtil.maskBankCard("6222021234567890123");
        assertTrue(result.startsWith("***************")); // 15 位星号
        assertTrue(result.endsWith("0123"));
    }

    /** null 输入 → 返回 null */
    @Test
    void maskBankCard_null_shouldReturnNull() {
        assertNull(SensitiveDataUtil.maskBankCard(null));
    }

    /** 长度不足 4 位 → 原样返回 */
    @Test
    void maskBankCard_tooShort_shouldReturnOriginal() {
        assertEquals("123", SensitiveDataUtil.maskBankCard("123"));
    }

    // ==================== 姓名 ====================

    /** 双字姓名：保留姓，名替换为星号 */
    @Test
    void maskName_twoChars_shouldMaskSecond() {
        assertEquals("张*", SensitiveDataUtil.maskName("张三"));
    }

    /** 三字姓名：保留姓，后两字替换为星号 */
    @Test
    void maskName_threeChars_shouldMaskLastTwo() {
        assertEquals("张**", SensitiveDataUtil.maskName("张三丰"));
    }

    /** 单字姓名 → 返回单一星号 */
    @Test
    void maskName_singleChar_shouldReturnStar() {
        assertEquals("*", SensitiveDataUtil.maskName("张"));
    }

    /** null 输入 → 返回 null */
    @Test
    void maskName_null_shouldReturnNull() {
        assertNull(SensitiveDataUtil.maskName(null));
    }

    /** 空字符串 → 返回空字符串 */
    @Test
    void maskName_empty_shouldReturnEmpty() {
        assertEquals("", SensitiveDataUtil.maskName(""));
    }
}
