package com.percero.agents.sync.metadata;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.lang.reflect.InvocationTargetException;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class MappedFieldString extends MappedField {


	@Override
	public void readJsonField(JsonObject jsonObject, Object anObject)
			throws IllegalArgumentException, IllegalAccessException,
			InvocationTargetException, ClassNotFoundException, IOException {
		JsonElement elementField = jsonObject.get(getField().getName());
		String elementFieldValue = elementField.getAsString();
		getSetter().invoke(anObject, elementFieldValue);
	}

	@Override
	public void readExternalField(ObjectInput input, Object anObject)
			throws IllegalArgumentException, IllegalAccessException,
			InvocationTargetException, ClassNotFoundException, IOException {
		getSetter().invoke(anObject, input.readUTF());
	}

	@Override
	public void writeExternalField(ObjectOutput output, Object anObject)
			throws IllegalArgumentException, IOException,
			IllegalAccessException, InvocationTargetException {
		String stringValue = (String) getGetter().invoke(anObject);
		if (stringValue == null)
			stringValue = "";
		output.writeUTF(stringValue);
	}
	
	public Boolean compareObjects(Object objectA, Object objectB) throws IllegalArgumentException,
	IllegalAccessException, InvocationTargetException {
		String valueA = (String) getValue(objectA);
		String valueB = (String) getValue(objectB);
		
		if (valueA == null && valueB == null)
			return true;
		else if (valueA == null || valueB == null)
			return false;
		else
			return valueA.equals(valueB);
	}
}
