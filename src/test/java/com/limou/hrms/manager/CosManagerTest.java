package com.limou.hrms.manager;

import javax.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Cos 操作测试
 */
@SpringBootTest
class CosManagerTest {

    @Resource
    private CosManager cosManager;

    @Test
    void putObject() {
        // COS 凭证未配置时，putObject 应抛出 IllegalStateException
        assertThrows(IllegalStateException.class,
                () -> cosManager.putObject("test", "test.json"));
    }
}