package com.github.tarsys.android.orm.base;

/**
 * Creado por TaRSyS el 7/04/14.
 */
public abstract interface IEntityEnum<T>
{
	T fromInt(int value);
	int value();
}
