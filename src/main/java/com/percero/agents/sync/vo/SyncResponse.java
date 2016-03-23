package com.percero.agents.sync.vo;

import java.io.IOException;
import java.util.Date;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.annotate.JsonTypeInfo;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;


@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="cn")
public class SyncResponse {
	private String clientId = "";
	public String getClientId() {
		return clientId;
	}
	public void setClientId(String clientId) {
		this.clientId = clientId;
	}
	
	private Long timestamp = (new Date()).getTime();
	public Long getTimestamp() {
		return timestamp;
	}
	public void setTimestamp(Long value) {
		timestamp = value;
	}

/*	private ClassIDPair classIdPair;
	public ClassIDPair getClassIdPair() {
		return classIdPair;
	}
	public void setClassIdPair(ClassIDPair classIdPair) {
		this.classIdPair = classIdPair;
	}
	*/
	private String[] ids;
	public String[] getIds() {
		return ids;
	}
	public void setIds(String[] ids) {
		this.ids = ids;
	}
	
	private Object data = null;
	public Object getData() {
		return data;
	}
	public void setData(Object data) {
		this.data = data;
	}
	
	private String type;
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	
	private String correspondingMessageId;
	public String getCorrespondingMessageId() {
		return correspondingMessageId;
	}
	public void setCorrespondingMessageId(String correspondingMessageId) {
		this.correspondingMessageId = correspondingMessageId;
	}
	
	private String gatewayMessageId;
	public String getGatewayMessageId() {
		return gatewayMessageId;
	}
	public void setGatewayMessageId(String gatewayMessageId) {
		this.gatewayMessageId = gatewayMessageId;
	}

	
	
	///////////////////////////////////////////////
	//	JSON
	///////////////////////////////////////////////
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
		return retrieveBaseJson(objectMapper);
	}
	
	public String retrieveBaseJson(ObjectMapper objectMapper) {
		StringBuilder objectJson = new StringBuilder("\"cn\":\"").append(getClass().getCanonicalName()).append("\",")
				.append("\"clientId\":");
		
		if (getClientId() == null) {
			objectJson.append("null");
		}
		else {
			objectJson.append('"').append(getClientId()).append('"');
		}

		objectJson.append(',').append("\"timestamp\":").append(getTimestamp().toString()).append(',');
		
		if (getIds() != null) {
			objectJson.append("\"ids\":[");
			int idsCounter = 0;
			for(String nextId : getIds()) {
				if (idsCounter > 0)
					objectJson.append(',');
				objectJson.append('"').append(nextId).append('"');
				idsCounter++;
			}
			objectJson.append("],");
		}
		else {
			objectJson.append("\"ids\":null,");
		}
		
		try {
			if (objectMapper == null)
				objectMapper = new ObjectMapper();
			objectJson.append("\"data\":").append((getData() == null ? "null" : objectMapper.writeValueAsString(getData()))).append(',');
		} catch (JsonGenerationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			objectJson.append("\"data\":null");
		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			objectJson.append("\"data\":null");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			objectJson.append("\"data\":null");
		}				

		objectJson.append("\"type\":").append((getType() == null ? "null" : "\"" + getType() + "\"")).append(',')
				.append("\"correspondingMessageId\":");
		
		if (getCorrespondingMessageId() == null) {
			objectJson.append("null");
		}
		else {
			objectJson.append('"').append(getCorrespondingMessageId()).append('"');
		}
		objectJson.append(',').append("\"gatewayMessageId\":");
		
		if (getGatewayMessageId() == null) {
			objectJson.append("null");
		}
		else {
			objectJson.append('"').append(getGatewayMessageId()).append('"');
		}
		
		return objectJson.toString();
	}
}
