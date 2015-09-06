package com.percero.example;

import com.google.gson.JsonObject;
import com.percero.agents.auth.vo.IUserAnchor;
import com.percero.agents.sync.metadata.annotations.EntityInterface;
import com.percero.agents.sync.metadata.annotations.Externalize;
import com.percero.agents.sync.metadata.annotations.PropertyInterface;
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
    }

}
