package com.example.spring.id;

/**
 * @author chenghui
 * @date 2024/5/5 17:20
 */
import lombok.Data;

import java.util.List;

/**
 * @className Expression
 * @date: 2021/2/18 下午2:53
 * @description: 解析$(pid)$(year)$(month)$(day)$(id:6:0)这种类型的表达式
 */
@Data
public class Expression {
    /** pid */
    private String key;
    /** 表达式 */
    private String expression;
    /** 解析结果 */
    private List<ExpressionElement> elements;
}
