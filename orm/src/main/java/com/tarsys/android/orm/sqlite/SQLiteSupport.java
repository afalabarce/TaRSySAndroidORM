package com.tarsys.android.orm.sqlite;

import android.annotation.SuppressLint;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.tarsys.android.orm.enums.DBDataType;

/**
 * Created by tarsys on 9/10/15.
 */
public class SQLiteSupport {
    public static final String SQLite3_EXTAG = "SQLite3Support";
    public static final String PREFIX_DATE_FIELD = "dt_";

    @SuppressLint("LongLogTag")
    public static SQLiteDatabase DataBaseConnection(String pathBD, boolean readOnly){
        SQLiteDatabase returnValue = null;

        try{
            returnValue = SQLiteDatabase.openDatabase(pathBD, null, readOnly ? SQLiteDatabase.OPEN_READONLY : SQLiteDatabase.OPEN_READWRITE);
            if (!(returnValue != null && returnValue.isOpen())){
                Log.e(SQLiteSupport.SQLite3_EXTAG, "The Database " + pathBD + "is not openned.");
                returnValue = null;
            }
        }catch(Exception ex){
            Log.e(SQLiteSupport.SQLite3_EXTAG + "::DataBaseConnection", ex.getMessage());
            returnValue = null;
        }

        return returnValue;
    }
    @SuppressLint("LongLogTag")
    public static boolean ExistsTableField(SQLiteDatabase database, String tableName, String fieldName){
        boolean returnValue = false;

        try{
            Cursor ti = database.rawQuery("PRAGMA table_info("+ tableName + ")", null);
            if ( ti.moveToFirst() ) {
                do {
                    String nBdField = ti.getString(1);
                    if (nBdField.equalsIgnoreCase(fieldName)) {
                        returnValue = true;
                        break;
                    }
                } while (ti.moveToNext());
                ti.close();
            }
        }catch (Exception ex){
            Log.e(SQLiteSupport.SQLite3_EXTAG + "::ExistsTableField", ex.getMessage());
        }

        return returnValue;
    }

    @SuppressLint("LongLogTag")
    public static String FieldDataTypeAsString(SQLiteDatabase database, String tableName, String fieldName){
        String returnValue = "";

        try{
            Cursor ti = database.rawQuery("PRAGMA table_info("+ tableName + ")", null);
            if ( ti.moveToFirst() ) {
                do {
                    String nDbField = ti.getString(1);
                    if (nDbField.equalsIgnoreCase(fieldName)) {
                        returnValue = ti.getString(2);
                        break;
                    }
                } while (ti.moveToNext());
                ti.close();
            }
        }catch (Exception ex){
            Log.e(SQLiteSupport.SQLite3_EXTAG + "::FieldDataTypeAsString", ex.getMessage());
        }

        return returnValue;
    }

    @SuppressLint("LongLogTag")
    public static DBDataType FieldDataType(SQLiteDatabase database, String tableName, String fieldName){
        DBDataType returnValue = DBDataType.StringDataType;

        try{
            Cursor ti = database.rawQuery("PRAGMA table_info("+ tableName + ")", null);
            if ( ti.moveToFirst() ) {
                do {
                    String nDbField = ti.getString(1);
                    if (nDbField.equalsIgnoreCase(fieldName)) {
                        returnValue = DBDataType.DataType(nDbField);
                        if (returnValue.equals(DBDataType.IntegerDataType) && fieldName.startsWith(SQLiteSupport.PREFIX_DATE_FIELD)) returnValue = DBDataType.DateDataType;
                        break;
                    }
                } while (ti.moveToNext());
                ti.close();
            }
        }catch (Exception ex){
            Log.e(SQLiteSupport.SQLite3_EXTAG + "::FieldDataType", ex.getMessage());
        }

        return returnValue;
    }

    @SuppressLint("LongLogTag")
    public static long FieldDataTypeLength(SQLiteDatabase database, String tableName, String fieldName){
        long returnValue = 0L;

        try{
            if (SQLiteSupport.FieldDataType(database, tableName, fieldName).equals(DBDataType.StringDataType)){
                
                String lCampo = SQLiteSupport.FieldDataTypeAsString(database, tableName, fieldName).replace("varchar", "").replace("(", "").replace(")", "").trim();
                returnValue = Long.parseLong(lCampo);
            }
        }catch(Exception ex){
            returnValue = 0L;
            Log.e(SQLiteSupport.SQLite3_EXTAG + "::FieldDataTypeLength", ex.getMessage());
        }
        return returnValue;
    }

    @SuppressLint("LongLogTag")
    public static boolean TableExistsInDataBase(SQLiteDatabase database, String tableName){
        boolean returnvalue = false;

        try{
            Cursor ti = database.rawQuery("PRAGMA table_info("+ tableName + ")", null);
            returnvalue = ti.moveToFirst();
            if (returnvalue) ti.close();
        }catch (Exception ex){
            Log.e(SQLiteSupport.SQLite3_EXTAG + "::TableExistsInDataBase", ex.getMessage());
        }

        return returnvalue;
    }
}
