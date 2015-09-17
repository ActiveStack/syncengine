package com.percero.agents.sync.metadata;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.lang.reflect.InvocationTargetException;

public class MappedFieldDouble extends MappedField {

	@Override
	public void readExternalField(ObjectInput input, Object anObject)
			throws IllegalArgumentException, IllegalAccessException,
			InvocationTargetException, ClassNotFoundException, IOException {
		getSetter().invoke(anObject, input.readDouble());
	}

	@Override
	public void writeExternalField(ObjectOutput output, Object anObject)
			throws IllegalArgumentException, IOException,
			IllegalAccessException, InvocationTargetException {
		double doubleValue = ((Double) getGetter().invoke(anObject)).doubleValue();
		output.writeDouble(doubleValue);
	}

	@Override
	public Boolean isValueSetForQuery(Object anObject) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		Double value = (Double) getValue(anObject);
		return (value != null && value.doubleValue() != 0.0);
	}
	
	public Boolean compareObjects(Object objectA, Object objectB) throws IllegalArgumentException,
	IllegalAccessException, InvocationTargetException {
		Double valueA = (Double) getValue(objectA);
		Double valueB = (Double) getValue(objectB);
		
		if (valueA == null && valueB == null)
			return true;
		else if (valueA == null || valueB == null)
			return false;
		else
			return valueA.equals(valueB);
	}
}
