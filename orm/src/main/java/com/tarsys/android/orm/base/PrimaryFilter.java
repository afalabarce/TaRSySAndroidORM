package com.tarsys.android.orm.base;

import java.util.ArrayList;

/**
 * Creado por TaRSyS el 7/04/14.
 */
public class PrimaryFilter
{
	public String FilterString;
	public ArrayList<String> FilterData;

	public PrimaryFilter(){
		this.FilterString = "";
		this.FilterData = new ArrayList<String>();
	}
}
