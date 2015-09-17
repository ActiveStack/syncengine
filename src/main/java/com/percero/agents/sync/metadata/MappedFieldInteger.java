package com.percero.agents.sync.metadata;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.lang.reflect.InvocationTargetException;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class MappedFieldInteger extends MappedField {

	@Override
	public void readJsonField(JsonObject jsonObject, Object anObject)
			throws IllegalArgumentException, IllegalAccessException,
			InvocationTargetException, ClassNotFoundException, IOException {
		JsonElement elementField = jsonObject.get(getField().getName());
		int elementFieldValue = elementField.getAsInt();
		getSetter().invoke(anObject, elementFieldValue);
	}

	@Override
	public void readExternalField(ObjectInput input, Object anObject)
			throws IllegalArgumentException, IllegalAccessException,
			InvocationTargetException, ClassNotFoundException, IOException {
		getSetter().invoke(anObject, input.readInt());
	}

	@Override
	public void writeExternalField(ObjectOutput output, Object anObject)
			throws IllegalArgumentException, IOException,
			IllegalAccessException, InvocationTargetException {
		Number numberValue = ((Number) getGetter().invoke(anObject));
		int intValue = 0;
		if (numberValue != null) {
			intValue = numberValue.intValue();
		}
		output.writeInt(intValue);
	}
	
	@Override
	public Boolean isValueSetForQuery(Object anObject) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		Integer value = (Integer) getValue(anObject);
		return (value != null && value.intValue() != 0);
	}
	
	public Boolean compareObjects(Object objectA, Object objectB) throws IllegalArgumentException,
	IllegalAccessException, InvocationTargetException {
		Integer valueA = (Integer) getValue(objectA);
		Integer valueB = (Integer) getValue(objectB);
		
		if (valueA == null && valueB == null)
			return true;
		else if (valueA == null || valueB == null)
			return false;
		else
			return valueA.equals(valueB);
	}
}
