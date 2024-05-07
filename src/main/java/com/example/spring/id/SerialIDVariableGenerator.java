package com.example.spring.id;

/**
 * @author chenghui
 * @date 2024/5/5 17:27
 */

import com.google.common.collect.Lists;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.serializer.RedisSerializer;

@Slf4j
public class SerialIDVariableGenerator extends VariableGenerator {

    @Autowired
    private RedisTemplate redisTemplate;

    private static InitParams initParams;

    static {
        log.info("static initParams");
        initParams("IdGenerator", "XM:1:9:30:50:100:1");
    }

    // 构造函数
    public static void initParams(String key, String initFields) {
        initParams = parse(key, initFields);
    }
    /**
     *  解析表达式   字段名:初始值:最大值:最小数量:扩容数量:初始数量:增长步长
     *
     * @param initField
     */
    private static InitParams parse(String key, String initField) {
        InitParams initParams = new InitParams();
        if (StringUtils.contains(initField, COLON)) {
            String[] params = StringUtils.splitByWholeSeparatorPreserveAllTokens(initField, COLON);
            initParams.setFieldName(key + COLON + params[0]);
            initParams.setField(params);
        } else {
            initParams.setFieldName(key + COLON + initField);
            initParams.updateFields();
        }
        return initParams;
    }
    // 执行lua脚本，生成对应的自增id
    public String generate() {
        if (initParams == null) {
            this.initParams = new InitParams();
            this.initParams.updateFields();
        }
        final InitParams initParams = this.initParams;
        String fieldName = initParams.getFieldName();
        return executeLua(
                fieldName,
                initParams.getInitValue(),
                initParams.getMaxValue(),
                initParams.getMinCount(),
                initParams.getInitCount(),
                initParams.getExpansionStep(),
                initParams.getIncrStep());
    }
    // 执行生成函数
    @Override
    protected String apply(ExpressionElement e, Expression expression) {
        return generate();
    }
    // 执行lua脚本
    private String executeLua(
            String key,
            int initValue,
            int maxValue,
            int minCount,
            int initCount,
            int expansionStep,
            int incrStep) {
        // 执行lua脚本
        DefaultRedisScript<String> defaultRedisScript = new DefaultRedisScript<>();
        defaultRedisScript.setResultType(String.class);
        defaultRedisScript.setScriptText(LUA_SCRIPT);
        RedisSerializer<String> serializer = redisTemplate.getStringSerializer();
        String result =
                CastUtil.castString(
                        redisTemplate.execute(
                                defaultRedisScript,
                                serializer,
                                serializer,
                                Lists.newArrayList(key),
                                CastUtil.castString(initValue),
                                CastUtil.castString(maxValue),
                                CastUtil.castString(minCount),
                                CastUtil.castString(initCount),
                                CastUtil.castString(expansionStep),
                                CastUtil.castString(incrStep)));
        return result;
    }

    @Data
    private static class InitParams {

        /** 默认初始值 */
        private static final int DEFAULT_INIT_VALUE = 1;
        /** 默认最大值 */
        private static final int DEFAULT_MAX_VALUE = 9999;
        /** 默认最小数量 */
        private static final int DEFAULT_MIN_COUNT = 30;
        /** 默认初始数量 */
        private static final int DEFAULT_INIT_COUNT = 100;
        /** 默认扩容数量 */
        private static final int DEFAULT_EXPANSION_STEP = 50;
        /** 默认自增步长 */
        private static final int DEFAULT_INCR_STEP = 1;

        private final int[] params = {
                0,
                DEFAULT_INIT_VALUE,
                DEFAULT_MAX_VALUE,
                DEFAULT_MIN_COUNT,
                DEFAULT_EXPANSION_STEP,
                DEFAULT_INIT_COUNT,
                DEFAULT_INCR_STEP
        };
        /** 字段名称，其实就是key */
        private String fieldName;
        /** 初始值 */
        private int initValue;
        /** 最大值 */
        private int maxValue;
        /** 最小数量 */
        private int minCount;
        /** 扩容步长 */
        private int expansionStep;
        /** 初始数量 */
        private int initCount;
        /** 自增步长 */
        private int incrStep;

        public void setField(Object[] objects) {
            if (ArrayUtils.isEmpty(objects) || ArrayUtils.getLength(objects) < 2) {
                return;
            }
            for (int i = 1; i < objects.length; i++) {
                Object obj = objects[i];
                params[i] = CastUtil.castInt(obj);
            }
            updateFields();
        }

        public void updateFields() {
            this.initValue = params[1];
            this.maxValue = params[2];
            this.minCount = params[3];
            this.expansionStep = params[4];
            this.initCount = params[5];
            this.incrStep = params[6];
        }
    }
    // 该脚本的执行逻辑
    // 在redis中生成一个队列，指定初始化长度，第一个初始值，最大值，队列最小数量，每次扩容的数量，自增的步长
    // 1.如果队列不存在，就初始化队列，按照给定的初始化长度，初始值，自增步长，最大值等参数创建一个队列
    // 2.如果队列中值的数量超过队列最小数量，那么直接pop出一个值
    // 3.如果小于最小数量，那么直接循环生成指定步长的自增ID
    // 4.最终会pop出第一个数值
    // 5.如果是初始化的话，会返回success，否则就直接pop出第一个ID
    private static final String LUA_SCRIPT =
            "local key=KEYS[1]\nlocal initValue=tonumber(ARGV[1])\nlocal maxValue=tonumber(ARGV[2])\nlocal minCount=tonumber(ARGV[3])\nlocal initCount=tonumber(ARGV[4])\nlocal expansionStep=tonumber(ARGV[5])\nlocal incrStep=tonumber(ARGV[6])\nlocal len=redis.call('llen',key)\nlocal isInit=true\nlocal loop=initCount\nlocal nextValue=initValue\nif len>minCount\nthen\nreturn redis.call('lpop',key)\nend\nif len>0\nthen\nisInit=false\nloop=len+expansionStep\nnextValue=tonumber(redis.call('rpop',key))\nend\nwhile(len<loop)\ndo\nif nextValue>maxValue\nthen\nnextValue=initValue\nend\nredis.call('rpush',key,nextValue)\nnextValue=nextValue+incrStep\nlen=len+1\nend\nif isInit\nthen\nreturn 'success'\nend\nreturn redis.call('lpop',key)";
}
