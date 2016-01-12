package com.percero.example;

import com.google.gson.JsonObject;
import com.percero.agents.sync.metadata.annotations.Externalize;
import com.percero.agents.sync.vo.BaseDataObject;
import com.percero.serial.JsonUtils;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import java.io.IOException;
import java.io.Serializable;

@Entity
public class Block extends BaseDataObject implements Serializable{

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

        return objectJson;
    }

    @Override
    protected void fromJson(JsonObject jsonObject) {
        super.fromJson(jsonObject);

        // Properties
        setColor(JsonUtils.getJsonString(jsonObject, "color"));
    }
}
