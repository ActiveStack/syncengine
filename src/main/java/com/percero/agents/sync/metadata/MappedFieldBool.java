package com.percero.agents.sync.metadata;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.lang.reflect.InvocationTargetException;

public class MappedFieldBool extends MappedField {

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
	
	public Boolean compareObjects(Object objectA, Object objectB) throws IllegalArgumentException,
	IllegalAccessException, InvocationTargetException {
		boolean valueA = (Boolean) getValue(objectA);
		boolean valueB = (Boolean) getValue(objectB);
		
		return (valueA == valueB);
	}
}
