package com.percero.agents.auth.vo;

import java.util.HashMap;
import java.util.Map;

import org.boon.core.value.CharSequenceValue;
import org.boon.json.ObjectMapper;
import org.boon.json.implementation.ObjectMapperImpl;

import com.google.common.base.CaseFormat;

/**
 * Created by jonnysamps on 8/27/15.
 */
public class BasicAuthCredential {

	public static final String FIRST_NAME = "firstName";
	public static final String LAST_NAME = "lastName";
	public static final String EMAIL = "email";

	private String username;
    public String getUsername(){
        return username;
    }
    public void setUsername(String val){
        this.username = val;
    }

    private String password;
    public String getPassword(){
        return password;
    }
    public void setPassword(String val){
        this.password = val;
    }
    
    private Map<String, Object> metadata;
    public Map<String, Object> getMetadata() {
    	return metadata;
    }
    public void setMetadata(Map<String, Object> metadata) {
    	this.metadata = metadata;
    }

    public BasicAuthCredential(){
    	
    }
    
    public BasicAuthCredential(String username, String password){
    	this.username = username;
    	this.password = password;
    	this.metadata = new HashMap<String, Object>();
    }
    
    public BasicAuthCredential(String username, String password, Map<String, Object> metadata){
        this.username = username;
        this.password = password;
        this.metadata = metadata;
    }

    /**
     * Used to deserialize from "<USERNAME>:<PASSWORD>" format to an object
     * @param credential
     * @return
     */
    public static BasicAuthCredential fromString(String credential){
        BasicAuthCredential result = new BasicAuthCredential("","");
        String[] parts = credential.split(":");
        if(parts.length == 2) {
            result = new BasicAuthCredential(parts[0],parts[1]);
        }
        return result;
    }
    
    /**
     * Used to deserialize from `{"username":<USERNAME>, "password":<PASSWORD>}` format to an object
     * @param credential
     * @return
     */
    public static BasicAuthCredential fromJsonString(String jsonCredential){
    	ObjectMapper om = new ObjectMapperImpl();
    	BasicAuthCredential result = om.fromJson(jsonCredential, BasicAuthCredential.class);
    	return result;
    }
	
    public String retrieveMetadataString(String keyName) {
    	return BasicAuthCredential.retrieveMetadataString(getMetadata(), keyName);
    }

    public static String retrieveMetadataString(Map<String, Object> metadata, String keyName) {
    	return retrieveMetadataString(metadata, keyName, false);
    }
    public static String retrieveMetadataString(Map<String, Object> metadata, String keyName, boolean caseSensitive) {
		String result = "";
		if (metadata != null && keyName != null) {
			// We won't allow for non-casesensitive lookups on large datasets.
			if (caseSensitive || metadata.size() > 1000) {
				Object value = metadata.get(keyName);
				if (value != null) {
					if (value instanceof String) {
						result = (String) value;
					}
					else if (value instanceof CharSequenceValue) {
						CharSequenceValue charSequenceValue = (CharSequenceValue) value;
						result = charSequenceValue.stringValue();
					}
				}
			}
			else {
				// Sort of defeats the purpose of a Map, but we want to allow
				// flexibility in this case, so we iterate through the entry set
				// of the Map to see if we can find a non-casesensitive match.
				String keyLowerUnderscore = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, keyName);
				for(Map.Entry<String, Object> nextEntry : metadata.entrySet()) {
					String nextKey = nextEntry.getKey();
					if (keyName.equalsIgnoreCase(nextKey) || keyLowerUnderscore.equalsIgnoreCase(nextKey)) {
						// We have found our match.
						Object value = nextEntry.getValue();
						if (value != null) {
							if (value instanceof String) {
								result = (String) value;
							}
							else if (value instanceof CharSequenceValue) {
								CharSequenceValue charSequenceValue = (CharSequenceValue) value;
								result = charSequenceValue.stringValue();
							}
						}
					}
				}
			}
		}
		
		return result;
	}
}
