package com.percero.example;

import com.google.gson.JsonObject;
import com.percero.agents.auth.vo.IUserIdentifier;
import com.percero.agents.sync.metadata.annotations.*;
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


@EntityInterface(interfaceClass=IUserIdentifier.class)
@Entity
public class Email extends BaseDataObject implements Serializable, IUserIdentifier
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

    //////////////////////////////////////////////////////
    // Properties
    //////////////////////////////////////////////////////
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
    @PropertyInterface(entityInterfaceClass=IUserIdentifier.class, propertyName="userIdentifier",
            params={@PropertyInterfaceParam(name="paradigm", value="email")}
    )
    @Externalize
    private String value;
    public String getValue() {
        return this.value;
    }
    public void setValue(String value)
    {
        this.value = value;
    }


    //////////////////////////////////////////////////////
    // Source Relationships
    //////////////////////////////////////////////////////
    @RelationshipInterface(entityInterfaceClass=IUserIdentifier.class, sourceVarName="userAnchor")
    @Externalize
    @JsonSerialize(using=BDOSerializer.class)
    @JsonDeserialize(using=BDODeserializer.class)
    @JoinColumn(name="user_ID")
    @org.hibernate.annotations.ForeignKey(name="FK_Person_person_TO_Email")
    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    private Person person;
    public Person getPerson() {
        return this.person;
    }
    public void setPerson(Person value) {
        this.person = value;
    }

    //////////////////////////////////////////////////////
    // Target Relationships
    //////////////////////////////////////////////////////



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

        objectJson += ",\"value\":";
        if (getValue() == null)
            objectJson += "null";
        else {
            if (objectMapper == null)
                objectMapper = new ObjectMapper();
            try {
                objectJson += objectMapper.writeValueAsString(getValue());
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

        // Source Relationships
        objectJson += ",\"person\":";
        if (getPerson() == null)
            objectJson += "null";
        else {
            try {
                objectJson += getPerson().toEmbeddedJson();
            } catch(Exception e) {
                objectJson += "null";
            }
        }
        objectJson += "";

        // Target Relationships

        return objectJson;
    }

    @Override
    protected void fromJson(JsonObject jsonObject) {
        super.fromJson(jsonObject);

        // Properties
        setDateCreated(JsonUtils.getJsonDate(jsonObject, "dateCreated"));
        setDateModified(JsonUtils.getJsonDate(jsonObject, "dateModified"));
        setValue(JsonUtils.getJsonString(jsonObject, "value"));

        // Source Relationships
        this.person = JsonUtils.getJsonPerceroObject(jsonObject, "person");

        // Target Relationships
    }
}
