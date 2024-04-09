package com.original.flowcraft.annotation;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
//@Component
//@Scope("prototype")//每次注入都是新的实例
public @interface SeleniumTest {
//    @AliasFor(
//            annotation = Component.class
//    )
    String value() default "";
}
