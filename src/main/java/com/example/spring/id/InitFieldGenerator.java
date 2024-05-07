package com.example.spring.id;

/**
 * @author chenghui
 * @date 2024/5/5 17:21
 */
public abstract class InitFieldGenerator {

    public static final String INIT_FIELD_GENERATOR = "InitFieldGenerator";
    // 执行初始化操作
    public abstract String generator(String key, String initField);
}
