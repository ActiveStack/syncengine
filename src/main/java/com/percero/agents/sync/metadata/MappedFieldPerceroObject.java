package com.percero.agents.sync.metadata;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.lang.reflect.InvocationTargetException;

import org.apache.log4j.Logger;
import org.springframework.util.StringUtils;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.percero.agents.sync.vo.ClassIDPair;
import com.percero.framework.vo.IPerceroObject;

public class MappedFieldPerceroObject extends MappedField {

	private static final Logger log = Logger.getLogger(MappedFieldPerceroObject.class);
	
	private Boolean targetEntity = null;
	public Boolean isTargetEntity() {
		if (targetEntity == null) {
			targetEntity = this.getMappedClass().getTargetMappedFields().contains(this);
		}
		return targetEntity;
	}
	
	private Boolean sourceEntity = null;
	public Boolean isSourceEntity() {
		if (sourceEntity == null) {
			sourceEntity = this.getMappedClass().getSourceMappedFields().contains(this);
		}
		return sourceEntity;
	}

	@Override
	public void writeJsonField(JsonObject jsonObject, Object anObject)
			throws IllegalArgumentException, IOException,
			IllegalAccessException, InvocationTargetException {
		IPerceroObject perceroObject = (IPerceroObject) getGetter().invoke(anObject);
		ClassIDPair classIdPair = new ClassIDPair("0", "");
		if (perceroObject != null) {
			classIdPair.setClassName(perceroObject.getClass().getName());
			classIdPair.setID(perceroObject.getID());
		}
		Gson gson = new Gson();
		jsonObject.add(getField().getName(), gson.toJsonTree(classIdPair));
	}
	
	@SuppressWarnings("rawtypes")
	@Override
	public void readExternalField(ObjectInput input, Object anObject)
			throws IllegalArgumentException, IllegalAccessException,
			InvocationTargetException, ClassNotFoundException, IOException {
		try {
			// TODO: Is there any reason to retrieve the entire object from the database?
			//	Or is it ok to just have the stub (ID)?
			ClassIDPair classIdPair = (ClassIDPair) input.readObject();
			if (!classIdPair.getID().equals("0") && StringUtils.hasText(classIdPair.getClassName())) {
				Class clazz = MappedClass.forName(classIdPair.getClassName());
				Object foundObject = clazz.newInstance();
				if (foundObject instanceof IPerceroObject)
					((IPerceroObject) foundObject).setID(classIdPair.getID());
				else
					log.warn("Attempt to deserialize in MappedFieldCachedObject object that is NOT IPerceroObject: " + classIdPair.getClassName());
				getSetter().invoke(anObject, foundObject);
			}
		} catch(Exception e) {
			log.error("Error deserializing " + anObject.getClass().getName() + ", field " + this.getField().getName(), e);
		}
	}

	@Override
	public void writeExternalField(ObjectOutput output, Object anObject)
			throws IllegalArgumentException, IOException,
			IllegalAccessException, InvocationTargetException {
		IPerceroObject perceroObject = (IPerceroObject) getGetter().invoke(anObject);
		ClassIDPair classIdPair = new ClassIDPair("0", "");
		if (perceroObject != null) {
			classIdPair.setClassName(perceroObject.getClass().getName());
			classIdPair.setID(perceroObject.getID());
		}
		output.writeObject(classIdPair);
	}
	
	public IPerceroObject getPerceroObjectValue(Object anObject) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		IPerceroObject result = (IPerceroObject) getValue(anObject);
		return result;
	}
	
	@Override
	public Boolean isValueSetForQuery(Object anObject) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		IPerceroObject value = getPerceroObjectValue(anObject);
		return (value != null && StringUtils.hasText(value.getID()));
	}
	
	public Boolean compareObjects(Object objectA, Object objectB) throws IllegalArgumentException,
	IllegalAccessException, InvocationTargetException {
		IPerceroObject valueA = getPerceroObjectValue(objectA);
		IPerceroObject valueB = getPerceroObjectValue(objectB);
		
		if (valueA == null && valueB == null)
			return true;
		else if (valueA == null || valueB == null)
			return false;
		else
			return (valueA.getID().equals(valueB.getID()) && valueA.getClass().getCanonicalName().equals(valueB.getClass().getCanonicalName()));
	}

}
