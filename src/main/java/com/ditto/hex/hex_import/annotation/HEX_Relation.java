package com.ditto.hex.hex_import.annotation;

import java.lang.annotation.*;

/**
 * 自定义表格头注解，用于标记字段对应的表头信息
 */
@Target(ElementType.FIELD)  // 注解可以作用在字段或方法上
@Retention(RetentionPolicy.RUNTIME)              // 注解在运行时保留，可以通过反射获取
@Documented                                      // 注解会被包含在JavaDoc中
public @interface HEX_Relation {
    public static String RELATION_TOP="RELATIONTOP";

    /**
     * 单元格表头名称
     * @return 表头名称
     */
    String property() default "";

    /**
     * 表头内容描述
     * @return 表头内容
     */
    String parentField() default RELATION_TOP;

}
