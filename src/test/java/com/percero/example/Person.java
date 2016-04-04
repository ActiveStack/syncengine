package com.percero.example;

import com.google.gson.JsonObject;
import com.percero.agents.auth.vo.IUserAnchor;
import com.percero.agents.sync.metadata.MappedClass;
import com.percero.agents.sync.metadata.MappedClass.MappedClassMethodPair;
import com.percero.agents.sync.metadata.annotations.EntityInterface;
import com.percero.agents.sync.metadata.annotations.Externalize;
import com.percero.agents.sync.metadata.annotations.PropertyInterface;
import com.percero.agents.sync.vo.BaseDataObject;
import com.percero.serial.BDODeserializer;
import com.percero.serial.BDOSerializer;
import com.percero.serial.JsonUtils;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonDeserialize;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import javax.persistence.*;
import java.io.IOException;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "Person")
@EntityInterface(interfaceClass=IUserAnchor.class)
public class Person extends BaseDataObject implements Serializable, IUserAnchor
{
    //////////////////////////////////////////////////////
    // VERSION
    //////////////////////////////////////////////////////
    @Override
    public String classVersion() {
        return "0.0.0";
    }

    //////////////////////////////////////////////////////
    // Properties
    //////////////////////////////////////////////////////
    @Id
    @Externalize
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

    @Column
    @Externalize
    private Date dateCreated;
    public Date getDateCreated() {
        return this.dateCreated;
    }
    public void setDateCreated(Date value)
    {
        this.dateCreated = value;
    }

    @Column
    @Externalize
    private Date dateModified;
    public Date getDateModified() {
        return this.dateModified;
    }
    public void setDateModified(Date value)
    {
        this.dateModified = value;
    }

    @Column
    @PropertyInterface(entityInterfaceClass=IUserAnchor.class, propertyName="userId", params={})
    @Externalize
    private String userId;
    public String getUserId() {
        return this.userId;
    }
    public void setUserId(String value)
    {
        this.userId = value;
    }

    @Column
    @PropertyInterface(entityInterfaceClass=IUserAnchor.class, propertyName="firstName", params={})
    @Externalize
    private String firstName;
    public String getFirstName() {
        return this.firstName;
    }
    public void setFirstName(String value)
    {
        this.firstName = value;
    }

    @Column
    @PropertyInterface(entityInterfaceClass=IUserAnchor.class, propertyName="lastName",params={})
    @Externalize
    private String lastName;
    public String getLastName() {
        return this.lastName;
    }
    public void setLastName(String value)
    {
        this.lastName = value;
    }

    @Externalize
    @OneToMany(fetch= FetchType.LAZY, targetEntity=PersonRole.class, mappedBy="person", cascade=javax.persistence.CascadeType.REMOVE)
    private List<PersonRole> personRoles;
    public List<PersonRole> getPersonRoles() {
        return this.personRoles;
    }
    public void setPersonRoles(List<PersonRole> value) {
        this.personRoles = value;
    }

    @Externalize
    @OneToMany(fetch=FetchType.LAZY, targetEntity=Email.class, mappedBy="person", cascade=javax.persistence.CascadeType.REMOVE)
    private List<Email> emails;
    public List<Email> getEmails() {
        return this.emails;
    }
    public void setEmails(List<Email> value) {
        this.emails = value;
    }

    @Externalize
    @OneToMany(fetch=FetchType.LAZY, targetEntity=Circle.class, mappedBy="person", cascade=javax.persistence.CascadeType.REMOVE)
    private List<Circle> circles;
    public List<Circle> getCircles() {
        return this.circles;
    }
    public void setCircles(List<Circle> value) {
        this.circles = value;
    }
    
    
    
	@JsonSerialize(using=BDOSerializer.class)
	@JsonDeserialize(using=BDODeserializer.class)
    @com.percero.agents.sync.metadata.annotations.Externalize
	@OneToOne(fetch=FetchType.LAZY, mappedBy="person", cascade=javax.persistence.CascadeType.REMOVE)
	private PersonDetail personDetail;
	public PersonDetail getPersonDetail() {
		return this.personDetail;
	}
	public void setPersonDetail(PersonDetail value) {
		this.personDetail = value;
	}
    

    //////////////////////////////////////////////////////
    // JSON
    //////////////////////////////////////////////////////
    @Override
    public String retrieveJson(ObjectMapper objectMapper) {
        String objectJson = super.retrieveJson(objectMapper);

        // Properties
        objectJson += ",\"dateCreated\":";
        if (getDateCreated() == null)
            objectJson += "null";
        else {
            objectJson += getDateCreated().getTime();
        }

        objectJson += ",\"dateModified\":";
        if (getDateModified() == null)
            objectJson += "null";
        else {
            objectJson += getDateModified().getTime();
        }

        objectJson += ",\"userId\":";
        if (getUserId() == null)
            objectJson += "null";
        else {
            if (objectMapper == null)
                objectMapper = new ObjectMapper();
            try {
                objectJson += objectMapper.writeValueAsString(getUserId());
            } catch (JsonGenerationException e) {
                objectJson += "null";
                e.printStackTrace();
            } catch (JsonMappingException e) {
                objectJson += "null";
                e.printStackTrace();
            } catch (IOException e) {
                objectJson += "null";
                e.printStackTrace();
            }
        }

        objectJson += ",\"firstName\":";
        if (getFirstName() == null)
            objectJson += "null";
        else {
            if (objectMapper == null)
                objectMapper = new ObjectMapper();
            try {
                objectJson += objectMapper.writeValueAsString(getFirstName());
            } catch (JsonGenerationException e) {
                objectJson += "null";
                e.printStackTrace();
            } catch (JsonMappingException e) {
                objectJson += "null";
                e.printStackTrace();
            } catch (IOException e) {
                objectJson += "null";
                e.printStackTrace();
            }
        }

        objectJson += ",\"lastName\":";
        if (getLastName() == null)
            objectJson += "null";
        else {
            if (objectMapper == null)
                objectMapper = new ObjectMapper();
            try {
                objectJson += objectMapper.writeValueAsString(getLastName());
            } catch (JsonGenerationException e) {
                objectJson += "null";
                e.printStackTrace();
            } catch (JsonMappingException e) {
                objectJson += "null";
                e.printStackTrace();
            } catch (IOException e) {
                objectJson += "null";
                e.printStackTrace();
            }
        }

        objectJson += ",\"personRoles\":[";
        if (getPersonRoles() != null) {
            int personRolesCounter = 0;
            for(PersonRole nextPersonRoles : getPersonRoles()) {
                if (personRolesCounter > 0)
                    objectJson += ",";
                try {
                    objectJson += nextPersonRoles.toEmbeddedJson();
                    personRolesCounter++;
                } catch(Exception e) {
                    // Do nothing.
                }
            }
        }
        objectJson += "]";

        objectJson += ",\"emails\":[";
        if (getEmails() != null) {
            int emailsCounter = 0;
            for(Email email : getEmails()) {
                if (emailsCounter > 0)
                    objectJson += ",";
                try {
                    objectJson += email.toEmbeddedJson();
                    emailsCounter++;
                } catch(Exception e) {
                    // Do nothing.
                }
            }
        }
        objectJson += "]";

        objectJson += ",\"circles\":[";
        if (getCircles() != null) {
            int circlesCounter = 0;
            for(Circle circle : getCircles()) {
                if (circlesCounter > 0)
                    objectJson += ",";
                try {
                    objectJson += circle.toEmbeddedJson();
                    circlesCounter++;
                } catch(Exception e) {
                    // Do nothing.
                }
            }
        }
        objectJson += "]";

		// Target Relationships
		objectJson += ",\"personDetail\":";
		if (getPersonDetail() == null)
			objectJson += "null";
		else {
			try {
				objectJson += ((BaseDataObject) getPersonDetail()).toEmbeddedJson();
			} catch(Exception e) {
				objectJson += "null";
			}
		}


        return objectJson;
    }

    @Override
    protected void fromJson(JsonObject jsonObject) {
        super.fromJson(jsonObject);

        // Properties
        setDateCreated(JsonUtils.getJsonDate(jsonObject, "dateCreated"));
        setDateModified(JsonUtils.getJsonDate(jsonObject, "dateModified"));
        setUserId(JsonUtils.getJsonString(jsonObject, "userId"));
        setFirstName(JsonUtils.getJsonString(jsonObject, "firstName"));
        setLastName(com.percero.serial.JsonUtils.getJsonString(jsonObject, "lastName"));

		this.personRoles = (List<PersonRole>) JsonUtils.getJsonListPerceroObject(jsonObject, "personRoles");
        this.emails = (List<Email>) JsonUtils.getJsonListPerceroObject(jsonObject, "emails");
        this.circles = (List<Circle>) JsonUtils.getJsonListPerceroObject(jsonObject, "circles");
        
        // Target Relationships
        this.personDetail = JsonUtils.getJsonPerceroObject(jsonObject, "personDetail");
    }

	@Override
	protected List<MappedClassMethodPair> getListSetters() {
		List<MappedClassMethodPair> listSetters = super.getListSetters();

		// Target Relationships
		listSetters.add(MappedClass.getFieldSetters(CountryPermit.class, "personDetail"));
	
		return listSetters;
	}

}
