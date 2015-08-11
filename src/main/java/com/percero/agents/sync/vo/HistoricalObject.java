/**
 * This is a generated class and is not intended for modification.  To customize behavior
 * of this value object you may modify the generated sub-class of this class - Person.java.
 */

package com.percero.agents.sync.vo;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.ObjectMapper;


@Entity
public class HistoricalObject extends BaseDataObject
{
	@Id
	@Column(unique=true)
	private String ID;
	@JsonProperty(value="ID")
	public String getID() {
		return this.ID;
	}
	@JsonProperty(value="ID")
	public void setID(String value) {
		this.ID = value;
	}

    /*
     * Property getters and setters
     */
	@Column
	private Date objectChangeDate;
	public Date getObjectChangeDate() {
		return this.objectChangeDate;
	}
	public void setObjectChangeDate(Date value) {
		this.objectChangeDate = value;
	}

	@Column
	private String objectChangerId;
	public String getObjectChangerId() {
		return this.objectChangerId;
	}
	public void setObjectChangerId(String value) {
		this.objectChangerId = value;
	}
	
	@Column
	private String objectVersion;
	public String getObjectVersion() {
		return this.objectVersion;
	}
	public void setObjectVersion(String value) {
		this.objectVersion = value;
	}
	
	@Column
	private String objectId;
	public String getObjectId() {
		return this.objectId;
	}
	public void setObjectId(String value) {
		this.objectId = value;
	}
	
	@Column
	private String objectClassName;
	public String getObjectClassName() {
		return this.objectClassName;
	}
	public void setObjectClassName(String value) {
		this.objectClassName = value;
	}
	
	@Column
	private String objectData;
	public String getObjectData() {
		return this.objectData;
	}
	public void setObjectData(String value) {
		this.objectData = value;
	}


	
	//////////////////////////////////////////////////////
	// JSON
	//////////////////////////////////////////////////////
	@Override
	public String retrieveJson(ObjectMapper objectMapper) {
		String objectJson = super.retrieveJson(objectMapper);

		// Properties
		objectJson += ",\"objectChangeDate\":";
		if (getObjectChangeDate() == null)
			objectJson += "null";
		else {
			objectJson += getObjectChangeDate().getTime();
		}
		objectJson += "";

		objectJson += ",\"objectChangerId\":";
		if (getObjectChangerId() == null)
			objectJson += "null";
		else {
			objectJson += "\"" + getObjectChangerId() + "\"";
		}
		objectJson += "";

		objectJson += ",\"objectVersion\":";
		if (getObjectVersion() == null)
			objectJson += "null";
		else {
			objectJson += "\"" + getObjectVersion() + "\"";
		}
		objectJson += "";
		
		objectJson += ",\"objectId\":";
		if (getObjectId() == null)
			objectJson += "null";
		else {
			objectJson += "\"" + getObjectId() + "\"";
		}
		objectJson += "";
		
		objectJson += ",\"objectClassName\":";
		if (getObjectClassName() == null)
			objectJson += "null";
		else {
			objectJson += "\"" + getObjectClassName() + "\"";
		}
		objectJson += "";
		
		objectJson += ",\"objectData\":";
		if (getObjectData() == null)
			objectJson += "null";
		else {
			objectJson += "\"" + getObjectData() + "\"";
		}
		objectJson += "";
		
		return objectJson;
	}
}

