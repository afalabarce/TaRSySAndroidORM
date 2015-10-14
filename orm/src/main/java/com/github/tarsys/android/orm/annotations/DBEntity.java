package com.github.tarsys.android.orm.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by tarsys on 9/10/15.
 */
@Target(ElementType.TYPE) @Retention(RetentionPolicy.RUNTIME)
public @interface DBEntity
{
    String TableName() default "";
    String Description() default "";
}
