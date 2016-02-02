package com.percero.agents.sync.vo;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.persistence.Transient;

import org.apache.log4j.Logger;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonTypeInfo;
import org.codehaus.jackson.map.ObjectMapper;
import org.mortbay.log.Log;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.percero.agents.sync.exceptions.SyncException;
import com.percero.agents.sync.helpers.ProcessHelper;
import com.percero.agents.sync.manager.DataExternalizer;
import com.percero.agents.sync.metadata.MappedClass;
import com.percero.agents.sync.metadata.MappedClass.MappedClassMethodPair;
import com.percero.agents.sync.services.SyncAgentService;
import com.percero.framework.vo.IPerceroObject;
import com.percero.serial.JsonUtils;

@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="cn")
//@JsonIgnoreProperties(ignoreUnknown=true)
public class BaseDataObject implements Externalizable, IPerceroObject, IJsonObject, Comparable<BaseDataObject> {
	
	private static final Logger log = Logger.getLogger(BaseDataObject.class);

	public static final String DATA_SOURCE_CACHE="CACHE";

	public static final String DATA_SOURCE_DATA_STORE="DATA_STORE";
	
	public BaseDataObject() {
		super();
	}

	private String ID = "";
	public String getID() {
		return ID;
	}
	public void setID(String value) {
		ID = value;
	}
	
	@Transient
	transient private Boolean isClean = false;
	public final Boolean getIsClean() {
		return isClean;
	}
	public final void setIsClean(Boolean isClean) {
		this.isClean = isClean;
	}

	@Transient
	transient private String dataSource = DATA_SOURCE_DATA_STORE;
	public final String getDataSource() {
		return dataSource;
	}
	public final void setDataSource(String value) {
		this. dataSource = value;
	}

	
	public String classVersion() {
		return null;
	}
	
	public void readExternal(ObjectInput in) throws IOException,
			ClassNotFoundException {
    	DataExternalizer.readExternal(in, this);
	}

	public void writeExternal(ObjectOutput out) throws IOException {
    	DataExternalizer.writeExternal(out, this);
	}
	
	public String toJson() {
		return toJson(true, null);
	}
	
	public String toJson(ObjectMapper objectMapper) {
		return toJson(true, objectMapper);
	}

	public String toJson(Boolean encloseString, ObjectMapper objectMapper) {
		String objectJson = retrieveJson(objectMapper);
		if (encloseString)
			objectJson = "{" + objectJson + "}";
		return objectJson;
	}
	
	public String retrieveJson(ObjectMapper objectMapper) {
		return retrieveBaseJson();
	}
	
	public String toEmbeddedJson() {
		return toEmbeddedJson(true);
	}
	
	public String toEmbeddedJson(Boolean encloseString) {
		String objectJson = retrieveEmbeddedJson();
		if (encloseString)
			objectJson = "{" + objectJson + "}";
		return objectJson;
	}
	
	public String retrieveBaseJson() {
		String objectJson = "\"cn\":\"" + getClass().getCanonicalName() + "\"," + 
				"\"ID\":\"" + getID() + "\"";
		//+ "\"dataSource\":\"" + getDataSource() +  "\"" ;
		
		return objectJson;
	}
	
	public String retrieveEmbeddedJson() {
		String objectJson = "\"className\":\"" + getClass().getCanonicalName() + "\"," + 
				"\"ID\":\"" + getID() + "\"";
		
		return objectJson;
	}
	
	public void fromJson(String jsonString) {
		JsonParser parser = new JsonParser();
		JsonElement element = parser.parse(jsonString);
		JsonObject jsonObject = element.getAsJsonObject();
		
		fromJson(jsonObject);
	}
	protected void fromJson(JsonObject jsonObject) {
		// ID
		this.setID(JsonUtils.getJsonString(jsonObject, "ID"));
//		this.setDataSource(JsonUtils.getJsonString(jsonObject, "dataSource"));
	}
	
	public static ClassIDPair toClassIdPair(BaseDataObject object) {
		if (object != null)
			return new ClassIDPair(object.getID(), object.getClass().getCanonicalName());
		else
			return null;
	}

	public static ClassIDPair toClassIdPair(IPerceroObject object) {
		if (object != null)
			return new ClassIDPair(object.getID(), object.getClass().getCanonicalName());
		else
			return null;
	}
	
	/**
	 * Basic compare by ID
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(BaseDataObject otherBaseDataObject) {
		if (otherBaseDataObject == null) {
			return 1;
		} else {
			String str1 = this.getID();
			String str2 = otherBaseDataObject.getID();
			return str1.compareTo(str2);
		}
	}
	
	public static List<ClassIDPair> sortList(List<? extends BaseDataObject> objectList) {
		List<ClassIDPair> result = new ArrayList<ClassIDPair>();
		Collections.sort(objectList);
		
		Iterator<? extends BaseDataObject> itrInvItemProducts = objectList.iterator();
		while (itrInvItemProducts.hasNext()) {
			BaseDataObject nextObject = itrInvItemProducts.next();
			result.add(BaseDataObject.toClassIdPair(nextObject));
		}
		
		return result;
	}
	
	public static Boolean resultExists(BaseDataObject object, List<? extends BaseDataObject> list) {
		Boolean resultExists = false;
		Iterator<? extends BaseDataObject> itrResults = list.iterator();
		while (itrResults.hasNext()) {
			BaseDataObject nextResult = itrResults.next();
			if (BaseDataObject.toClassIdPair(nextResult).comparePerceroObject(object)) {
				resultExists = true;
				break;
			}
		}
		
		return resultExists;
	}

	// Merging Capabilities
	public final void mergeAndDeleteObjectIntoThis(IPerceroObject objectToMergeAndDelete, String userId, String clientId) throws SyncException {
		if (!objectToMergeAndDelete.getClass().getCanonicalName().equalsIgnoreCase(this.getClass().getCanonicalName())) {
			// Can't merge objects that are two different types.
			throw new SyncException("Cannot merge objects that are two different types: " + objectToMergeAndDelete.getClass().getCanonicalName() + "->" + this.getClass().getCanonicalName(), -112);
		}
		else if (objectToMergeAndDelete.getID().equalsIgnoreCase(this.getID())) {
			// Can't merge objects that are two different types.
			throw new SyncException("Cannot merge the same object: " + this.getClass().getCanonicalName() + ": " + this.getID(), -114);
		}

		List<MappedClassMethodPair> listSetters = getListSetters();
		ProcessHelper.mergeObjects(objectToMergeAndDelete, this, listSetters, userId, clientId);
	}
	
	// Should return a list of all setters that own the relationship between themselves and this object.
	// This method is meant to be overridden by inheriting classes.
	protected List<MappedClassMethodPair> getListSetters() {
		List<MappedClassMethodPair> listSetters = new ArrayList<MappedClass.MappedClassMethodPair>();
		return listSetters;
	}
}
