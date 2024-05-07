package com.example.spring.id;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author chenghui
 * @date 2024/5/5 17:51
 */
@SpringBootTest
class SerialConfigTest {
    @Autowired IDFactory idFactory;
    @Autowired
    RedisTemplate redisTemplate;

    @Test
    void serialIdFactory() {
        final List o = redisTemplate.opsForList().range("IdGenerator:XM", 0, 1);
        System.out.println(o);
        final Boolean b = redisTemplate.hasKey("IdGenerator:XM");
        System.out.println(b);
        final Long size = redisTemplate.opsForList().size("IdGenerator:XM");
        System.out.println(size);
        for (int i = 0; i < 100; i++) {
            System.out.println(idFactory.get("TEST"));
        }
        System.out.println(idFactory.get("TEST"));
        System.out.println(idFactory.get("TEST"));
        System.out.println(idFactory.get("TEST"));
    }
}
