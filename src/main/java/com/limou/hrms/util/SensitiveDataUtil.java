package com.limou.hrms.util;

/**
 * 敏感数据脱敏工具类
 */
public class SensitiveDataUtil {

    private SensitiveDataUtil() {
    }

    /**
     * 身份证号脱敏：保留前4位+后4位，中间替换为星号
     */
    public static String maskIdCard(String idCard) {
        if (idCard == null || idCard.length() < 8) {
            return idCard;
        }
        int maskLen = idCard.length() - 8;
        StringBuilder sb = new StringBuilder();
        sb.append(idCard, 0, 4);
        for (int i = 0; i < maskLen; i++) {
            sb.append('*');
        }
        sb.append(idCard.substring(idCard.length() - 4));
        return sb.toString();
    }

    /**
     * 手机号脱敏：保留前3位+后4位，中间替换为星号
     */
    public static String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    /**
     * 银行卡号脱敏：保留后4位，其余替换为星号
     */
    public static String maskBankCard(String bankCard) {
        if (bankCard == null || bankCard.length() < 4) {
            return bankCard;
        }
        int maskLen = bankCard.length() - 4;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < maskLen; i++) {
            sb.append('*');
        }
        sb.append(bankCard.substring(bankCard.length() - 4));
        return sb.toString();
    }

    /**
     * 姓名脱敏：保留姓，名替换为星号
     */
    public static String maskName(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        if (name.length() == 1) {
            return "*";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(name.charAt(0));
        for (int i = 1; i < name.length(); i++) {
            sb.append('*');
        }
        return sb.toString();
    }
}
