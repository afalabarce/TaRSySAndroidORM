package com.github.tarsys.android.orm.dataobjects;

import java.io.Serializable;

/**
 * Creado por TaRSyS el 3/11/14.
 */
public class DataColumn implements Serializable
{
	private String columnName;
	private String columnTitle;
	private Class columnDataType;
	private boolean visible;

	//region Class Constructors

	public DataColumn(String columnName, String columnTitle, Class columnDataType)
	{
		this.columnName = columnName;
		this.columnTitle = columnTitle;
		this.columnDataType = columnDataType;
	}

	public DataColumn(String columnName, Class columnDataType)
	{
		this(columnName, columnName, columnDataType);
	}

	public DataColumn(String columnName, String columnTitle, Class columnDataType, boolean visible)
	{
		this(columnName, columnTitle, columnDataType);
		this.visible = visible;
	}

	public DataColumn(String columnName, Class columnDataType, boolean visible)
	{
		this(columnName, columnName, columnDataType,visible);
	}

	//endregion

	//region Getters y Setters

	public boolean isVisible()
	{
		return visible;
	}

	public void setVisible(boolean visible)
	{
		this.visible = visible;
	}

	public String getColumnName()
	{
		return columnName;
	}

	public void setColumnName(String columnName)
	{
		this.columnName = columnName;
	}

	public String getColumnTitle()
	{
		return columnTitle;
	}

	public void setColumnTitle(String columnTitle)
	{
		this.columnTitle = columnTitle;
	}

	public Class getColumnDataType()
	{
		return columnDataType;
	}

	public void setColumnDataType(Class columnDataType)
	{
		this.columnDataType = columnDataType;
	}

	//endregion

}
