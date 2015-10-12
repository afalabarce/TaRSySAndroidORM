package com.tarsys.android.orm.engine;

import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import com.tarsys.android.orm.annotations.DBEntity;
import com.tarsys.android.orm.annotations.Index;
import com.tarsys.android.orm.annotations.TableField;
import com.tarsys.android.orm.enums.DBDataType;
import com.tarsys.android.orm.sqlite.SQLiteSupport;

import java.util.ArrayList;

/**
 * Created by tarsys on 9/10/15.
 */
public class DBTable {
    public DBEntity Table;
    public ArrayList<TableField> Fields = new ArrayList<TableField>();
    public ArrayList<Index> Indexes = new ArrayList<Index>();

    private String sqlCreationForeignKey(DBTable foreignKeyTable){
        String returnValue;

        String foreignKeyTableName = "rel_" + this.Table.TableName().toLowerCase() + "_" + foreignKeyTable.Table.TableName().toLowerCase();
        String foreignKeyFieldsDefinition  = "";
        ArrayList<String> primaryKeyForeignKeyFields = new ArrayList<String>();
        for(TableField cmp : this.Fields){
            if (cmp.PrimaryKey()){
                primaryKeyForeignKeyFields.add(this.Table.TableName() + "_" + cmp.FieldName());
                foreignKeyFieldsDefinition += foreignKeyFieldsDefinition.trim().isEmpty()? "" : ", ";
                foreignKeyFieldsDefinition +=  String.format("%s_%s %s", this.Table.TableName().toLowerCase(),
                        (cmp.DataType().equals(DBDataType.DateDataType) ?
                                SQLiteSupport.PREFIX_DATE_FIELD : "")
                                + cmp.FieldName(),
                        cmp.DataType().SqlType(cmp.DataTypeLength())
                );
            }
        }

        for(TableField cmp : foreignKeyTable.Fields){
            if (cmp.PrimaryKey()){
                primaryKeyForeignKeyFields.add(foreignKeyTable.Table.TableName() + "_" + cmp.FieldName());
                foreignKeyFieldsDefinition += foreignKeyFieldsDefinition.trim().isEmpty()? "" : ", ";
                foreignKeyFieldsDefinition +=  String.format("%s_%s %s", foreignKeyTable.Table.TableName().toLowerCase(),
                        (cmp.DataType().equals(DBDataType.DateDataType) ?
                                SQLiteSupport.PREFIX_DATE_FIELD : "")
                                + cmp.FieldName(),
                        cmp.DataType().SqlType(cmp.DataTypeLength())
                );
            }
        }

        String pKeyFK = ", PRIMARY KEY(" + TextUtils.join(", ", primaryKeyForeignKeyFields) + ")";
        returnValue = "create table if not exists " + foreignKeyTableName +"(" + foreignKeyFieldsDefinition + pKeyFK + ")";

        return returnValue;
    }

    public ArrayList<String> sqlCreateTable(){
        ArrayList<String> returnValue = new ArrayList<String>();
        String createTable = "create table if not exists " + this.Table.TableName();
        String paramDefinition = "";
        ArrayList<String> primaryKeyFields = new ArrayList<String>();

        for(TableField field : this.Fields){
            if (field.PrimaryKey()){
                if (field.DataType() != DBDataType.EntityDataType && field.DataType() != DBDataType.EntityListDataType){
                    primaryKeyFields.add(field.FieldName());
                }else if (field.DataType() == DBDataType.EntityDataType){
                    // Si la clave primaria está formada por una entidad, entonces la clave primaria estár formada por los campos identificadores de la entidad
                    DBEntity eBdPKEnt = field.EntityClass().getAnnotation(DBEntity.class);
                    ArrayList<String> camposPKEntidad = SGBDEngine.getPrimaryKeyFieldNames(field.EntityClass());
                    for(String cmpPKEnt : camposPKEntidad){
                        primaryKeyFields.add(eBdPKEnt.TableName() + "_" + cmpPKEnt);
                    }
                }
            }
            if (field.DataType() != DBDataType.EntityDataType && field.DataType() != DBDataType.EntityListDataType){
                //region Primitive fields creation
                paramDefinition += paramDefinition.trim().isEmpty()? "" : ", ";
                paramDefinition +=  String.format("%s %s %s %s", (field.DataType().equals(DBDataType.DateDataType) ?
                                SQLiteSupport.PREFIX_DATE_FIELD : "") + field.FieldName(),
                        field.DataType().SqlType(field.DataTypeLength()),
                        field.NotNull() ? "not null" : "",
                        field.NotNull() && !field.DefaultValue().isEmpty() ? ("default " + field.DefaultValue()) : "");
                //endregion
            }else if (field.DataType() == DBDataType.EntityDataType && field.EntityClass().getAnnotation(DBEntity.class) != null){
                //region EntityDataType field creation

                // Si el campo es de tipo entidad, se crean tantos campos con el nombre del campo nombretablaexterna_nombrecampopkentablaexterna
                ArrayList<DBTable> tablasEntidad = SGBDEngine.createDBTableEntity(field.EntityClass());
                DBEntity eBd = field.EntityClass().getAnnotation(DBEntity.class);
                for(DBTable tbl : tablasEntidad){
                    if (tbl.Table.TableName().equalsIgnoreCase(eBd.TableName())){
                        // Una vez tenemos la tabla,  crearemos los campos en función de la clave primaria...
                        for(TableField cmp : tbl.Fields){
                            if (cmp.PrimaryKey()){
                                paramDefinition += paramDefinition.trim().isEmpty()? "" : ", ";
                                paramDefinition +=  String.format("%s_%s %s", tbl.Table.TableName().toLowerCase(),
                                        (cmp.DataType().equals(DBDataType.DateDataType) ?
                                                SQLiteSupport.PREFIX_DATE_FIELD : "") + cmp.FieldName(),
                                        cmp.DataType().SqlType(cmp.DataTypeLength())
                                );
                            }
                        }
                        break;
                    }
                }
                //endregion

            }else if (field.DataType() == DBDataType.EntityListDataType && field.EntityClass().getAnnotation(DBEntity.class) != null){
                //region EntityList Fields creation

                ArrayList<DBTable> entityTables = SGBDEngine.createDBTableEntity(field.EntityClass());
                DBEntity eBd = field.EntityClass().getAnnotation(DBEntity.class);
                for(DBTable tbl : entityTables){
                    if (tbl.Table.TableName().equalsIgnoreCase(eBd.TableName())){
                        returnValue.add(this.sqlCreationForeignKey(tbl));
                        break;
                    }
                }
                //endregion
            }
        }
        String pKey = primaryKeyFields.size() > 0 ? ", PRIMARY KEY(" + TextUtils.join(", ", primaryKeyFields) + ")" : "";
        createTable += " (" + paramDefinition + pKey + ")";
        returnValue.add(createTable);
        return returnValue;
    }

    public ArrayList<String> sqlCreationQuerys(SQLiteDatabase database){
        ArrayList<String> retorno = new ArrayList<String>();

        // Si la tabla no existe, crearemos todo lo que necesitemos...
        if (!SQLiteSupport.TableExistsInDataBase(database, this.Table.TableName())){
            retorno.addAll(this.sqlCreateTable());
        }else{
            for(TableField campo : this.Fields){


                if (campo.DataType() != DBDataType.EntityDataType && campo.DataType() != DBDataType.EntityListDataType){
                    //region add new primitive fields to tableName
                    String nField = (campo.DataType().equals(DBDataType.DateDataType)? SQLiteSupport.PREFIX_DATE_FIELD: "") +  campo.FieldName();
                    if (!SQLiteSupport.ExistsTableField(database, this.Table.TableName(), nField)){
                        // Si el campo no existe... lo agregaremos
                        String uptField = String.format("alter table %s add column %s %s %s %s", this.Table.TableName(),
                                nField,
                                campo.DataType().SqlType(campo.DataTypeLength()),
                                campo.NotNull() ? "not null" : "",
                                campo.NotNull() ? ("default " + campo.DefaultValue()) : "");
                        retorno.add(uptField);
                    }
                    //endregion
                }else{
                    DBEntity eBd = campo.EntityClass().getAnnotation(DBEntity.class);
                    ArrayList<DBTable> entityTables = SGBDEngine.createDBTableEntity(campo.EntityClass());

                    if (campo.EntityClass().getAnnotation(DBEntity.class) != null){
                        if (campo.DataType() == DBDataType.EntityDataType){
                            //region add new Entity fields
                            for(DBTable tbl : entityTables){
                                if (tbl.Table.TableName().equalsIgnoreCase(eBd.TableName())){
                                    // Una vez tenemos la tabla,  crearemos los campos en función de la clave primaria...
                                    for(TableField cmp : tbl.Fields){
                                        if (cmp.PrimaryKey()){
                                            String nCmp = String.format("%s_%s", tbl.Table.TableName(),
                                                    (cmp.DataType().equals(DBDataType.DateDataType) ?
                                                            SQLiteSupport.PREFIX_DATE_FIELD : "")  // Si es una fecha, le pegaremos como prefijo un dt_
                                                            + cmp.FieldName());
                                            if (!SQLiteSupport.ExistsTableField(database, this.Table.TableName(), nCmp)){
                                                String uptCampo = String.format("alter table %s add column %s %s ", this.Table.TableName(),
                                                        nCmp,
                                                        cmp.DataType().SqlType(cmp.DataTypeLength()));
                                                retorno.add(uptCampo);
                                            }
                                        }
                                    }
                                    break;
                                }
                            }
                            //endregion
                        }else if (campo.DataType() == DBDataType.EntityListDataType){
                            for(DBTable tbl : entityTables){
                                if (tbl.Table.TableName().equalsIgnoreCase(eBd.TableName())){
                                    String nombreTablaFK = "rel_" + this.Table.TableName().toLowerCase() + "_" + tbl.Table.TableName().toLowerCase();
                                    // Si la tabla con clave externa no existe, la crearemos... (si ya existe, no se toca, ya que en sqlite las primary key no se miran...)
                                    if (!SQLiteSupport.TableExistsInDataBase(database, nombreTablaFK)){
                                        retorno.add(this.sqlCreationForeignKey(tbl));
                                    }
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
        // finally, we add indexes...
        for (Index indice : this.Indexes){
            // Primero la eliminación del índice...
            retorno.add("drop index if exists " + indice.IndexName());
            retorno.add(String.format("create %s index if not exists %s on %s (%s)",
                    indice.IsUniqueIndex() ? "unique" : "",
                    indice.IndexName(),
                    this.Table.TableName(),
                    TextUtils.join(",", indice.IndexFields())));
        }
        return retorno;
    }

}
