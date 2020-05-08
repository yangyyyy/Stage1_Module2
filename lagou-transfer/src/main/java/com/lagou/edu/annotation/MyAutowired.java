package com.lagou.edu.annotation;

import java.lang.annotation.*;
//指定注解作用范围
@Target({ElementType.CONSTRUCTOR, ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME) //指定注解生命周期
@Documented
public @interface MyAutowired {
    boolean required() default true;
}
