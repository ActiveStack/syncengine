package com.percero.agents.sync.metadata;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.percero.agents.sync.manager.DataExternalizer;
import com.percero.framework.vo.IPerceroObject;

public class MappedFieldList extends MappedField {

	@SuppressWarnings("rawtypes")
	@Override
	public void writeJsonField(JsonObject jsonObject, Object anObject)
			throws IllegalArgumentException, IOException,
			IllegalAccessException, InvocationTargetException {
		List listObject = (List) getGetter().invoke(anObject);
		Object[] objectArray = DataExternalizer.getClassIDPairArray(listObject);

		Gson gson = new Gson();
		jsonObject.add(getField().getName(), gson.toJsonTree(objectArray));
	}
	
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
		List listObject = (List) getGetter().invoke(anObject);
		Object[] objectArray = DataExternalizer.getClassIDPairArray(listObject);
		output.writeObject(objectArray);
	}
	
	@SuppressWarnings("rawtypes")
	@Override
	public Boolean isValueSetForQuery(Object anObject) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		List value = (List) getValue(anObject);
		return (value != null && !value.isEmpty());
	}
	
	public Boolean compareObjects(Object objectA, Object objectB) throws IllegalArgumentException,
	IllegalAccessException, InvocationTargetException {
		List valueA = (List) getValue(objectA);
		List valueB = (List) getValue(objectB);
		
		if (valueA == null && valueB == null)
			return true;
		else if (valueA == null || valueB == null)
			return false;
		else if (valueA.size() != valueB.size())
			return false;
		else {
			Iterator itrListA = valueA.iterator();
			while (itrListA.hasNext()) {
				Object nextValueA = itrListA.next();
				Iterator itrListB = valueB.iterator();
				Boolean objectExists = false;
				while (itrListB.hasNext()) {
					Object nextValueB = itrListB.next();
					
					if (nextValueA instanceof IPerceroObject) {
						if (nextValueB instanceof IPerceroObject) {
							if (((IPerceroObject)nextValueA).getID().equals(((IPerceroObject)nextValueB).getID()) &&
									nextValueA.getClass().getCanonicalName().equals(nextValueB.getClass().getCanonicalName())
							) {
								objectExists = true;
								break;
							}
						}
						else {
							continue;
						}
					}
					else if (nextValueB instanceof IPerceroObject) {
						continue;
					}
					else {
						if (nextValueA.equals(nextValueB)) {
							objectExists = true;
							break;
						}
					}
				}
				
				if (!objectExists) {
					return false;
				}
			}
			
			// Since the sizes are the same, and all objects in ListA were in ListB, then all objects in ListB are in ListA.
			return true;
		}
	}
}
