package com.percero.example;

import com.google.gson.JsonObject;
import com.percero.agents.sync.metadata.annotations.Externalize;
import com.percero.agents.sync.vo.BaseDataObject;
import com.percero.serial.JsonUtils;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

import javax.persistence.*;
import java.io.IOException;
import java.io.Serializable;

@Entity
public class Circle extends BaseDataObject implements Serializable{

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
    private String color;
    public String getColor() {
        return this.color;
    }
    public void setColor(String value)
    {
        this.color = value;
    }

    //////////////////////////////////////////////////////
    // Source Relationships
    //////////////////////////////////////////////////////
    @Externalize
    @JoinColumn(name="person_ID")
    @org.hibernate.annotations.ForeignKey(name="FK_Person_person_TO_Circle")
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
        objectJson += ",\"color\":";
        if (getColor() == null)
            objectJson += "null";
        else {
            if (objectMapper == null)
                objectMapper = new ObjectMapper();
            try {
                objectJson += objectMapper.writeValueAsString(getColor());
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

        return objectJson;
    }

    @Override
    protected void fromJson(JsonObject jsonObject) {
        super.fromJson(jsonObject);

        // Properties
        setColor(JsonUtils.getJsonString(jsonObject, "color"));

        // Source Relationships
        this.person = JsonUtils.getJsonPerceroObject(jsonObject, "person");
    }
}
