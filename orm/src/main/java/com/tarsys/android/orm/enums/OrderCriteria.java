package com.tarsys.android.orm.enums;

/**
 * Created by tarsys on 9/10/15.
 */
public enum OrderCriteria
{
    Asc
            {
                @Override
                public String toString() {
                    return "ASC";
                }
            },
    Desc
            {
                @Override
                public String toString() {
                    return "DESC";
                }
            }
}
