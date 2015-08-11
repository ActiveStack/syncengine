package com.percero.agents.sync.metadata;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import com.percero.agents.sync.manager.DataExternalizer;

public class MappedFieldMap extends MappedField {

	@Override
	public void readExternalField(ObjectInput input, Object anObject)
			throws IllegalArgumentException, IllegalAccessException,
			InvocationTargetException, ClassNotFoundException, IOException {
		// Basically do nothing.  Maps and Lists are not passed from the client to the server.
		input.readObject();
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void writeExternalField(ObjectOutput output, Object anObject)
			throws IllegalArgumentException, IOException,
			IllegalAccessException, InvocationTargetException {
		Map mapObject = (Map) getGetter().invoke(anObject);
		Object[] objectArray = DataExternalizer.getClassIDPairArray(mapObject.values());
		output.writeObject(objectArray);
	}
}
