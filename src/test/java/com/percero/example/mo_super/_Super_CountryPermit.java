package com.percero.example.mo_super;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonDeserialize;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import com.google.gson.JsonObject;
import com.percero.agents.sync.metadata.MappedClass;
import com.percero.agents.sync.metadata.MappedClass.MappedClassMethodPair;
import com.percero.agents.sync.vo.BaseDataObject;
import com.percero.example.Country;
import com.percero.example.PermitDocument;
import com.percero.example.PermitLine;
import com.percero.example.PostalAddress;
import com.percero.serial.BDODeserializer;
import com.percero.serial.BDOSerializer;
import com.percero.serial.JsonUtils;

@MappedSuperclass
/*
*/
public class _Super_CountryPermit extends BaseDataObject implements Serializable
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
	private Double quantity;
	public Double getQuantity() {
		return this.quantity;
	}
	public void setQuantity(Double value)
	{
		this.quantity = value;
	}

	@Column
    @com.percero.agents.sync.metadata.annotations.Externalize
	private Date expirationDate;
	public Date getExpirationDate() {
		return this.expirationDate;
	}
	public void setExpirationDate(Date value)
	{
		this.expirationDate = value;
	}


	//////////////////////////////////////////////////////
	// Source Relationships
	//////////////////////////////////////////////////////
    @com.percero.agents.sync.metadata.annotations.Externalize
	@JsonSerialize(using=BDOSerializer.class)
	@JsonDeserialize(using=BDODeserializer.class)
	@JoinColumn(name="country_ID")
	@org.hibernate.annotations.ForeignKey(name="FK_Country_country_TO_CountryPermit")
	@ManyToOne(fetch=FetchType.LAZY, optional=false)
	private Country country;
	public Country getCountry() {
		return this.country;
	}
	public void setCountry(Country value) {
		this.country = value;
	}

    @com.percero.agents.sync.metadata.annotations.Externalize
	@JsonSerialize(using=BDOSerializer.class)
	@JsonDeserialize(using=BDODeserializer.class)
	@JoinColumn(name="issuerAddress_ID")
	@org.hibernate.annotations.ForeignKey(name="FK_PostalAddress_issuerAddress_TO_CountryPermit")
	@OneToOne(fetch=FetchType.LAZY)
	private PostalAddress issuerAddress;
	public PostalAddress getIssuerAddress() {
		return this.issuerAddress;
	}
	public void setIssuerAddress(PostalAddress value) {
		this.issuerAddress = value;
	}


	//////////////////////////////////////////////////////
	// Target Relationships
	//////////////////////////////////////////////////////
    @com.percero.agents.sync.metadata.annotations.Externalize
	@JsonSerialize(contentUsing=BDOSerializer.class)
	@JsonDeserialize(contentUsing=BDODeserializer.class)
	@OneToMany(fetch=FetchType.LAZY, targetEntity=PermitLine.class, mappedBy="countryPermit", cascade=javax.persistence.CascadeType.REMOVE)
	private List<PermitLine> permitLines;
	public List<PermitLine> getPermitLines() {
		return this.permitLines;
	}
	public void setPermitLines(List<PermitLine> value) {
		this.permitLines = value;
	}

	@JsonSerialize(using=BDOSerializer.class)
	@JsonDeserialize(using=BDODeserializer.class)
    @com.percero.agents.sync.metadata.annotations.Externalize
	@JoinColumn(name="permitDocument_ID")
	@org.hibernate.annotations.ForeignKey(name="FK_CountryPermit_permitDoc_PermitDocument")
	@OneToOne(fetch=FetchType.LAZY, mappedBy="countryPermit", cascade=javax.persistence.CascadeType.REMOVE)
	private PermitDocument permitDocument;
	public PermitDocument getPermitDocument() {
		return this.permitDocument;
	}
	public void setPermitDocument(PermitDocument value) {
		this.permitDocument = value;
	}



	
	//////////////////////////////////////////////////////
	// JSON
	//////////////////////////////////////////////////////
	@Override
	public String retrieveJson(ObjectMapper objectMapper) {
		StringBuilder objectJson = new StringBuilder(super.retrieveJson(objectMapper));

		// Properties
		objectJson.append(",\"quantity\":");
		if (getQuantity() == null)
			objectJson.append("null");
		else {
			objectJson.append(getQuantity());
		}

		objectJson.append(",\"expirationDate\":");
		if (getExpirationDate() == null)
			objectJson.append("null");
		else {
			objectJson.append(getExpirationDate().getTime());
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

		objectJson.append(",\"issuerAddress\":");
		if (getIssuerAddress() == null)
			objectJson.append("null");
		else {
			try {
				objectJson.append(((BaseDataObject) getIssuerAddress()).toEmbeddedJson());
			} catch(Exception e) {
				objectJson.append("null");
			}
		}

		// Target Relationships
		objectJson.append(",\"permitLines\":[");
		if (getPermitLines() != null) {
			int permitLinesCounter = 0;
			for(PermitLine nextPermitLines : getPermitLines()) {
				if (permitLinesCounter > 0)
					objectJson.append(',');
				try {
					objectJson.append(((BaseDataObject) nextPermitLines).toEmbeddedJson());
					permitLinesCounter++;
				} catch(Exception e) {
					// Do nothing.
				}
			}
		}
		objectJson.append(']');

		objectJson.append(",\"permitDocument\":");
		if (getPermitDocument() == null)
			objectJson.append("null");
		else {
			try {
				objectJson.append(((BaseDataObject) getPermitDocument()).toEmbeddedJson());
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
		setQuantity(JsonUtils.getJsonDouble(jsonObject, "quantity"));
		setExpirationDate(JsonUtils.getJsonDate(jsonObject, "expirationDate"));

		// Source Relationships
        this.country = JsonUtils.getJsonPerceroObject(jsonObject, "country");
        this.issuerAddress = JsonUtils.getJsonPerceroObject(jsonObject, "issuerAddress");

		// Target Relationships
		this.permitLines = (List<PermitLine>) JsonUtils.getJsonListPerceroObject(jsonObject, "permitLines");
		this.permitDocument = JsonUtils.getJsonPerceroObject(jsonObject, "permitDocument");
	}

	@Override
	protected List<MappedClassMethodPair> getListSetters() {
		List<MappedClassMethodPair> listSetters = super.getListSetters();

		// Target Relationships
		listSetters.add(MappedClass.getFieldSetters(PermitLine.class, "countryPermit"));
		listSetters.add(MappedClass.getFieldSetters(PermitDocument.class, "countryPermit"));
	
		return listSetters;
	}
}