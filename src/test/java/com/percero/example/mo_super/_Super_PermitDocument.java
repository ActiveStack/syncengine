package com.percero.example.mo_super;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.MappedSuperclass;
import javax.persistence.OneToOne;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonDeserialize;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import com.google.gson.JsonObject;
import com.percero.agents.sync.metadata.MappedClass.MappedClassMethodPair;
import com.percero.agents.sync.vo.BaseDataObject;
import com.percero.example.CountryPermit;
import com.percero.serial.BDODeserializer;
import com.percero.serial.BDOSerializer;
import com.percero.serial.JsonUtils;

@MappedSuperclass
/*
*/
public class _Super_PermitDocument extends BaseDataObject implements Serializable
{
	//////////////////////////////////////////////////////
	// VERSION
	//////////////////////////////////////////////////////
	@Override
	public String classVersion() {
		return "0.0.0.0";
	}


	//////////////////////////////////////////////////////
	// ID
	//////////////////////////////////////////////////////
	@Id
    @com.percero.agents.sync.metadata.annotations.Externalize
	@Column(unique=true,name="ID")
	private String ID;
	@JsonProperty(value="ID")
	public String getID() {
		return this.ID;
	}
	@JsonProperty(value="ID")
	public void setID(String value) {
		this.ID = value;
	}
	
	//////////////////////////////////////////////////////
	// Properties
	//////////////////////////////////////////////////////
	@Column
    @com.percero.agents.sync.metadata.annotations.Externalize
	private byte[] documentContent;
	public byte[] getDocumentContent() {
		return this.documentContent;
	}
	public void setDocumentContent(byte[] value)
	{
		this.documentContent = value;
	}


	//////////////////////////////////////////////////////
	// Source Relationships
	//////////////////////////////////////////////////////
    @com.percero.agents.sync.metadata.annotations.Externalize
	@JsonSerialize(using=BDOSerializer.class)
	@JsonDeserialize(using=BDODeserializer.class)
	@JoinColumn(name="countryPermit_ID")
	@org.hibernate.annotations.ForeignKey(name="FK_CountryPermit_countryPermit_TO_PermitDocument")
	@OneToOne(fetch=FetchType.LAZY, optional=false)
	private CountryPermit countryPermit;
	public CountryPermit getCountryPermit() {
		return this.countryPermit;
	}
	public void setCountryPermit(CountryPermit value) {
		this.countryPermit = value;
	}


	//////////////////////////////////////////////////////
	// Target Relationships
	//////////////////////////////////////////////////////


	
	//////////////////////////////////////////////////////
	// JSON
	//////////////////////////////////////////////////////
	@Override
	public String retrieveJson(ObjectMapper objectMapper) {
		StringBuilder objectJson = new StringBuilder(super.retrieveJson(objectMapper));

		// Properties
		objectJson.append(",\"documentContent\":");
		if (getDocumentContent() == null)
			objectJson.append("null");
		else {
			if (objectMapper == null)
				objectMapper = new ObjectMapper();
			try {
				objectJson.append(objectMapper.writeValueAsString(getDocumentContent()));
			} catch (JsonGenerationException e) {
				objectJson.append("null");
				e.printStackTrace();
			} catch (JsonMappingException e) {
				objectJson.append("null");
				e.printStackTrace();
			} catch (IOException e) {
				objectJson.append("null");
				e.printStackTrace();
			}
		}

		// Source Relationships
		objectJson.append(",\"countryPermit\":");
		if (getCountryPermit() == null)
			objectJson.append("null");
		else {
			try {
				objectJson.append(((BaseDataObject) getCountryPermit()).toEmbeddedJson());
			} catch(Exception e) {
				objectJson.append("null");
			}
		}

		// Target Relationships
		
		return objectJson.toString();
	}

	@Override
	protected void fromJson(JsonObject jsonObject) {
	    super.fromJson(jsonObject);

		// Properties
		setDocumentContent(JsonUtils.getJsonByteArray(jsonObject, "documentContent"));

		// Source Relationships
        this.countryPermit = JsonUtils.getJsonPerceroObject(jsonObject, "countryPermit");

		// Target Relationships
	}

	@Override
	protected List<MappedClassMethodPair> getListSetters() {
		List<MappedClassMethodPair> listSetters = super.getListSetters();

		// Target Relationships
	
		return listSetters;
	}
}