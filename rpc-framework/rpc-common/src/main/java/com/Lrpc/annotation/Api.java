package com.Lrpc.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)// 在类上使用
@Retention(RetentionPolicy.RUNTIME)// 在运行时使用
public @interface Api {
}
