package com.percero.example;

import com.google.gson.JsonObject;
import com.percero.agents.auth.vo.IUserRole;
import com.percero.agents.sync.metadata.annotations.EntityInterface;
import com.percero.agents.sync.metadata.annotations.Externalize;
import com.percero.agents.sync.metadata.annotations.PropertyInterface;
import com.percero.agents.sync.metadata.annotations.RelationshipInterface;
import com.percero.agents.sync.vo.BaseDataObject;
import com.percero.serial.JsonUtils;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

import javax.persistence.*;
import java.io.IOException;
import java.io.Serializable;
import java.util.Date;

@EntityInterface(interfaceClass=IUserRole.class)
@Entity
public class PersonRole extends BaseDataObject implements Serializable, IUserRole
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
    @PropertyInterface(entityInterfaceClass=IUserRole.class, propertyName="roleName",params={})
    @Externalize
    private String roleName;
    public String getRoleName() {
        return this.roleName;
    }
    public void setRoleName(String value)
    {
        this.roleName = value;
    }


    //////////////////////////////////////////////////////
    // Source Relationships
    //////////////////////////////////////////////////////
    @RelationshipInterface(entityInterfaceClass=IUserRole.class, sourceVarName="userAnchor")
    @Externalize
    @JoinColumn(name="person_ID")
    @org.hibernate.annotations.ForeignKey(name="FK_User_user_TO_UserRole")
    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    private Person person;
    public Person getPerson() {
        return this.person;
    }
    public void setPerson(Person value) {
        this.person = value;
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

        objectJson += ",\"roleName\":";
        if (getRoleName() == null)
            objectJson += "null";
        else {
            if (objectMapper == null)
                objectMapper = new ObjectMapper();
            try {
                objectJson += objectMapper.writeValueAsString(getRoleName());
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
        setRoleName(JsonUtils.getJsonString(jsonObject, "roleName"));

        // Source Relationships
        this.person = JsonUtils.getJsonPerceroObject(jsonObject, "person");

        // Target Relationships
    }
}

