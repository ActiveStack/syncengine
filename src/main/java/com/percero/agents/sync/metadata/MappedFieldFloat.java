package com.percero.agents.sync.metadata;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.lang.reflect.InvocationTargetException;

public class MappedFieldFloat extends MappedField {

	@Override
	public void readExternalField(ObjectInput input, Object anObject)
			throws IllegalArgumentException, IllegalAccessException,
			InvocationTargetException, ClassNotFoundException, IOException {
		getSetter().invoke(anObject, input.readFloat());
	}

	@Override
	public void writeExternalField(ObjectOutput output, Object anObject)
			throws IllegalArgumentException, IOException,
			IllegalAccessException, InvocationTargetException {
		float floatValue = ((Float) getGetter().invoke(anObject)).floatValue();
		output.writeFloat(floatValue);
	}
	
	public Boolean compareObjects(Object objectA, Object objectB) throws IllegalArgumentException,
	IllegalAccessException, InvocationTargetException {
		Float valueA = (Float) getValue(objectA);
		Float valueB = (Float) getValue(objectB);
		
		if (valueA == null && valueB == null)
			return true;
		else if (valueA == null || valueB == null)
			return false;
		else
			return valueA.equals(valueB);
	}
}
