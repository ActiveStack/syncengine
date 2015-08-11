package com.percero.agents.sync.metadata;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.lang.reflect.InvocationTargetException;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class MappedFieldInt extends MappedField {

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
		int intValue = ((Integer) getGetter().invoke(anObject)).intValue();
		output.writeInt(intValue);
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
