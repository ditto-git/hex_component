package com.ditto.hex_component.hex_import.annotation;

import java.lang.annotation.*;

/**
 * 自定义表格头注解，用于标记字段对应的表头信息
 */
@Target({ElementType.FIELD,ElementType.TYPE})  // 注解可以作用在字段或方法上
@Retention(RetentionPolicy.RUNTIME)              // 注解在运行时保留，可以通过反射获取
@Documented                                      // 注解会被包含在JavaDoc中
public @interface HEX_Template {
    /**
     * 单元格表头名称
     * @return 表头名称
     */
    String cellHead() default "";

    /**
     * 表头内容描述
     * @return 表头内容
     */
    String headContent() default "";

    /**
     * 列
     * @return 所在列
     */
    int cellIndex() default 0;

    /**
     * 初始行
     * @return 初始行
     */
    int cellStartRow() default 0;


}
