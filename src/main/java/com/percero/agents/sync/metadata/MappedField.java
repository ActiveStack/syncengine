package com.percero.agents.sync.metadata;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.percero.framework.metadata.IMappedQuery;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.Map.Entry;

public class MappedField {

	private MappedClass mappedClass;
	private Field field;
	private Method getter;
	private Method setter;
	private String columnName;
	private String joinColumnName;
	private Boolean useLazyLoading = true;
	public List<IMappedQuery> queries = new ArrayList<IMappedQuery>();
	private Boolean hasReadAccessRights = false;

	private MappedField reverseMappedField = null;
	public MappedField getReverseMappedField() {
		return reverseMappedField;
	}

	public void setReverseMappedField(MappedField reverseMappedField) {
		this.reverseMappedField = reverseMappedField;
		
		if (this.reverseMappedField != null) {
			if(this.reverseMappedField.getReverseMappedField() == null) {
				this.reverseMappedField.setReverseMappedField(this);
			}
		}
	}
	
	public MappedClass getMappedClass() {
		return mappedClass;
	}

	public void setMappedClass(MappedClass mappedClass) {
		this.mappedClass = mappedClass;
	}
	
	public Boolean getHasReadAccessRights() {
		return hasReadAccessRights;
	}

	public void setHasReadAccessRights(Boolean hasReadAccessRights) {
		this.hasReadAccessRights = hasReadAccessRights;
	}

	public Field getField() {
		return field;
	}

	public void setField(Field value) {
		field = value;
	}

	public Method getGetter() {
		return getter;
	}

	public void setGetter(Method value) {
		getter = value;
	}

	public Method getSetter() {
		return setter;
	}

	public void setSetter(Method value) {
		setter = value;
	}

	public String getColumnName() {
		return columnName;
	}

	public void setColumnName(String columnName) {
		this.columnName = columnName;
	}
	
	public String getJoinColumnName() {
		return joinColumnName;
	}
	
	public void setJoinColumnName(String joinColumnName) {
		this.joinColumnName = joinColumnName;
	}
	
	public Boolean getUseLazyLoading() {
		return true;
		// TODO: Lazy Loading is always used. Need better method to NOT use lazy-loading.
		//return this.useLazyLoading;
	}
	
	public void setUseLazyLoading(Boolean value) {
		this.useLazyLoading = value;
	}

	private IMappedQuery readQuery = null;
	public IMappedQuery getReadQuery() {
		return readQuery;
	}

	public void setReadQuery(IMappedQuery readQuery) {
		this.readQuery = readQuery;
	}

	public void readJsonField(JsonObject jsonObject, Object anObject)
			throws IllegalArgumentException, IllegalAccessException,
			InvocationTargetException, ClassNotFoundException, IOException {
		JsonElement elementField = jsonObject.get(getField().getName());
		if (elementField.isJsonObject()) {
			JsonObject elementFieldObject = elementField.getAsJsonObject();
			Iterator<Entry<String,JsonElement>> iterator = elementFieldObject.entrySet().iterator();
			Map<String, Object> map = new HashMap<String, Object>();
			while(iterator.hasNext()) {
				Entry<String,JsonElement> nextEntry = iterator.next();
				JsonElement nextEntryElement = nextEntry.getValue();
				map.put(nextEntry.getKey(), "");
			}
			getSetter().invoke(anObject, map);
		}
		else {
			String elementFieldValue = elementField.getAsString();
			getSetter().invoke(anObject, elementFieldValue);
		}
	}
	
	public void writeJsonField(JsonObject jsonObject, Object anObject)
			throws IllegalArgumentException, IOException,
			IllegalAccessException, InvocationTargetException {
		Object fieldValue = null;
		fieldValue = getValue(anObject);

		if (fieldValue == null)
			jsonObject.addProperty(getField().getName(), "");
		else
			jsonObject.addProperty(getField().getName(), fieldValue.toString());
	}

	public void readExternalField(ObjectInput input, Object anObject)
			throws IllegalArgumentException, IllegalAccessException,
			InvocationTargetException, ClassNotFoundException, IOException {
		getSetter().invoke(anObject, input.readObject());
	}

	public void writeExternalField(ObjectOutput output, Object anObject)
			throws IllegalArgumentException, IOException,
			IllegalAccessException, InvocationTargetException {
		output.writeObject(getGetter().invoke(anObject));
	}

	public Object getValue(Object anObject) throws IllegalArgumentException,
			IllegalAccessException, InvocationTargetException {
		if(anObject == null) return null;
		Object result = getGetter().invoke(anObject);
		return result;
	}
	
	public Boolean isValueSetForQuery(Object anObject) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		Object value = getValue(anObject);
		return (value != null);
	}
	
	public Boolean compareObjects(Object objectA, Object objectB) throws IllegalArgumentException,
	IllegalAccessException, InvocationTargetException {
		Object valueA = getValue(objectA);
		Object valueB = getValue(objectB);
		
		if (valueA == null && valueB == null)
			return true;
		else if (valueA == null || valueB == null)
			return false;
		else
			return valueA.equals(valueB);
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		else if (obj.getClass() != this.getClass())
			return false;
		
		MappedField mfObj = (MappedField) obj;
		
		if (this.field == null && mfObj.field == null)
			return true;
		else if (this.field == null || mfObj.field == null)
			return false;
		else if (!this.field.equals(mfObj.field))
			return false;
		else {
			if (mfObj.getMappedClass() == null && this.getMappedClass() == null)
				return true;
			else if (mfObj.getMappedClass() == null || this.getMappedClass() == null)
				return false;
			else
				return this.getMappedClass().equals(mfObj.getMappedClass());
		}		
	}
}
