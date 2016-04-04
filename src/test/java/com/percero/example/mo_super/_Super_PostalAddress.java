package com.percero.example.mo_super;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import javax.persistence.OneToOne;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonDeserialize;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import com.google.gson.JsonObject;
import com.percero.agents.sync.metadata.MappedClass;
import com.percero.agents.sync.metadata.MappedClass.MappedClassMethodPair;
import com.percero.agents.sync.vo.BaseDataObject;
import com.percero.example.Country;
import com.percero.example.CountryPermit;
import com.percero.serial.BDODeserializer;
import com.percero.serial.BDOSerializer;
import com.percero.serial.JsonUtils;

@MappedSuperclass
//@Inheritance(strategy=InheritanceType.TABLE_PER_CLASS)
@Inheritance(strategy=InheritanceType.JOINED)
/*
*/
public class _Super_PostalAddress extends BaseDataObject implements Serializable
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
	private String address1;
	public String getAddress1() {
		return this.address1;
	}
	public void setAddress1(String value)
	{
		this.address1 = value;
	}

	@Column
    @com.percero.agents.sync.metadata.annotations.Externalize
	private String address2;
	public String getAddress2() {
		return this.address2;
	}
	public void setAddress2(String value)
	{
		this.address2 = value;
	}

	@Column
    @com.percero.agents.sync.metadata.annotations.Externalize
	private String address3;
	public String getAddress3() {
		return this.address3;
	}
	public void setAddress3(String value)
	{
		this.address3 = value;
	}

	@Column
    @com.percero.agents.sync.metadata.annotations.Externalize
	private String city;
	public String getCity() {
		return this.city;
	}
	public void setCity(String value)
	{
		this.city = value;
	}

	@Column
    @com.percero.agents.sync.metadata.annotations.Externalize
	private String state;
	public String getState() {
		return this.state;
	}
	public void setState(String value)
	{
		this.state = value;
	}

	@Column
    @com.percero.agents.sync.metadata.annotations.Externalize
	private String postalCode;
	public String getPostalCode() {
		return this.postalCode;
	}
	public void setPostalCode(String value)
	{
		this.postalCode = value;
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

	@Column
    @com.percero.agents.sync.metadata.annotations.Externalize
	private Double latitude;
	public Double getLatitude() {
		return this.latitude;
	}
	public void setLatitude(Double value)
	{
		this.latitude = value;
	}

	@Column
    @com.percero.agents.sync.metadata.annotations.Externalize
	private Double longitude;
	public Double getLongitude() {
		return this.longitude;
	}
	public void setLongitude(Double value)
	{
		this.longitude = value;
	}

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
	private String countyName;
	public String getCountyName() {
		return this.countyName;
	}
	public void setCountyName(String value)
	{
		this.countyName = value;
	}


	//////////////////////////////////////////////////////
	// Source Relationships
	//////////////////////////////////////////////////////
    @com.percero.agents.sync.metadata.annotations.Externalize
	@JsonSerialize(using=BDOSerializer.class)
	@JsonDeserialize(using=BDODeserializer.class)
	@JoinColumn(name="country_ID")
	@org.hibernate.annotations.ForeignKey(name="FK_Country_country_TO_PostalAddress")
	@ManyToOne(fetch=FetchType.LAZY)
	private Country country;
	public Country getCountry() {
		return this.country;
	}
	public void setCountry(Country value) {
		this.country = value;
	}


	//////////////////////////////////////////////////////
	// Target Relationships
	//////////////////////////////////////////////////////
	/**
	 *  The address of the entiy that issued the import permit.  Used to determing the "Issuing County" for Packing Lists.  
	 */
	@JsonSerialize(using=BDOSerializer.class)
	@JsonDeserialize(using=BDODeserializer.class)
    @com.percero.agents.sync.metadata.annotations.Externalize
	@JoinColumn(name="countryPermit_ID")
	@org.hibernate.annotations.ForeignKey(name="FK_PostalAddress_countryPermit_CountryPermit")
	@OneToOne(fetch=FetchType.LAZY, mappedBy="issuerAddress", cascade=javax.persistence.CascadeType.REMOVE)
	private CountryPermit countryPermit;
	public CountryPermit getCountryPermit() {
		return this.countryPermit;
	}
	public void setCountryPermit(CountryPermit value) {
		this.countryPermit = value;
	}



	
	//////////////////////////////////////////////////////
	// JSON
	//////////////////////////////////////////////////////
	@Override
	public String retrieveJson(ObjectMapper objectMapper) {
		StringBuilder objectJson = new StringBuilder(super.retrieveJson(objectMapper));

		// Properties
		objectJson.append(",\"address1\":");
		if (getAddress1() == null)
			objectJson.append("null");
		else {
			if (objectMapper == null)
				objectMapper = new ObjectMapper();
			try {
				objectJson.append(objectMapper.writeValueAsString(getAddress1()));
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

		objectJson.append(",\"address2\":");
		if (getAddress2() == null)
			objectJson.append("null");
		else {
			if (objectMapper == null)
				objectMapper = new ObjectMapper();
			try {
				objectJson.append(objectMapper.writeValueAsString(getAddress2()));
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

		objectJson.append(",\"address3\":");
		if (getAddress3() == null)
			objectJson.append("null");
		else {
			if (objectMapper == null)
				objectMapper = new ObjectMapper();
			try {
				objectJson.append(objectMapper.writeValueAsString(getAddress3()));
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

		objectJson.append(",\"city\":");
		if (getCity() == null)
			objectJson.append("null");
		else {
			if (objectMapper == null)
				objectMapper = new ObjectMapper();
			try {
				objectJson.append(objectMapper.writeValueAsString(getCity()));
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

		objectJson.append(",\"state\":");
		if (getState() == null)
			objectJson.append("null");
		else {
			if (objectMapper == null)
				objectMapper = new ObjectMapper();
			try {
				objectJson.append(objectMapper.writeValueAsString(getState()));
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

		objectJson.append(",\"postalCode\":");
		if (getPostalCode() == null)
			objectJson.append("null");
		else {
			if (objectMapper == null)
				objectMapper = new ObjectMapper();
			try {
				objectJson.append(objectMapper.writeValueAsString(getPostalCode()));
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

		objectJson.append(",\"latitude\":");
		if (getLatitude() == null)
			objectJson.append("null");
		else {
			objectJson.append(getLatitude());
		}

		objectJson.append(",\"longitude\":");
		if (getLongitude() == null)
			objectJson.append("null");
		else {
			objectJson.append(getLongitude());
		}

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

		objectJson.append(",\"countyName\":");
		if (getCountyName() == null)
			objectJson.append("null");
		else {
			if (objectMapper == null)
				objectMapper = new ObjectMapper();
			try {
				objectJson.append(objectMapper.writeValueAsString(getCountyName()));
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
		objectJson.append(",\"country\":");
		if (getCountry() == null)
			objectJson.append("null");
		else {
			try {
				objectJson.append(((BaseDataObject) getCountry()).toEmbeddedJson());
			} catch(Exception e) {
				objectJson.append("null");
			}
		}

		// Target Relationships
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

		
		return objectJson.toString();
	}

	@Override
	protected void fromJson(JsonObject jsonObject) {
	    super.fromJson(jsonObject);

		// Properties
		setAddress1(JsonUtils.getJsonString(jsonObject, "address1"));
		setAddress2(JsonUtils.getJsonString(jsonObject, "address2"));
		setAddress3(JsonUtils.getJsonString(jsonObject, "address3"));
		setCity(JsonUtils.getJsonString(jsonObject, "city"));
		setState(JsonUtils.getJsonString(jsonObject, "state"));
		setPostalCode(JsonUtils.getJsonString(jsonObject, "postalCode"));
		setName(JsonUtils.getJsonString(jsonObject, "name"));
		setLatitude(JsonUtils.getJsonDouble(jsonObject, "latitude"));
		setLongitude(JsonUtils.getJsonDouble(jsonObject, "longitude"));
		setAbbreviation(JsonUtils.getJsonString(jsonObject, "abbreviation"));
		setCountyName(JsonUtils.getJsonString(jsonObject, "countyName"));

		// Source Relationships
        this.country = JsonUtils.getJsonPerceroObject(jsonObject, "country");

		// Target Relationships
		this.countryPermit = JsonUtils.getJsonPerceroObject(jsonObject, "countryPermit");
	}

	@Override
	protected List<MappedClassMethodPair> getListSetters() {
		List<MappedClassMethodPair> listSetters = super.getListSetters();

		// Target Relationships
		listSetters.add(MappedClass.getFieldSetters(CountryPermit.class, "issuerAddress"));
	
		return listSetters;
	}
}