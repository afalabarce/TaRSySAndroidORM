package com.github.tarsys.android.orm.base;

import java.util.List;

/**
 * Created by tarsys on 9/10/15.
 */
public abstract class TarsysEntity {

    public static <T extends TarsysEntity> T findByPrimaryKey (Class<T> classType, long id){
        T returnedValue = null;

        return returnedValue;
    }

    public boolean delete(){
        boolean returnValue = false;

        return returnValue;
    }

    public boolean save(){
        boolean returnValue = false;

        return returnValue;
    }

    public static <T extends TarsysEntity> boolean saveAll(Class<T> classType, List<T> entities){
        boolean returnValue = false;

        return returnValue;
    }
}
