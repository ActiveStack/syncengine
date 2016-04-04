package com.percero.example.mo_super;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

import com.google.gson.JsonObject;
import com.percero.agents.sync.metadata.MappedClass;
import com.percero.agents.sync.metadata.MappedClass.MappedClassMethodPair;
import com.percero.agents.sync.vo.BaseDataObject;
import com.percero.example.CountryPermit;
import com.percero.example.PostalAddress;
import com.percero.serial.JsonUtils;

@MappedSuperclass
@NamedQueries({
	@NamedQuery(name="updateQuery", query="sql:SELECT COUNT(DISTINCT (p.ID)) FROM Person p INNER JOIN PersonRole pr ON p.ID=pr.person_ID WHERE p.userId=:userId AND pr.roleName='PSI Global Admin'"),
	@NamedQuery(name="createQuery", query="sql:SELECT COUNT(DISTINCT (p.ID)) FROM Person p INNER JOIN PersonRole pr ON p.ID=pr.person_ID WHERE p.userId=:userId AND pr.roleName='PSI Global Admin'"),
	@NamedQuery(name="deleteQuery", query="sql:SELECT COUNT(DISTINCT (p.ID)) FROM Person p INNER JOIN PersonRole pr ON p.ID=pr.person_ID WHERE p.userId=:userId AND pr.roleName='PSI Global Admin' AND (SELECT COUNT(ae.ID) FROM AccountingEntity ae WHERE ae.country_ID=:id) = 0 AND (SELECT COUNT(pa.ID) FROM PostalAddress pa WHERE pa.country_ID=:id) = 0 AND (SELECT COUNT(vp.ID) FROM VarietyPatent vp WHERE vp.country_ID=:id) = 0")
})
/*
*/
public class _Super_Country extends BaseDataObject implements Serializable
{
	//////////////////////////////////////////////////////
	// VERSION
	//////////////////////////////////////////////////////
	@Override
	public String classVersion() {
		return "0.0.0";
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
	private String abbreviation;
	public String getAbbreviation() {
		return this.abbreviation;
	}
	public void setAbbreviation(String value)
	{
		this.abbreviation = value;
	}

	@Column
    @com.percero.agents.sync.metadata.annotations.Externalize
	private Boolean requiresPermit;
	public Boolean getRequiresPermit() {
		return this.requiresPermit;
	}
	public void setRequiresPermit(Boolean value)
	{
		this.requiresPermit = value;
	}

	@Column
    @com.percero.agents.sync.metadata.annotations.Externalize
	private String name;
	public String getName() {
		return this.name;
	}
	public void setName(String value)
	{
		this.name = value;
	}


	//////////////////////////////////////////////////////
	// Source Relationships
	//////////////////////////////////////////////////////


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
		objectJson.append(",\"abbreviation\":");
		if (getAbbreviation() == null)
			objectJson.append("null");
		else {
			if (objectMapper == null)
				objectMapper = new ObjectMapper();
			try {
				objectJson.append(objectMapper.writeValueAsString(getAbbreviation()));
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

		objectJson.append(",\"requiresPermit\":");
		if (getRequiresPermit() == null)
			objectJson.append("null");
		else {
			objectJson.append(getRequiresPermit());
		}

		objectJson.append(",\"name\":");
		if (getName() == null)
			objectJson.append("null");
		else {
			if (objectMapper == null)
				objectMapper = new ObjectMapper();
			try {
				objectJson.append(objectMapper.writeValueAsString(getName()));
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

		// Target Relationships

		
		return objectJson.toString();
	}

	@Override
	protected void fromJson(JsonObject jsonObject) {
	    super.fromJson(jsonObject);

		// Properties
		setAbbreviation(JsonUtils.getJsonString(jsonObject, "abbreviation"));
		setRequiresPermit(JsonUtils.getJsonBoolean(jsonObject, "requiresPermit"));
		setName(JsonUtils.getJsonString(jsonObject, "name"));

		// Source Relationships

		// Target Relationships
	}

	@Override
	protected List<MappedClassMethodPair> getListSetters() {
		List<MappedClassMethodPair> listSetters = super.getListSetters();

		// Target Relationships
		listSetters.add(MappedClass.getFieldSetters(CountryPermit.class, "country"));
		listSetters.add(MappedClass.getFieldSetters(PostalAddress.class, "country"));
	
		return listSetters;
	}
}