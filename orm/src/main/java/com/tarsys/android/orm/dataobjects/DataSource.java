package com.tarsys.android.orm.dataobjects;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/**
 * Creado por TaRSyS el 3/11/14.
 */
public class DataSource implements Serializable, Iterable<DataRow>
{
	private ArrayList<DataColumn> columns;
	private ArrayList<DataRow> rows;

	public DataSource()
	{
		this.columns = new ArrayList<DataColumn>();
		this.rows = new ArrayList<DataRow>();
	}

    public DataSource clone(){
        DataSource retorno = new DataSource();

        retorno.getColumns().addAll(this.getColumns());

        return retorno;
    }

    public boolean isEmpty(){
        return this.rows.isEmpty();
    }

    public DataRow importRow(DataRow origen){
        DataRow returnValue = new DataRow(this);

        returnValue.putAll(origen);

        return returnValue;
    }

    public DataSource copy() {
        DataSource retorno = this.clone();

        for(DataRow fila : this){
            retorno.getRows().add(this.importRow(fila));
        }

        return retorno;
    }

	public void addColumns(Collection<? extends DataColumn> cols)
	{
		this.columns.addAll(cols);
	}

	//region Getters y Setters

	public ArrayList<DataColumn> getColumns()
	{
		return columns;
	}

	public DataColumn getColumna (String nombreColumna){
		DataColumn returnValue = null;

		for(DataColumn columna : this.columns){
			if (columna.getColumnName().equalsIgnoreCase(nombreColumna)){
				returnValue = columna;
				break;
			}
		}

		return returnValue;
	}
	public void setColumns(ArrayList<DataColumn> columns)
	{
		this.columns = columns;
	}

	public ArrayList<DataRow> getRows()
	{
		return rows;
	}

	//endregion

	//region interface Iterable<LinkedHashMap<String,Object>>

	@Override
	public Iterator<DataRow> iterator()
	{
		Iterator<DataRow> it = new Iterator<DataRow>() {
			private final ArrayList<DataRow> dataRows = DataSource.this.rows;

			private int currentIndex = 0;

			@Override
			public boolean hasNext() {
				return currentIndex < this.dataRows.size() && this.dataRows.get(currentIndex) != null;
			}

			@Override
			public DataRow next() {
				return this.dataRows.get(currentIndex++);
			}

			@Override
			public void remove() {
				// TODO Auto-generated method stub
			}
		};
		return it;
	}

    public int size(){
        return this.rows.size();
    }

	//endregion

}
