package com.percero.agents.sync.metadata;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.lang.reflect.InvocationTargetException;

public class MappedFieldBoolean extends MappedField {

	@Override
	public void readExternalField(ObjectInput input, Object anObject)
			throws IllegalArgumentException, IllegalAccessException,
			InvocationTargetException, ClassNotFoundException, IOException {
		getSetter().invoke(anObject, input.readBoolean());
	}

	@Override
	public void writeExternalField(ObjectOutput output, Object anObject)
			throws IllegalArgumentException, IOException,
			IllegalAccessException, InvocationTargetException {
		boolean booleanValue = ((Boolean) getGetter().invoke(anObject)).booleanValue();
		output.writeBoolean(booleanValue);
	}

	@Override
	public Boolean isValueSetForQuery(Object anObject) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		Boolean value = (Boolean) getValue(anObject);
		return (value != null);
	}
	
	public Boolean compareObjects(Object objectA, Object objectB) throws IllegalArgumentException,
	IllegalAccessException, InvocationTargetException {
		Boolean valueA = (Boolean) getValue(objectA);
		Boolean valueB = (Boolean) getValue(objectB);
		
		if ((valueA == null || !valueA) && (valueB == null || !valueB))
			return true;
		else if (valueA == null || !valueA || valueB == null || !valueB)
			return false;
		else
			return valueA.equals(valueB);
	}
}
