/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.tarsys.android.orm.dataobjects;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

/**
 *
 * @author TaRSyS
 */
public class JSONDataSource implements Serializable, Iterable<JsonObject>
{
	//<editor-fold defaultstate="collapsed" desc="Variables privadas a la clase">

	private final ArrayList<String> columns = new ArrayList<String>();
	private String jsonData = "";
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Métodos públicos de la clase">

	/**
	 * Obtiene las columns del origen de dataRows
	 * @return
	 */
	public ArrayList<String> getColumns() {
		return columns;
	}

	/**
	 * Obtiene el número de elementos del origen de dataRows
	 * @return
	 */
	public int size(){

		return this.getData().size();
	}

	/**
	 * Obtiene el objeto JsonArray asociado al origen de dataRows
	 * @return
	 */
	public JsonArray getData() {
		JsonArray datos = new JsonArray();
		try{
			if (!this.jsonData.isEmpty()) datos = new JsonParser().parse(this.jsonData).getAsJsonArray();
		}catch(JsonSyntaxException ex){

		}
		return datos;
	}

	public void addDataObject(JsonObject item){
		JsonArray datos = this.getData();

		if (this.columns.isEmpty()){
			if (item != null){
				for (Map.Entry<String, JsonElement>e : item.entrySet()) this.columns.add(e.getKey());
			}
		}
		datos.add(item);

		this.jsonData = datos.toString();
	}

	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Constructores de la clase">

	public JSONDataSource(){

	}
	public JSONDataSource(JsonArray jArray){
		if (jArray != null && jArray.size() > 0){
			// cogemos el primer objeto del array, para sacarle sus claves...
			try{
				JsonObject obj = jArray.get(0).getAsJsonObject();
				if (obj != null){
					for (Map.Entry<String, JsonElement>e : obj.entrySet()) this.columns.add(e.getKey());
				}
				this.jsonData = jArray.toString();
			}catch(Exception ex){
			}
		}
	}

	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Métodos de la interfaz Iterable<JsonObject>">

	public Iterator<JsonObject> iterator() {
		Iterator<JsonObject> it = new Iterator<JsonObject>() {
			private final JsonArray dataRows = JSONDataSource.this.getData();

			private int currentIndex = 0;

			@Override
			public boolean hasNext() {
				return currentIndex < this.dataRows.size() && this.dataRows.get(currentIndex) != null;
			}

			@Override
			public JsonObject next() {
				return this.dataRows.get(currentIndex++).getAsJsonObject();
			}

			@Override
			public void remove() {
				// TODO Auto-generated method stub
			}
		};
		return it;
	}

	//</editor-fold>
}
