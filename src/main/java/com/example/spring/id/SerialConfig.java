package com.example.spring.id;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author chenghui
 * @date 2024/5/5 17:12
 */
@Configuration
public class SerialConfig {

    @Autowired
    private IDExpressionProperties idExpressionProperties;
    /**
     ** 咱们要创建一个ID工厂类，专门用来生成ID的类
     ** 使用方法：
     ** @Autowired
     ** private IDFactory idFactory；
     ** String id = idFactory.get("产品标志");
     */
    @Bean(initMethod = "init")
    public IDFactory serialIdFactory() {
        return new IDFactory(idExpressionProperties.getExpressions());
    }
}
