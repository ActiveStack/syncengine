package com.percero.example;

import java.io.IOException;
import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

import com.google.gson.JsonObject;
import com.percero.agents.auth.vo.IUserRole;
import com.percero.agents.sync.metadata.annotations.Externalize;
import com.percero.agents.sync.metadata.annotations.RelationshipInterface;
import com.percero.agents.sync.vo.BaseDataObject;
import com.percero.serial.JsonUtils;

@Entity
public class PersonDetail extends BaseDataObject implements Serializable
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
    private String detail1;
    public String getDetail1() {
        return this.detail1;
    }
    public void setDetail1(String value)
    {
        this.detail1 = value;
    }


    //////////////////////////////////////////////////////
    // Source Relationships
    //////////////////////////////////////////////////////
    @RelationshipInterface(entityInterfaceClass=IUserRole.class, sourceVarName="userAnchor")
    @Externalize
    @JoinColumn(name="person_ID")
    @org.hibernate.annotations.ForeignKey(name="FK_User_user_TO_UserRole")
    @OneToOne(fetch=FetchType.LAZY, optional=false)
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
        objectJson += ",\"detail1\":";
        if (getDetail1() == null)
            objectJson += "null";
        else {
            if (objectMapper == null)
                objectMapper = new ObjectMapper();
            try {
                objectJson += objectMapper.writeValueAsString(getDetail1());
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
        setDetail1(JsonUtils.getJsonString(jsonObject, "detail1"));

        // Source Relationships
        this.person = JsonUtils.getJsonPerceroObject(jsonObject, "person");

        // Target Relationships
    }
}

