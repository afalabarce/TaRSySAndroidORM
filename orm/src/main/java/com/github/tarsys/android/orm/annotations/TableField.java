package com.github.tarsys.android.orm.annotations;

import com.github.tarsys.android.orm.enums.DBDataType;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by tarsys on 9/10/15.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface TableField {
    String FieldName() default "";
    String Description() default "";
    DBDataType DataType() default DBDataType.None;
    int DataTypeLength() default 0;
    Class<?> EntityClass() default String.class;
    boolean PrimaryKey() default false;
    String ForeignKeyName() default "";
    String ForeignKeyTableName() default "";
    String ForeignKeyFieldName() default "";
    boolean NotNull() default false;
    String DefaultValue() default "";
    boolean CascadeDelete() default false;
    boolean AutoIncrement() default false;
}
