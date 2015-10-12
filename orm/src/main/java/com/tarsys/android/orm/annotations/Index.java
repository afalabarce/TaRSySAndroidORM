package com.tarsys.android.orm.annotations;

import com.tarsys.android.orm.enums.OrderCriteria;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by tarsys on 9/10/15.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Index {
    String IndexName() default "";
    String[] IndexFields() default "";
    boolean IsUniqueIndex() default false;
    String Collation() default "";
    OrderCriteria Order() default OrderCriteria.Asc;
}
