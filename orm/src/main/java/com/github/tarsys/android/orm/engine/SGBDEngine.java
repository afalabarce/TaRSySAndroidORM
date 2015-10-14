package com.github.tarsys.android.orm.engine;

import android.content.ContentValues;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;

import com.github.tarsys.android.orm.annotations.DBEntity;
import com.github.tarsys.android.orm.annotations.Index;
import com.github.tarsys.android.orm.annotations.Indexes;
import com.github.tarsys.android.orm.annotations.TableField;
import com.github.tarsys.android.orm.base.PrimaryFilter;
import com.github.tarsys.android.orm.dataobjects.DataColumn;
import com.github.tarsys.android.orm.dataobjects.DataRow;
import com.github.tarsys.android.orm.dataobjects.DataSource;
import com.github.tarsys.android.orm.enums.DBDataType;
import com.github.tarsys.android.orm.sqlite.SQLiteSupport;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import dalvik.system.DexClassLoader;
import dalvik.system.DexFile;
import dalvik.system.PathClassLoader;

/**
 * Created by tarsys on 9/10/15.
 */
public class SGBDEngine {
    public static String SQLiteDatabasePath;

    public static boolean Initialize(Context context){
        boolean returnValue = false;

        boolean isExternalStorage = false;
        String databaseName = "";
        String databaseDirectory = "";
        ArrayList<String> entityContainerPackages = new ArrayList<>();

        try {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
            ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);

            int savedAppVersion = sharedPreferences.getInt("AppVersion", 0),
                actualAppVersion = packageInfo.versionCode;
            isExternalStorage = appInfo.metaData.getBoolean("IS_EXTERNALSTORAGE", false);
            databaseDirectory = appInfo.metaData.getString("DATABASE_DIRECTORY", "");
            databaseName = appInfo.metaData.getString("DATABASE_NAME", context.getPackageName() + ".db");
            String containers = appInfo.metaData.getString("ENTITY_PACKAGES", "").replace(" ","");
            if (isExternalStorage){
                if (!new File(Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + databaseDirectory).exists())
                {
                    if (!new File(Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + databaseDirectory).mkdirs()){
                        throw new IOException(Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + databaseDirectory + " NOT CREATED!");
                    }
                }
            }

            SGBDEngine.SQLiteDatabasePath = (isExternalStorage ? Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + databaseDirectory : "") + File.separator + databaseName;

            if (!new File(SQLiteDatabasePath).exists())
                savedAppVersion = 0;

            if (!containers.isEmpty()) entityContainerPackages.addAll(Arrays.asList(containers.split(",")));

            if (!entityContainerPackages.isEmpty()) {

                if (actualAppVersion != savedAppVersion) {
                    ArrayList<String> sqlCreacion = new ArrayList<>();

                    for (String entityContainer : entityContainerPackages) {
                        ArrayList<DBTable> bdCommands = SGBDEngine.createDatabaseModel(context, entityContainer);
                        sqlCreacion.addAll(SGBDEngine.createSqlQuerys(bdCommands));
                    }
                    for(String sql:sqlCreacion)
                        SGBDEngine.SQLiteDataBase(false).execSQL(sql);



                    sharedPreferences.edit().putInt("AppVersion", actualAppVersion).commit();
                }
            }

            returnValue = true;
        }catch (Exception ex){
            returnValue = false;
        }
        return returnValue;
    }

    public static SQLiteDatabase SQLiteDataBase(boolean readOnly){
        SQLiteDatabase returnalue = null;

        if (SGBDEngine.SQLiteDatabasePath != null && !SGBDEngine.SQLiteDatabasePath.isEmpty()){
            int openMode = readOnly? SQLiteDatabase.OPEN_READONLY : SQLiteDatabase.OPEN_READWRITE;
            try{
                returnalue = SQLiteDatabase.openDatabase(SQLiteDatabasePath, null, SQLiteDatabase.CREATE_IF_NECESSARY | openMode);
            }catch(Exception ex){
                Log.e(SGBDEngine.class.toString(), "Exception opening database " + SGBDEngine.SQLiteDatabasePath + ":\n" + ex.toString());
                returnalue = null;
            }
        }else{
            Log.e(SGBDEngine.class.toString(), "Database Path not stablished");
        }

        return returnalue;
    }


    //region Protected Methods

    //region Methods for Data Object Model creation

    protected static String nameSetMethod(Method getMethod){
        String retorno = getMethod.getName();

        if (retorno.startsWith("get") || (retorno.startsWith("is") && (getMethod.getReturnType() == Boolean.class || getMethod.getReturnType() == boolean.class ))){
            retorno = retorno.replaceFirst("get|is", "set");
        }

        return retorno;
    }
    
    protected static ArrayList<DBTable> createDBTableEntity(Class<?> tipo){
        ArrayList<DBTable> retorno = new ArrayList<>();
        DBEntity tablaBD = tipo.getAnnotation(DBEntity.class);

        if (tablaBD != null){
            // si tenemos clase y tiene el decorado DBEntity
            DBTable tabla = new DBTable();
            tabla.Table = tablaBD;
            tabla.Indexes = new ArrayList<Index>();
            tabla.Fields = new ArrayList<TableField>();
            Method[]metodos = tipo.getMethods();
            for(Method metodo : metodos){
                TableField campoTabla = metodo.getAnnotation(TableField.class);
                if (campoTabla != null){
                    if (campoTabla.DataType() == DBDataType.EntityDataType || campoTabla.DataType() == DBDataType.EntityListDataType){
                        // Si el tipo de dato es de tipo entidad o lista de entidades, debemos crear la tabla correspondiente a la entidad
                        // ANTES que la que está en curso... esto se hará de forma recursiva...
                        Class<?> tipoCampo = campoTabla.DataType() == DBDataType.EntityDataType ? metodo.getReturnType(): metodo.getGenericReturnType().getClass();

                        if (tipoCampo != null){
                            retorno.addAll(SGBDEngine.createDBTableEntity(tipoCampo));
                        }
                    }
                    tabla.Fields.add(campoTabla);
                }
            }
            Indexes indices = tipo.getAnnotation(Indexes.class);
            if (indices != null && indices.value() != null){
                tabla.Indexes.addAll(Arrays.asList(indices.value()));
            }
            retorno.add(tabla);
        }

        return retorno;
    }

    protected static ArrayList<DBTable> createDatabaseModel(Context context, String packageName){
        ArrayList<DBTable> returnValue = new ArrayList<DBTable>();
        HashMap<String, DBTable> entities = new HashMap<String, DBTable>();
        try{

            String apkName = context.getPackageCodePath();
            PathClassLoader classLoader2 = new PathClassLoader(apkName, Thread.currentThread().getContextClassLoader());
            DexClassLoader cLoader = new DexClassLoader(apkName, new ContextWrapper(context).getCacheDir().getAbsolutePath(), null, context.getClassLoader());
            DexFile df = new DexFile(apkName);
            for (Enumeration<String> iter = df.entries(); iter.hasMoreElements();) {
                String s = iter.nextElement();

                if (s.startsWith(packageName)){

                    Class<?> classTable = cLoader.loadClass(s);
                    DBEntity databaseHeader = classTable.getAnnotation(DBEntity.class);

                    if (databaseHeader != null){
                        ArrayList<DBTable> tablas = SGBDEngine.createDBTableEntity(classTable);
                        for(DBTable tbl : tablas){
                            if (!entities.containsKey(tbl.Table.TableName())) entities.put(tbl.Table.TableName(), tbl);
                        }
                    }
                }
            }
            for(String key : entities.keySet()){
                returnValue.add(entities.get(key));
            }
        }catch(IOException ex){
            if (ex != null){

            }
        } catch (ClassNotFoundException ex)
        {
            if (ex != null){

            }
        }

        return returnValue;
    }

    protected static ArrayList<String> createSqlQuerys(ArrayList<DBTable> dbTableCommands){
        ArrayList<String> returnValue = new ArrayList<String>();
        SQLiteDatabase db = SGBDEngine.SQLiteDataBase(true);
        if (db != null){
            for(DBTable dbTable : dbTableCommands){
                returnValue.addAll(dbTable.sqlCreationQuerys(db));
            }
            if (db.isOpen()) db.close();
        }

        return returnValue;
    }

    //endregion

    //region Methods for getting information of entity clases

    protected static <T>ArrayList<Object> entityReport(T entity, boolean includeWithoutCascadeDelete){
        ArrayList<Object> returnValue = new ArrayList<Object>();

        if (SGBDEngine.entityWithEntities(entity.getClass())){
            // Si la entidad tiene entidades, recargaremos la entidad, para garantizarnos que tenemos el objeto completo...
            for(Method m : entity.getClass().getMethods()){
                try{
                    TableField campoTabla = m.getAnnotation(TableField.class);
                    if (campoTabla == null) continue;
                    if (campoTabla.DataType() != DBDataType.EntityDataType && campoTabla.DataType() != DBDataType.EntityListDataType) continue;
                    if (!includeWithoutCascadeDelete && !campoTabla.CascadeDelete()) continue;
                    Object e = m.invoke(entity);
                    if (campoTabla.DataType() == DBDataType.EntityDataType) {
                        if (e != null && e.getClass().getAnnotation(DBEntity.class) != null) returnValue.add(e);
                    }else if (campoTabla.DataType() == DBDataType.EntityListDataType && (e.getClass().isArray() || e.getClass() == ArrayList.class)){
                        returnValue.addAll((ArrayList<Object>) e);
                    }
                }catch(IllegalAccessException ex){
                    returnValue.clear();
                } catch (IllegalArgumentException ex)
                {
                    returnValue.clear();
                } catch (InvocationTargetException ex)
                {
                    returnValue.clear();
                }
            }
        }

        return returnValue;
    }

    protected static boolean entityWithEntities(Class<?> classValue){
        boolean retorno = false;

        if (classValue.getAnnotation(DBEntity.class) != null){
            for(Method m : classValue.getMethods()){
                TableField campo = m.getAnnotation(TableField.class);
                if (retorno = (campo != null && (campo.DataType() == DBDataType.EntityDataType || campo.DataType() == DBDataType.EntityListDataType))) break;
            }
        }
        return retorno;
    }

    protected static ArrayList<String> getPrimaryKeyFieldNames(Class<?> clase){
        ArrayList<String> retorno = new ArrayList<String>();

        if (clase.getAnnotation(DBEntity.class) != null){
            for (Method m: clase.getMethods()){
                TableField cmp;
                if ((cmp = m.getAnnotation(TableField.class)) != null && cmp.PrimaryKey()){
                    if (cmp.DataType() == DBDataType.EntityDataType || cmp.DataType() == DBDataType.EntityListDataType) continue;

                    retorno.add((cmp.DataType() != DBDataType.DateDataType ? "" : SQLiteSupport.PREFIX_DATE_FIELD) + cmp.FieldName());
                }
            }
        }

        return retorno;
    }

    protected static HashMap<TableField,Method> primaryKeyMethods(Class<?> clase, boolean metodosInsercion){
        HashMap<TableField,Method> retorno = new HashMap<TableField,Method>();
        if (clase.getAnnotation(DBEntity.class) != null){
            for(Method m : clase.getMethods()){
                TableField campo = m.getAnnotation(TableField.class);
                if (campo != null && campo.PrimaryKey()){
                    if (!metodosInsercion){
                        retorno.put(campo,m);
                    }else{
                        try{ retorno.put(campo,clase.getMethod(SGBDEngine.nameSetMethod(m), m.getReturnType())); }catch(NoSuchMethodException ex){} catch (SecurityException ex)
                        {
                        }
                    }

                }
            }
        }
        return retorno;
    }

    protected static <T>ContentValues foreignKeysContentValues(T foreignKeyEntity){
        ContentValues retorno = null;
        DBEntity tEntidad = null;
        if ((tEntidad = foreignKeyEntity.getClass().getAnnotation(DBEntity.class)) != null){
            retorno = new ContentValues();
            for(Method m : foreignKeyEntity.getClass().getMethods()){
                try{
                    TableField tCampo = m.getAnnotation(TableField.class);
                    if (tCampo != null && tCampo.PrimaryKey() && tCampo.DataType() != DBDataType.EntityDataType && tCampo.DataType() != DBDataType.EntityListDataType){
                        String nCampo = tEntidad.TableName() + "_" + (tCampo.DataType() == DBDataType.DateDataType ? SQLiteSupport.PREFIX_DATE_FIELD : "") + tCampo.FieldName();
                        Object valorCampo = m.invoke(foreignKeyEntity);
                        switch (tCampo.DataType()){
                            case StringDataType:
                            case TextDataType:
                                retorno.put(nCampo, valorCampo != null ? String.valueOf(valorCampo) : tCampo.DefaultValue());
                                break;
                            case RealDataType:
                                retorno.put(nCampo, valorCampo != null ? ((Double)valorCampo) : Double.parseDouble(tCampo.DefaultValue()));
                                break;
                            case IntegerDataType:
                                retorno.put(nCampo, valorCampo != null ? ((Long)valorCampo) : Long.parseLong(tCampo.DefaultValue()));
                                break;
                            case DateDataType:
                                retorno.put(nCampo, valorCampo != null ? ((Date)valorCampo).getTime() : 0L);
                                break;
                        }
                    }
                }catch (IllegalAccessException ex){
                    retorno = null;
                } catch (IllegalArgumentException ex)
                {
                    retorno = null;
                } catch (InvocationTargetException ex)
                {
                    retorno = null;
                }
            }
        }

        return retorno;
    }

    protected static ArrayList<String> getEntityListRelationTable(Class<?> classValue, boolean addWithoutCascadeDelete){
        ArrayList<String> retorno = new ArrayList<String>();

        DBEntity eBd;
        if ((eBd = classValue.getAnnotation(DBEntity.class)) != null){
            for(Method m : classValue.getMethods()){
                TableField campo = m.getAnnotation(TableField.class);
                DBEntity eBdFk = null;

                if (campo != null && campo.DataType() == DBDataType.EntityListDataType && (eBdFk = campo.EntityClass().getAnnotation(DBEntity.class)) != null){
                    if (!addWithoutCascadeDelete && !campo.CascadeDelete()) continue;
                    retorno.add("rel_" + eBd.TableName() + "_" + eBdFk.TableName());
                }
            }

        }
        return retorno;
    }

    protected static PrimaryFilter contentValues2PrimaryFilter(ContentValues valores){
        PrimaryFilter retorno = null;
        try {
            if (valores.size() > 0){
                retorno = new PrimaryFilter();

                for (Map.Entry<String,Object> e : valores.valueSet()){
                    retorno.FilterString += retorno.FilterString.trim().equals("")?"":" and ";
                    retorno.FilterString += e.getKey() + "=?";
                    retorno.FilterData.add(String.valueOf(e.getValue()));
                }
            }
        } catch (Exception ex) {
            retorno = null;
        }

        return retorno;
    }

    protected static <T>PrimaryFilter createPrimaryKeyFilter(T entity, boolean forTableRelation){
        PrimaryFilter retorno = null;

        DBEntity datoTabla;
        try{

            if ((datoTabla = entity.getClass().getAnnotation(DBEntity.class)) != null){
                String clausulaWhere = "";
                ArrayList<String> datosWhere = new ArrayList<String>();

                for (Method metodo : entity.getClass().getMethods()){
                    TableField campoTabla = metodo.getAnnotation(TableField.class);
                    if (campoTabla != null){
                        if (campoTabla.PrimaryKey()){
                            if (!clausulaWhere.trim().isEmpty()){
                                clausulaWhere += " and ";
                            }
                            clausulaWhere += (forTableRelation?datoTabla.TableName() + "_":"") + (campoTabla.DataType() == DBDataType.DateDataType ? SQLiteSupport.PREFIX_DATE_FIELD:"") + campoTabla.FieldName() + "=?";
                            Object valorCampo = metodo.invoke(entity);
                            switch (campoTabla.DataType()){
                                case StringDataType:
                                case TextDataType:
                                    datosWhere.add(valorCampo != null ? String.valueOf(valorCampo) : campoTabla.DefaultValue());
                                    break;
                                case RealDataType:
                                    Double valor = valorCampo != null ? ((Double)valorCampo) : Double.parseDouble(campoTabla.DefaultValue());
                                    datosWhere.add(valor.toString().replace(",", "."));
                                    break;
                                case IntegerDataType:
                                    datosWhere.add(valorCampo != null ? valorCampo.toString() : campoTabla.DefaultValue());
                                    break;
                                case DateDataType:
                                    datosWhere.add(((Long)(valorCampo != null ? ((Date)valorCampo).getTime() : 0L)).toString());
                                    break;
                                case EnumDataType:
                                    Class tipoEnum;
                                    try{
                                        if ((tipoEnum = valorCampo.getClass().getDeclaringClass()).isEnum() && tipoEnum.getMethod("value", new Class[]{}) != null){
                                            Integer v = (Integer) tipoEnum.getMethod("value", new Class[]{}).invoke(valorCampo, new Object[]{});
                                            datosWhere.add(v.toString());
                                        }else{
                                            datosWhere.add(campoTabla.DefaultValue());
                                        }
                                    }catch(NoSuchMethodException ex){
                                    } catch (SecurityException ex) {
                                    } catch (IllegalAccessException ex) {
                                    } catch (IllegalArgumentException ex) {
                                    } catch (InvocationTargetException ex) {
                                    }

                                    break;
                                case BooleanDataType:
                                    datosWhere.add(valorCampo != null ? (Boolean)valorCampo?"1":"0" : campoTabla.DefaultValue());
                                    break;
                                case EntityDataType:
                                    PrimaryFilter f = SGBDEngine.createPrimaryKeyFilter(valorCampo, true);
                                    clausulaWhere += " and " + f.FilterString;
                                    datosWhere.addAll(f.FilterData);
                                    break;
                            }
                        }
                    }
                }
                if (datosWhere.size() > 0){
                    retorno = new PrimaryFilter();
                    retorno.FilterString = clausulaWhere;
                    retorno.FilterData = datosWhere;
                }
            }
        }catch(IllegalAccessException ex){
            Log.e("ModeladoDatos", "Error generando filtro de clave primaria");
            retorno = null;
        } catch (IllegalArgumentException ex)
        {
            Log.e("ModeladoDatos", "Error generando filtro de clave primaria");
            retorno = null;
        } catch (SecurityException ex)
        {
            Log.e("ModeladoDatos", "Error generando filtro de clave primaria");
            retorno = null;
        } catch (InvocationTargetException ex)
        {
            Log.e("ModeladoDatos", "Error generando filtro de clave primaria");
            retorno = null;
        }

        return retorno;
    }

    protected static <T>PrimaryFilter createForeignKeyFilter(Class<T> clase, Cursor cursorDatos){
        PrimaryFilter retorno = null;
        DBEntity datoTabla;

        try{

            if ((datoTabla = clase.getAnnotation(DBEntity.class)) != null){
                String clausulaWhere = "";
                ArrayList<String> datosWhere = new ArrayList<String>();

                for (Method metodo : clase.getMethods()){
                    TableField campoTabla = metodo.getAnnotation(TableField.class);
                    if (campoTabla != null){
                        if (campoTabla.PrimaryKey()){
                            String campoEx = (campoTabla.DataType() == DBDataType.DateDataType ? SQLiteSupport.PREFIX_DATE_FIELD : "") + campoTabla.FieldName();
                            String nCampo = datoTabla.TableName().toLowerCase() + "_" + campoEx;
                            if (!clausulaWhere.trim().isEmpty()) clausulaWhere += " and ";
                            clausulaWhere += campoEx + "=?";
                            String valorCampo = "";
                            switch (campoTabla.DataType()){
                                case StringDataType:
                                case TextDataType:
                                    valorCampo = cursorDatos.getString(cursorDatos.getColumnIndex(nCampo));
                                    break;
                                case RealDataType:
                                    Double valor = cursorDatos.getDouble(cursorDatos.getColumnIndex(nCampo));
                                    valorCampo = valor.toString().replace(",", ".");
                                    break;
                                case EnumDataType:
                                case IntegerDataType:
                                case DateDataType:
                                case BooleanDataType:
                                    valorCampo = ((Long)cursorDatos.getLong(cursorDatos.getColumnIndex(nCampo))).toString();
                                    break;
                                case EntityDataType:
                                    break;
                            }
                            if (campoTabla.DataType() != DBDataType.EntityDataType) datosWhere.add(valorCampo);
                        }
                    }
                }
                if (datosWhere.size() > 0){
                    retorno = new PrimaryFilter();
                    retorno.FilterString = clausulaWhere;
                    retorno.FilterData = datosWhere;
                }
            }
        }catch(SecurityException ex){
            Log.e("ModeladoDatos", "Error generando filtro de clave externa");
            retorno = null;
        }

        return retorno;
    }

    protected static <T> T createForeignKeyEntity(Class<T> clase, Cursor cursorDatos){
        T retorno = null;
        DBEntity datoTabla;

        if ((datoTabla = clase.getAnnotation(DBEntity.class)) != null){
            ArrayList<Method> metodosPK = new ArrayList<Method>();
            for (Method metodo : clase.getMethods()){
                TableField campoTabla = metodo.getAnnotation(TableField.class);
                if (campoTabla != null){
                    if (campoTabla.PrimaryKey()){
                        metodosPK.add(metodo);
                    }
                }
            }
            // Si tenemos métodos de clave primaria, entonces, creamos la instancia del objeto e inicializamos su clave...
            if (metodosPK.size() > 0){
                try {
                    retorno = clase.newInstance();
                    for(Method metodo : metodosPK){
                        TableField campoTabla = metodo.getAnnotation(TableField.class);
                        String nCampoCursor = datoTabla.TableName().toLowerCase() + "_" + (campoTabla.DataType() == DBDataType.DateDataType ? SQLiteSupport.PREFIX_DATE_FIELD : "") + campoTabla.FieldName();
                        Object valorCampo = null;
                        switch (campoTabla.DataType()){
                            case StringDataType:
                            case TextDataType:
                                valorCampo = cursorDatos.getString(cursorDatos.getColumnIndex(nCampoCursor));
                                break;
                            case RealDataType:
                                valorCampo = cursorDatos.getDouble(cursorDatos.getColumnIndex(nCampoCursor));
                                break;
                            case IntegerDataType:
                                valorCampo = cursorDatos.getLong(cursorDatos.getColumnIndex(nCampoCursor));
                                break;
                            case DateDataType:
                                long ticks = cursorDatos.getLong(cursorDatos.getColumnIndex(nCampoCursor));
                                Calendar cal = Calendar.getInstance();
                                cal.setTimeInMillis(ticks);
                                valorCampo = cal.getTime();
                                break;
                        }
                        Method mIns = clase.getMethod(SGBDEngine.nameSetMethod(metodo), metodo.getReturnType());
                        if (mIns != null){
                            mIns.invoke(retorno, valorCampo);
                        }
                    }
                } catch (IllegalAccessException ex) {
                    Logger.getLogger(SGBDEngine.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IllegalArgumentException ex)
                {
                    Logger.getLogger(SGBDEngine.class.getName()).log(Level.SEVERE, null, ex);
                } catch (InstantiationException ex)
                {
                    Logger.getLogger(SGBDEngine.class.getName()).log(Level.SEVERE, null, ex);
                } catch (NoSuchMethodException ex)
                {
                    Logger.getLogger(SGBDEngine.class.getName()).log(Level.SEVERE, null, ex);
                } catch (SecurityException ex)
                {
                    Logger.getLogger(SGBDEngine.class.getName()).log(Level.SEVERE, null, ex);
                } catch (InvocationTargetException ex)
                {
                    Logger.getLogger(SGBDEngine.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }

        return retorno;
    }

    //endregion

    protected static <T> T findPrimaryKeyEntity(SQLiteDatabase bd, T primaryKeyEntity, boolean recursiveLoad){
        T returnValue = null;

        DBEntity DBEntity;

        if ((DBEntity = primaryKeyEntity.getClass().getAnnotation(DBEntity.class)) != null){
            String tableName = DBEntity.TableName();

            PrimaryFilter primaryKeyFilter = SGBDEngine.createPrimaryKeyFilter(primaryKeyEntity, false);
            if (primaryKeyFilter != null){

                if (bd != null && bd.isOpen()){
                    Cursor query = bd.query(tableName, null, primaryKeyFilter.FilterString, primaryKeyFilter.FilterData.toArray(new String[primaryKeyFilter.FilterData.size()]), null, null, null);
                    if (query != null){
                        if (query.moveToFirst()){
                            try{
                                returnValue = (T) primaryKeyEntity.getClass().newInstance();
                                for (Method method : primaryKeyEntity.getClass().getMethods()){
                                    TableField campoTabla = method.getAnnotation(TableField.class);
                                    if (campoTabla != null){
                                        Method metodoIns = primaryKeyEntity.getClass().getMethod(SGBDEngine.nameSetMethod(method), method.getReturnType());
                                        if (metodoIns != null){
                                            switch (campoTabla.DataType()){
                                                case StringDataType:
                                                case TextDataType:
                                                {
                                                    metodoIns.invoke(returnValue, query.getString(query.getColumnIndex(campoTabla.FieldName())));
                                                    break;
                                                }
                                                case RealDataType:
                                                {
                                                    metodoIns.invoke(returnValue, query.getDouble(query.getColumnIndex(campoTabla.FieldName())));
                                                    break;
                                                }
                                                case IntegerDataType:
                                                {
                                                    metodoIns.invoke(returnValue, query.getLong(query.getColumnIndex(campoTabla.FieldName())));
                                                    break;
                                                }
                                                case DateDataType:
                                                {
                                                    Calendar cal = Calendar.getInstance();
                                                    cal.setTimeInMillis(query.getLong(query.getColumnIndex(SQLiteSupport.PREFIX_DATE_FIELD + campoTabla.FieldName())));
                                                    metodoIns.invoke(returnValue, cal.getTime());
                                                    break;
                                                }
                                                case EnumDataType:
                                                {
                                                    Class tipoEnum;

                                                    if ((tipoEnum = method.getReturnType()).isEnum() && tipoEnum.getMethod("value", new Class[]{}) != null){
                                                        int v = query.getInt(query.getColumnIndex(campoTabla.FieldName()));
                                                        Object[]valores = tipoEnum.getEnumConstants();
                                                        for (Object vEnum : valores){
                                                            if ((Integer)vEnum.getClass().getMethod("value", new Class[]{}).invoke(vEnum, new Object[]{}) == v){
                                                                metodoIns.invoke(returnValue, method.getReturnType().cast(vEnum));
                                                                break;
                                                            }
                                                        }
                                                    }
                                                    break;
                                                }
                                                case BooleanDataType:
                                                {
                                                    metodoIns.invoke(returnValue, query.getInt(query.getColumnIndex(campoTabla.FieldName())) == 1);
                                                    break;
                                                }
                                                case EntityDataType:
                                                {
                                                    //Comprobamos que realmente es una entidad BD...
                                                    DBEntity tipoClaseEntidad = null;
                                                    if ((tipoClaseEntidad = method.getReturnType().getAnnotation(DBEntity.class)) != null){
                                                        // Preparamos una lectura, en función de los filtros por clave primaria...
                                                        Object entidadClaveExterna = SGBDEngine.createForeignKeyEntity(method.getReturnType(), query);

                                                        metodoIns.invoke(returnValue, SGBDEngine.findPrimaryKeyEntity(bd, entidadClaveExterna, recursiveLoad));
                                                    }
                                                    break;
                                                }
                                                case EntityListDataType:
                                                {
                                                    if (recursiveLoad){
                                                        DBEntity tipoClaseEntidad = campoTabla.EntityClass().getAnnotation(DBEntity.class);
                                                        Class<?> tipoRetorno = method.getReturnType();

                                                        if ((tipoRetorno.isArray() || tipoRetorno == ArrayList.class) &&
                                                                tipoClaseEntidad != null){
                                                            ArrayList<Object> arrayEntidades = new ArrayList<Object>();
                                                            // Si el método devuelve un array, agregaremos todos los elementos que cumplan la clave externa...
                                                            String nTablaRel = "rel_" + tableName.toLowerCase() + "_" + tipoClaseEntidad.TableName().toLowerCase();
                                                            // Preparamos el filtro para buscar en la tabla de relación, buscaremos todos y cada uno de los registros que
                                                            // se correspondan con la clave primaria de esta entidad...
                                                            // Preparamos el filtro para sacar los elementos necesarios de la tabla de relación
                                                            PrimaryFilter filtroRelPrimario = SGBDEngine.createPrimaryKeyFilter(primaryKeyEntity, true);
                                                            if (filtroRelPrimario != null){

                                                                Cursor curNav = bd.query(nTablaRel, null, filtroRelPrimario.FilterString, filtroRelPrimario.FilterData.toArray(new String[filtroRelPrimario.FilterData.size()]), null, null, null);
                                                                if (curNav != null){

                                                                    while (curNav.moveToNext()){
                                                                        Object eClaveExterna = SGBDEngine.createForeignKeyEntity(campoTabla.EntityClass(), curNav);
                                                                        if (eClaveExterna != null){
                                                                            arrayEntidades.add(SGBDEngine.findPrimaryKeyEntity(bd, eClaveExterna, true));
                                                                        }
                                                                    }

                                                                }
                                                                metodoIns.invoke(returnValue, arrayEntidades);
                                                            }
                                                        }
                                                    }
                                                    break;
                                                }
                                            }
                                        }
                                    }
                                }
                            }catch (IllegalAccessException ex){
                                returnValue = null;
                            } catch (IllegalArgumentException ex)
                            {
                                returnValue = null;
                            } catch (InstantiationException ex)
                            {
                                returnValue = null;
                            } catch (NoSuchMethodException ex)
                            {
                                returnValue = null;
                            } catch (SecurityException ex)
                            {
                                returnValue = null;
                            } catch (InvocationTargetException ex)
                            {
                                returnValue = null;
                            }
                        }
                    }
                }

            }
        }

        return returnValue;
    }

    //endregion

    //region Custom Query Methods

    public static <T> ArrayList<T> filterQuery(Class<T> entityClass, String whereFilter, String[] whereValues, String orderBy){
        return SGBDEngine.filterQuery(entityClass, whereFilter, whereValues, orderBy, true);
    }

    public static <T> ArrayList<T> filterQuery(Class<T> entityClass, String whereFilter, String[] whereValues, String orderBy, boolean recursiveLoad){
        ArrayList<T> retorno = new ArrayList<T>();
        try{
            DBEntity DBEntity;
            SQLiteDatabase bd = SGBDEngine.SQLiteDataBase(true);
            if ((DBEntity = entityClass.getAnnotation(DBEntity.class)) != null){
                String nombreTabla = DBEntity.TableName();

                if (bd != null && bd.isOpen()){
                    Cursor query = bd.query(nombreTabla, null, whereFilter, whereValues, null, null, orderBy);
                    if (query != null){
                        while (query.moveToNext()){
                            try{
                                T pkEntity = entityClass.newInstance();
                                HashMap<TableField,Method> mPKs = SGBDEngine.primaryKeyMethods(entityClass, true);
                                for(TableField field : mPKs.keySet()){
                                    switch (field.DataType()){
                                        case StringDataType:
                                        case TextDataType:
                                            mPKs.get(field).invoke(pkEntity, query.getString(query.getColumnIndex(field.FieldName())));
                                            break;
                                        case IntegerDataType:
                                            mPKs.get(field).invoke(pkEntity, query.getLong(query.getColumnIndex(field.FieldName())));
                                            break;
                                        case RealDataType:
                                            mPKs.get(field).invoke(pkEntity, query.getDouble(query.getColumnIndex(field.FieldName())));
                                            break;
                                        case DateDataType:
                                            Calendar cal = Calendar.getInstance();
                                            cal.setTimeInMillis(query.getLong(query.getColumnIndex(field.FieldName())));
                                            mPKs.get(field).invoke(pkEntity, cal.getTime());
                                            break;
                                    }
                                }
                                T entidad = SGBDEngine.findPrimaryKeyEntity(bd, pkEntity, recursiveLoad);
                                if (entidad != null) retorno.add(entidad);
                            }catch(IllegalAccessException ex){

                            } catch (IllegalArgumentException ex)
                            {
                            } catch (InstantiationException ex)
                            {
                            } catch (InvocationTargetException ex)
                            {
                            }
                        }
                    }
                }
            }
            if (bd.isOpen()) bd.close();
        }catch(Exception ex){
            retorno = new ArrayList<T>();
        }
        return retorno;
    }

    public static Cursor filterQueryAsCursor(boolean readOnly, String tableName, String[] columns, String whereFilter, String[] whereValues, String groupBy, String having, String orderBy, String limit){
        SQLiteDatabase bd = SGBDEngine.SQLiteDataBase(readOnly);
        return bd == null? null : bd.query(tableName, columns, whereFilter, whereValues, groupBy, having, orderBy,limit);

    }

    public static Cursor rawSqlQuery(String consultaSql){
        Cursor retorno = null;

        try{
            SQLiteDatabase db = SGBDEngine.SQLiteDataBase(true);
            if (db.isOpen()){
                retorno = db.rawQuery(consultaSql, null);
            }
        }catch(Exception ex){
            retorno = null;
        }
        return retorno;
    }

    public static DataSource rawSqlQueryToDataSource(String sqlQuery)
    {
        return SGBDEngine.rawSqlQueryToDataSource(sqlQuery, false);
    }

    public static DataSource rawSqlQueryToDataSource(String sqlQuery, boolean forceOnlyDate){
        DataSource retorno = null;

        Cursor curSql = SGBDEngine.rawSqlQuery(sqlQuery);
        if (curSql != null){
            // Como tenemos cursor... preparamos el origen de datos...
            retorno = new DataSource();
            curSql.moveToFirst();

            for (String nombreColumna : curSql.getColumnNames()){
                Class tipoDatoColumna = null;
                int idxColumna = curSql.getColumnIndex(nombreColumna);
                if (idxColumna < 0){
                    // No hemos encontrado la columna con su índice tal cual, la buscamos... iterando...
                    for (int idx = 0; idx < curSql.getColumnNames().length; idx++){
                        if (curSql.getColumnNames()[idx].equals(nombreColumna)){
                            idxColumna = idx;
                            break;
                        }
                    }
                }
                try{
                    long valor = curSql.getLong(idxColumna);
                    double valorDbl = curSql.getDouble(idxColumna);
                    if (nombreColumna.startsWith(SQLiteSupport.PREFIX_DATE_FIELD)) {
                        tipoDatoColumna = Date.class;
                    }
                    else if (valor == 0 && valorDbl != 0)
                        tipoDatoColumna = Double.class;
                    else
                        tipoDatoColumna = Long.class;
                }catch (Exception ex){
                    tipoDatoColumna = null;
                }
                if (tipoDatoColumna == null) {
                    try {
                        double valor = curSql.getDouble(idxColumna);
                        tipoDatoColumna = Double.class;
                    } catch (Exception ex) {
                        tipoDatoColumna = null;
                    }
                }
                if (tipoDatoColumna == null) tipoDatoColumna = String.class;

                DataColumn colDato = new DataColumn(nombreColumna, nombreColumna.replace(SQLiteSupport.PREFIX_DATE_FIELD,""), tipoDatoColumna , true);
                retorno.getColumns().add(colDato);
            }

            curSql.moveToPrevious();
            while (curSql.moveToNext()){
                // Comenzamos la carga de datos...
                DataRow filaDato = new DataRow(retorno);
                for(String nombreColumna : curSql.getColumnNames()){
                    DataColumn columnaDato = retorno.getColumna(nombreColumna);
                    int idxColumna = curSql.getColumnIndex(nombreColumna);
                    if (idxColumna < 0){
                        // No hemos encontrado la columna con su índice tal cual, la buscamos... iterando...
                        for (int idx = 0; idx < curSql.getColumnNames().length; idx++){
                            if (curSql.getColumnNames()[idx].equals(nombreColumna)){
                                idxColumna = idx;
                                break;
                            }
                        }
                    }
                    if (columnaDato != null){
                        if (columnaDato.getColumnDataType() == Date.class){
                            // Es una fecha...
                            long ticks = curSql.getLong(idxColumna);
                            Calendar cal = Calendar.getInstance();
                            cal.setTimeInMillis(ticks);
                            Date valorFecha = cal.getTime();
                            if (forceOnlyDate){
                                cal.set(Calendar.SECOND, 0);
                                cal.set(Calendar.MILLISECOND, 0);
                                cal.set(Calendar.MINUTE, 0);
                                cal.set(Calendar.HOUR_OF_DAY, 0);
                                valorFecha = cal.getTime();
                            }

                            filaDato.put(nombreColumna, valorFecha);
                        }else if (columnaDato.getColumnDataType() == Double.class){
                            // Valor Double...
                            Double valor = curSql.getDouble(idxColumna);
                            filaDato.put(nombreColumna,valor);
                        }
                        else if (columnaDato.getColumnDataType() == Long.class){
                            Long valor = curSql.getLong(idxColumna);
                            filaDato.put(nombreColumna,valor);
                        }else if (columnaDato.getColumnDataType() == String.class){
                            String valor = curSql.getString(idxColumna);
                            filaDato.put(nombreColumna,valor);
                        }
                    }
                }
                retorno.getRows().add(filaDato);
            }
        }

        return retorno;
    }

    //endregion

    //region CRUD Methods

    public static <T> T findPrimaryKeyEntity(T primaryKeyEntity){
        return SGBDEngine.findPrimaryKeyEntity(SGBDEngine.SQLiteDataBase(true), primaryKeyEntity, true);
    }

    public static <T> T findPrimaryKeyEntity(T primaryKeyEntity, boolean recursiveLoad){
        return SGBDEngine.findPrimaryKeyEntity(SGBDEngine.SQLiteDataBase(true), primaryKeyEntity, recursiveLoad);
    }

    public static <T> boolean deleteEntity(T primaryKeyEntity){
        return SGBDEngine.deleteEntity(SGBDEngine.SQLiteDataBase(false), primaryKeyEntity);
    }

    public static <T> boolean deleteEntity(SQLiteDatabase bd, T primaryKeyEntity){
        boolean retorno = false;

        if (bd.isReadOnly()) {
            Log.e("EliminarEntidadPK", "Base de datos abierta en modo solo lectura, no se puede ejecutar la operación de elimnación");
        }else{
            DBEntity DBEntity;
            if ((DBEntity = primaryKeyEntity.getClass().getAnnotation(DBEntity.class)) != null){
                String nombreTabla = DBEntity.TableName();

                PrimaryFilter filtroPrimario = SGBDEngine.createPrimaryKeyFilter(primaryKeyEntity, false);
                if (filtroPrimario != null){
                    // Si tenemos filtro primario... leeremos la base de la entidad...
                    if (bd.isOpen()){
                        // Si la entidad contiene entidades, debemos leerla por completo, para poder borrar en condiciones...
                        retorno = true;
                        ArrayList<Object> entidadesRelacionadas = SGBDEngine.entityReport(primaryKeyEntity, false);
                        for(Object e : entidadesRelacionadas){
                            retorno = SGBDEngine.deleteEntity(bd, e);
                            if (!retorno) break;
                        }
                        ArrayList<String> tblRelListaEntidad = SGBDEngine.getEntityListRelationTable(primaryKeyEntity.getClass(), false);
                        PrimaryFilter filtroFK = SGBDEngine.createPrimaryKeyFilter(primaryKeyEntity, true);
                        for(String tblRel : tblRelListaEntidad){
                            try{
                                bd.delete(tblRel, filtroFK.FilterString, filtroFK.FilterData.toArray(new String[filtroFK.FilterData.size()]));
                            }catch(Exception ex){}
                        }
                        if (retorno){
                            // Si todo ha ido bien, se borrará la entidad principal
                            try{
                                retorno = bd.delete(nombreTabla, filtroPrimario.FilterString, filtroPrimario.FilterData.toArray(new String[filtroPrimario.FilterData.size()])) > 0;
                            }catch (Exception ex){
                                retorno = false;
                            }
                        }
                    }
                }
            }

        }
        return retorno;
    }

    public static <T> boolean saveEntity(T primaryKeyEntity){
        return SGBDEngine.saveEntity(SGBDEngine.SQLiteDataBase(false), primaryKeyEntity, true);
    }

    public static <T> boolean saveEntity(T primaryKeyEntity, boolean forceEntitySave){
        return SGBDEngine.saveEntity(SGBDEngine.SQLiteDataBase(false), primaryKeyEntity, forceEntitySave);
    }

    public static <T> boolean saveEntity(SQLiteDatabase bd, T primaryKeyEntity, boolean forceEntitySave){
        boolean retorno = false;

        if (bd.isReadOnly()){
            Log.e("saveEntity", "Base de datos abierta en modo solo lectura, no se puede ejecutar la operación de guardado");
        }else{

            DBEntity DBEntity = null;
            if ((DBEntity = primaryKeyEntity.getClass().getAnnotation(DBEntity.class)) != null){
                String nombreTabla = DBEntity.TableName();

                PrimaryFilter filtroPrimario = SGBDEngine.createPrimaryKeyFilter(primaryKeyEntity, false);
                if (filtroPrimario != null){
                    // Si tenemos filtro primario... leeremos la base de la entidad...
                    if (bd.isOpen()){
                        ContentValues valores = new ContentValues();
                        ArrayList<Object> camposEntidad = new ArrayList<Object>();
                        ArrayList<Object> camposListaEntidad = new ArrayList<Object>();
                        // Agregamos los distintos valores para guardar...
                        boolean error = false;
                        Method autoincrementado = null;
                        ArrayList<TableField> clsListaEntidad = new ArrayList<TableField>();

                        for(Method m : primaryKeyEntity.getClass().getMethods()){
                            TableField campoTabla = m.getAnnotation(TableField.class);
                            if (campoTabla != null && !campoTabla.AutoIncrement()){
                                try{
                                    Object valorCampo = m.invoke(primaryKeyEntity);
                                    switch (campoTabla.DataType()){
                                        case StringDataType:
                                        case TextDataType:
                                            valores.put(campoTabla.FieldName(), valorCampo != null ? String.valueOf(valorCampo) : campoTabla.DefaultValue());
                                            break;
                                        case RealDataType:
                                            valores.put(campoTabla.FieldName(), valorCampo != null ? ((Double)valorCampo) : Double.parseDouble(campoTabla.DefaultValue()));
                                            break;
                                        case IntegerDataType:
                                            valores.put(campoTabla.FieldName(), valorCampo != null ? ((Long)valorCampo) : Long.parseLong(campoTabla.DefaultValue()));
                                            break;
                                        case EnumDataType:
                                            Class tipoEnum;
                                            try{
                                                if ((tipoEnum = valorCampo.getClass().getDeclaringClass()).isEnum() && tipoEnum.getMethod("value", new Class[]{}) != null){
                                                    int v = (Integer) tipoEnum.getMethod("value", new Class[]{}).invoke(valorCampo, new Object[]{});
                                                    valores.put(campoTabla.FieldName(), valorCampo != null ? v : Integer.parseInt(campoTabla.DefaultValue()));
                                                }
                                            }catch(NoSuchMethodException ex){
                                            } catch (SecurityException ex) {
                                            } catch (IllegalAccessException ex) {
                                            } catch (IllegalArgumentException ex) {
                                            } catch (InvocationTargetException ex) {
                                            }
                                            break;
                                        case BooleanDataType:
                                            valores.put(campoTabla.FieldName(), valorCampo != null ? ((Boolean)valorCampo)? 1:0 : Integer.parseInt(campoTabla.DefaultValue()));
                                            break;
                                        case DateDataType:
                                            valores.put(SQLiteSupport.PREFIX_DATE_FIELD + campoTabla.FieldName(), valorCampo != null ? ((Date)valorCampo).getTime() : 0L);
                                            break;
                                        case EntityDataType:
                                            // hay que agregar a los valores los campos de la clave primaria
                                            DBEntity eBdCampoE = null;
                                            if ((eBdCampoE = valorCampo.getClass().getAnnotation(DBEntity.class)) != null) {
                                                camposEntidad.add(valorCampo);
                                                ContentValues v = SGBDEngine.foreignKeysContentValues(valorCampo);
                                                if (v != null) valores.putAll(v);
                                            }
                                            break;
                                        case EntityListDataType:
                                            clsListaEntidad.add(campoTabla);
                                            camposListaEntidad.addAll((ArrayList<Object>)valorCampo);
                                            break;
                                    }
                                }catch(IllegalAccessException ex){
                                    error = true;
                                    break;
                                } catch (IllegalArgumentException ex)
                                {
                                    error = true;
                                    break;
                                } catch (InvocationTargetException ex)
                                {
                                    error = true;
                                    break;
                                }
                            }else if (campoTabla != null){
                                // Como es autoincrementado...
                                autoincrementado = m;
                            }
                        }
                        // Si hay algún error preparando el guardado... salimos estrepitosamente...
                        if (error) return false;
                        retorno = true;
                        // Si estamos aquí, no hay errores, y podremos guardar todo...

                        // Primero, le metemos mano a las entidades que forman parte de claves externas, ya sean 1 a 1 o 1 a muchos...
                        // Primero las entidades relacionadas directamente...
                        if (forceEntitySave){
                            for(Object c : camposEntidad){
                                retorno = SGBDEngine.saveEntity(bd, c, true);
                                if (!retorno) break;
                            }
                        }
                        if (retorno){
                            // Si hemos podido guardar todas las entidades externas relacionadas de forma directa, guardamos la entidad...
                            try{
                                // Intentamos borrar la entidad, si es que previamente existe...
                                bd.delete(nombreTabla, filtroPrimario.FilterString, filtroPrimario.FilterData.toArray(new String[filtroPrimario.FilterData.size()]));
                            }catch (Exception ex){

                            }
                            try{
                                Method metodoIns = null;

                                if (autoincrementado != null){
                                    TableField campoTabla = autoincrementado.getAnnotation(TableField.class);
                                    long id = (Long)autoincrementado.invoke(primaryKeyEntity);
                                    if (id > 0) valores.put(campoTabla.FieldName(), id);
                                    metodoIns = primaryKeyEntity.getClass().getMethod(SGBDEngine.nameSetMethod(autoincrementado), autoincrementado.getReturnType());

                                }
                                long idEntidad = bd.insert(nombreTabla, null, valores);
                                if (autoincrementado != null){
                                    metodoIns.invoke(primaryKeyEntity, idEntidad);
                                }
                                retorno = idEntidad >= 0;
                            }catch(IllegalAccessException ex){
                                retorno = false;
                            } catch (IllegalArgumentException ex)
                            {
                                retorno = false;
                            } catch (NoSuchMethodException ex)
                            {
                                retorno = false;
                            } catch (SecurityException ex)
                            {
                                retorno = false;
                            } catch (InvocationTargetException ex)
                            {
                                retorno = false;
                            }
                        }

                        if (retorno){
                            // Borramos de las tablas relacionales y las entidades aquellos elementos que no están en la lista...
                            PrimaryFilter fFG = SGBDEngine.createPrimaryKeyFilter(primaryKeyEntity, true);
                            for(TableField fk : clsListaEntidad){
                                String nombreTablaExt = fk.EntityClass().getAnnotation(DBEntity.class).TableName();
                                String nombreTablaRel = "rel_" + DBEntity.TableName() + "_" + nombreTablaExt;
                                String filtro = fFG.FilterString;

                                Cursor query = bd.query(nombreTablaRel,null, fFG.FilterString, fFG.FilterData.toArray(new String[fFG.FilterData.size()]),null,null,null);
                                while (query.moveToNext()){
                                    String filtroExt = "";
                                    ArrayList<String> valoresExt = new ArrayList<String>();
                                    String[] cols = query.getColumnNames();

                                    for(String col : cols){
                                        if (col.startsWith(nombreTablaExt)){
                                            filtroExt += (filtroExt.isEmpty() ? "" : " and ") + col.replace(nombreTablaExt + "_", "") + " = ?";
                                            valoresExt.add(String.valueOf(query.getInt(query.getColumnIndex(col))));
                                        }
                                    }
                                    if (!filtroExt.isEmpty()) bd.delete(nombreTablaExt, filtroExt, valoresExt.toArray(new String[valoresExt.size()]));
                                }

                                bd.delete(nombreTablaRel, fFG.FilterString, fFG.FilterData.toArray(new String[fFG.FilterData.size()]));
                            }
                        }
                        // por último, las que proceden de un list...
                        if (retorno && camposListaEntidad.size() > 0){
                            for(Object c : camposListaEntidad){
                                String nombreTablaRel = "rel_" + DBEntity.TableName() + "_" + c.getClass().getAnnotation(DBEntity.class).TableName();

                                // Primero guardamos la entidad externa...
                                retorno = SGBDEngine.saveEntity(bd, c, true);
                                if (retorno){
                                    // Si la entidad externa se ha guardado, guardamos los datos de relación
                                    ContentValues valoresRelacion = new ContentValues();
                                    valoresRelacion.putAll(SGBDEngine.foreignKeysContentValues(primaryKeyEntity));
                                    valoresRelacion.putAll(SGBDEngine.foreignKeysContentValues(c));
                                    PrimaryFilter fDeleted = SGBDEngine.contentValues2PrimaryFilter(valoresRelacion);
                                    if (fDeleted != null){
                                        try{
                                            bd.delete(nombreTablaRel, fDeleted.FilterString, fDeleted.FilterData.toArray(new String[fDeleted.FilterData.size()]));
                                        }catch (Exception ex){

                                        }
                                    }

                                    try{
                                        long idEntidad = bd.insert(nombreTablaRel, null, valoresRelacion);

                                        retorno = idEntidad >= 0;

                                    }catch (Exception ex){
                                        retorno = false;
                                    }
                                }else{
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
        return retorno;
    }

    //endregion

}
