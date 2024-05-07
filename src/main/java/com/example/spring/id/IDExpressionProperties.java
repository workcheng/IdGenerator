package com.example.spring.id;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * @author chenghui
 * @date 2024/5/5 17:12
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "id.generator")
public class IDExpressionProperties {

    private Map<String, SerialIdConfig> expressions;

    @Data
    public static class SerialIdConfig {
        // 表达式
        private String exp;
        // 初始化字段
        private String[] initFields;
    }
}
