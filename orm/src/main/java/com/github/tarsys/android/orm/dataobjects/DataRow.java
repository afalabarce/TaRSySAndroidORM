package com.github.tarsys.android.orm.dataobjects;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;

/**
 * Creado por TaRSyS el 3/11/14.
 */
public class DataRow extends LinkedHashMap<String,Object> implements Serializable
{
	private DataSource dataSource;

	public DataRow(DataSource dataSource)
	{
		this();
		this.dataSource = dataSource;
	}

	public DataRow(){
		super();
	}

	public DataSource getDataSource()
	{
		return dataSource;
	}

	public void setDataSource(DataSource dataSource)
	{
		this.dataSource = dataSource;
	}


    public boolean getAsBoolean(String columnName){
        boolean retorno;

        try{
           retorno = Boolean.parseBoolean(this.get(columnName).toString());
        }catch (Exception ex){
            retorno = false;
        }

        return retorno;
    }

    public boolean isNull (String columnName){
        return this.get(columnName) == null;
    }

    public int getAsInt(String columnName){
        int retorno;
        try {
            retorno = Integer.parseInt(this.get(columnName).toString());
        }catch(Exception ex){
            retorno = 0;
        }
        return retorno;
    }

    public float getAsFloat (String columnName){
        float retorno;
        try {
            retorno = Float.parseFloat(this.get(columnName).toString());
        }catch(Exception ex){
            retorno = 0;
        }
        return retorno;
    }

    public double getAsDouble (String columnName){
        double retorno;
        try {
            retorno = Double.parseDouble(this.get(columnName).toString());
        }catch(Exception ex){
            retorno = 0;
        }
        return retorno;
    }

    public Date getAsDate (String columnName){
        Date retorno;

        try{
            retorno = (Date)this.get(columnName);
        }catch(Exception ex){
            Calendar exCal = Calendar.getInstance();
            exCal.set(1900, Calendar.JANUARY,1);
            retorno = exCal.getTime();
        }

        return retorno;
    }

    public String getAsString(String columnName){
        return this.get(columnName) == null ? "" : this.get(columnName).toString();
    }
}
