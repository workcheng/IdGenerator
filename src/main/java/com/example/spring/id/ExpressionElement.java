package com.example.spring.id;

/**
 * @author chenghui
 * @date 2024/5/5 17:21
 */

import lombok.Data;
import org.apache.commons.lang3.StringUtils;

/**
 * @author zouwei
 * @className ExpressionElement
 * @date: 2021/2/18 下午2:56
 * @description: 解析${id:6:0}这种类型的标记
 */
@Data
public class ExpressionElement {
    // 原生变量表达式
    private String originString;
    // 变量名称
    private String variableName;
    // 总长度
    private int count;
    // 填充值，默认是空字符
    private String fillStringValue = StringUtils.SPACE;
}
