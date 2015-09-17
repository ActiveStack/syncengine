package com.percero.agents.sync.metadata;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Date;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class MappedFieldDate extends MappedField {

	@Override
	public void writeJsonField(JsonObject jsonObject, Object anObject)
			throws IllegalArgumentException, IOException,
			IllegalAccessException, InvocationTargetException {
		Date dateObject = (Date) getGetter().invoke(anObject);
		long timestamp = 0;
		if (dateObject != null)
			timestamp = dateObject.getTime();
		Gson gson = new Gson();
		jsonObject.addProperty(getField().getName(), gson.toJson(timestamp));
	}
	
	@Override
	public Boolean isValueSetForQuery(Object anObject) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		Date value = (Date) getValue(anObject);
		return (value != null && value.getTime() > 0);
	}
	
	public Boolean compareObjects(Object objectA, Object objectB) throws IllegalArgumentException,
	IllegalAccessException, InvocationTargetException {
		Date valueA = (Date) getValue(objectA);
		Date valueB = (Date) getValue(objectB);
		
		if ((valueA == null || valueA.getTime() <= 0) && (valueB == null || valueB.getTime() <= 0))
			return true;
		else if (valueA == null || valueA.getTime() <= 0 || valueB == null || valueB.getTime() <= 0)
			return false;
		else
			return (valueA.getTime() == valueB.getTime());
	}
	
}
