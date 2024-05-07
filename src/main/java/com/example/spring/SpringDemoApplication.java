package com.example.spring;

import cn.hutool.core.date.DateUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.example.spring.id.Expression;
import com.example.spring.id.ExpressionElement;
import com.example.spring.id.SerialIDVariableGenerator;
import com.example.spring.id.VariableGenerator;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.type.DateTime;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.util.Date;

@Import(cn.hutool.extra.spring.SpringUtil.class)
// 扫描cn.hutool.extra.spring包下所有类并注册之
//@ComponentScan(basePackages={"cn.hutool.extra.spring"})
@Slf4j
@SpringBootApplication
public class SpringDemoApplication {
    @Autowired
    protected Environment env;

    public static void main(String[] args) {
        final ConfigurableApplicationContext run = SpringApplication.run(SpringDemoApplication.class, args);
    }

    @Bean
    public VariableGenerator pidVariableGenerator() {
        return new VariableGenerator() {
            @Override
            public String apply(ExpressionElement e, Expression expression) {
                return expression.getKey();
            }
        };
    }

    private static final String YEAR_MONTH_DAY_HOUR_MINUTE_SECOND_FORMAT = "yyyyMMddHHmmss";

    @Bean
    public VariableGenerator yearMonthDayHmsVariableGenerator() {
        return new VariableGenerator() {
            @Override
            public String apply(ExpressionElement e, Expression expression) {
                return DateUtil.format(new Date(), YEAR_MONTH_DAY_HOUR_MINUTE_SECOND_FORMAT);
            }
        };
    }

    @Bean
    public SerialIDVariableGenerator idVariableGenerator() {
        final SerialIDVariableGenerator serialIDVariableGenerator = new SerialIDVariableGenerator();
//        serialIDVariableGenerator.initParams();
        return serialIDVariableGenerator;
    }
}
