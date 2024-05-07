package com.example.spring.id;

/**
 * @author chenghui
 * @date 2024/5/5 17:19
 */

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IDFactory {
    // 变量生成器
    @Autowired(required = false)
    private Map<String, VariableGenerator> variableGeneratorMap;
    // 字段初始化生成器
    @Autowired(required = false)
    private Map<String, InitFieldGenerator> initFieldGeneratorMap;

    // 构造函数，参数是生成规则
    public IDFactory(Map<String, IDExpressionProperties.SerialIdConfig> expressionMap) {
        this.expressionMap = expressionMap;
    }

    // 实例化后执行
    public void init() {
        // 如果没有规则表达式，那么直接就结束
        if (CollectionUtils.isEmpty(this.expressionMap)) {
            return;
        }
        for (Map.Entry<String, IDExpressionProperties.SerialIdConfig> e :
                this.expressionMap.entrySet()) {
            String key = e.getKey();
            // 规则表达式
            IDExpressionProperties.SerialIdConfig config = e.getValue();
            // 初始化字段参数
            String[] initFields = config.getInitFields();
            // 如果没有初始化字段生成器，直接结束
            if (CollectionUtils.isEmpty(initFieldGeneratorMap)) {
                return;
            }
            // 根据初始化规则，执行初始化操作
            for (String initField : initFields) {
                String fieldName = initField;
                // 获取初始化字段名称
                if (StringUtils.contains(initField, VariableGenerator.COLON)) {
                    fieldName = StringUtils.substringBefore(initField, VariableGenerator.COLON);
                }
                // 根据字段名称获取对应的初始化生成器的Bean实例
                InitFieldGenerator initFieldGenerator =
                        initFieldGeneratorMap.get(
                                fieldName + InitFieldGenerator.INIT_FIELD_GENERATOR);
                if (Objects.nonNull(initFieldGenerator)) {
                    // 执行字段初始化操作
                    initFieldGenerator.generator(key, initField);
                }
            }
        }
    }

    /**
     * 表达式
     *
     * <p>pid:expression格式
     */
    private Map<String, IDExpressionProperties.SerialIdConfig> expressionMap;

    /**
     * 根据指定的key规则生成id
     *
     * @param key
     * @return
     */
    public String get(String key) {
        // key为空直接抛异常
        if (StringUtils.isBlank(key)) {
            throw new IllegalArgumentException("无效的参数值：" + key);
        }
        // 获取规则表达式
        IDExpressionProperties.SerialIdConfig serialIdConfig = expressionMap.get(key);
        // 表达式字符串
        String expressionString = serialIdConfig.getExp();
        // 为空直接抛异常
        if (StringUtils.isBlank(expressionString)) {
            throw new IllegalArgumentException("没有找到对应的表达式");
        }
        // 解析指定的表达式
        Expression expression = parse(key, expressionString);
        // 匹配得出最终结果
        return matchExpression(expression);
    }

    // 生成器名称后缀
    private static final String VARIABLE_GENERATOR = "VariableGenerator";

    // 循环遍历表达式中所有的自定义变量，获取指定Bean实例，执行目标方法后得出最终ID
    private String matchExpression(Expression expression) {
        // 获取变量列表，例如pid，yearMonthDayHms等
        List<ExpressionElement> elements = expression.getElements();
        // 如果没有任何变量，那么直接返回原表达式，说明表达式是一个常量
        if (CollectionUtils.isEmpty(elements)) {
            return expression.getExpression();
        }
        // 获取原表达式，用来替换变量，生成最终的ID
        String expressionString = expression.getExpression();
        // 循环遍历变量列表
        for (ExpressionElement e : elements) {
            // 拼接Bean的名称
            String beanName = e.getVariableName() + VARIABLE_GENERATOR;
            // 从map中取出指定的Bean
            VariableGenerator variableGenerator = variableGeneratorMap.get(beanName);
            // 如果没有取到，那么直接忽略，说明没有创建该表达式对应的生成器
            if (Objects.isNull(variableGenerator)) {
                continue;
            }
            // 调用目标方法生成字符串
            String variableValue = variableGenerator.andThen(e, expression);
            // 如果不为空，就替换掉原表达式中的变量；就是用具体生成的值替换变量表达式
            // “$(pid)$(yearMonthDayHms)$(id:6:0)”会被替换成“TEST$(yearMonthDayHms)$(id:6:0)”
            if (StringUtils.isNotBlank(variableValue)) {
                expressionString =
                        StringUtils.replace(expressionString, e.getOriginString(), variableValue);
            }
        }
        // 返回最终结果
        return expressionString;
    }

    // 正则表达式，用来解析$(pid)$(yearMonthDayHms)$(id:6:0)表达式
    private static final Pattern EXPRESSION_PATTERN = Pattern.compile("\\$\\((.+?)\\)");

    private static final Map<String, Expression> EXPRESSION_MAP = Maps.newConcurrentMap();

    /**
     * 解析$(pid)$(yearMonthDayHms)$(id:6:0)
     *
     * @param expressionString
     * @return
     */
    private Expression parse(String key, String expressionString) {
        // 检查一下缓存中是否有解析号的表达式
        Expression expression = EXPRESSION_MAP.get(key);
        // 缓存不为空的话，直接返回
        if (Objects.nonNull(expression)) {
            return expression;
        }
        // 否则，直接解析
        synchronized (EXPRESSION_MAP) {
            // 双重检查，避免重复解析
            expression = EXPRESSION_MAP.get(key);
            if (Objects.nonNull(expression)) {
                return expression;
            }
            // 生成表达式对象
            expression = new Expression();
            expression.setKey(key);
            expression.setExpression(expressionString);
            List<ExpressionElement> expressionElements = Lists.newArrayList();
            Matcher matcher = EXPRESSION_PATTERN.matcher(expressionString);
            while (matcher.find()) {
                // 正则表达式，找出$()变量表达式，类似id:6:0
                String expressionVariable = matcher.group(1);
                // 表达式切割，分离出冒号分隔的参数
                String[] expressionVariables =
                        StringUtils.splitByWholeSeparatorPreserveAllTokens(
                                expressionVariable, VariableGenerator.COLON);
                ExpressionElement element = new ExpressionElement();
                // 变量名称id
                element.setVariableName(expressionVariables[0]);
                // 原生表达式$(id:6:0)，便于后面直接替换
                element.setOriginString(matcher.group());
                if (expressionVariables.length > 1) {
                    // 获取填充的最终长度
                    element.setCount(CastUtil.castInt(expressionVariables[1]));
                }
                if (expressionVariables.length > 2) {
                    // 获取填充值
                    element.setFillStringValue(expressionVariables[2]);
                }
                expressionElements.add(element);
            }
            expression.setElements(expressionElements);
            // 放入本地缓存
            EXPRESSION_MAP.put(key, expression);
        }
        // 返回解析出来的表达式
        return expression;
    }
}
