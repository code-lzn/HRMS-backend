package com.limou.hrms.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * AES-256 加解密工具
 * <p>
 * 用于身份证号、银行卡号等敏感字段的加密存储。
 * 密钥通过配置中心/application.yml 管理，支持密钥轮换。
 */
@Slf4j
@Component
public class AesUtil {

    private static final String ALGORITHM = "AES";

    private final String secretKey;

    public AesUtil(@Value("${hrms.aes.secret-key:limou-hrms-aes-key-256bit!!}") String secretKey) {
        // AES-256 要求密钥 32 字节，不足补 0，超出截断
        byte[] keyBytes = new byte[32];
        byte[] srcBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        int len = Math.min(srcBytes.length, 32);
        System.arraycopy(srcBytes, 0, keyBytes, 0, len);
        this.secretKey = new String(keyBytes, StandardCharsets.UTF_8);
    }

    /**
     * AES 加密，返回 Base64 编码的密文
     */
    public String encrypt(String plainText) {
        if (plainText == null) {
            return null;
        }
        try {
            SecretKeySpec keySpec = new SecretKeySpec(
                    secretKey.getBytes(StandardCharsets.UTF_8), ALGORITHM);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            log.error("AES encrypt error", e);
            throw new RuntimeException("AES encrypt error", e);
        }
    }

    /**
     * AES 解密，输入 Base64 编码的密文，返回明文。解密失败返回 null。
     */
    public String decrypt(String cipherText) {
        if (cipherText == null) {
            return null;
        }
        try {
            SecretKeySpec keySpec = new SecretKeySpec(
                    secretKey.getBytes(StandardCharsets.UTF_8), ALGORITHM);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(cipherText));
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.debug("AES decrypt failed — may be plaintext: {}", e.getMessage());
            return null;
        }
    }
}
