package com.example.spring.id;

/**
 * @author chenghui
 * @date 2024/5/5 17:13
 */

import org.apache.commons.lang3.StringUtils;

/**
 * 变量生成器
 * @className VariableGenerator
 * @date: 2021/2/18 下午2:53
 * @description:
 */
public abstract class VariableGenerator {

    public static final String COLON = ":";
    /**
     * apply是生成目标字符串的方法
     */
    protected abstract String apply(ExpressionElement e, Expression expression);

    /**
     * apply的后置处理方法，默认处理字符串不足的情况下，补足对应的填充数据
     */
    public String andThen(ExpressionElement e, Expression expression) {
        String variableValue = apply(e, expression);
        int count = e.getCount();
        String fillStringValue = e.getFillStringValue();
        if (StringUtils.isNotBlank(variableValue)) {
            if (count > 0) {
                variableValue = StringUtils.leftPad(variableValue, count, fillStringValue);
            } else {
                variableValue =
                        StringUtils.rightPad(variableValue, Math.abs(count), fillStringValue);
            }
        }
        return variableValue;
    }
}
