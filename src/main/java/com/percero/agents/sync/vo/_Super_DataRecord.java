/**
 * This is a generated class and is not intended for modification.  To customize behavior
 * of this value object you may modify the generated sub-class of this class - Email.java.
 */

package com.percero.agents.sync.vo;

import java.io.IOException;
import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.hibernate.annotations.GenericGenerator;


@MappedSuperclass
public class _Super_DataRecord extends BaseDataObject implements Serializable/*, DBObject*/
{
	/*
     * Properties
     */
    private String _ID;
    private String _dataID;
    private Date _dateCreated;
    private Boolean _isDefault;

    /*
     * Property getters and setters
     */


    @Id
	@GeneratedValue(generator="system-uuid")
	@GenericGenerator(name="system-uuid", strategy = "uuid")
	@Column(unique = true)
	@JsonProperty(value="ID")
    public String getID()
    {
        return _ID;
    }

	@JsonProperty(value="ID")
    public void setID(String value)
    {
        _ID = value;
    }


    @Column
    public String getDataID()
    {
        return _dataID;
    }

    public void setDataID(String value)
    {
        _dataID = value;
    }


    @Column
    public Date getDateCreated()
    {
        return _dateCreated;
    }

    public void setDateCreated(Date value)
    {
        _dateCreated = value;
    }


    @Column
    public Boolean getIsDefault()
    {
    	return _isDefault;
    }
    
    public void setIsDefault(Boolean value)
    {
    	_isDefault = value;
    }
    
    
    /**
     * Equals - defined as a comparison of ID properties only.
     */
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof _Super_DataRecord))
            return false;
        final _Super_DataRecord other = (_Super_DataRecord) obj;

        if (_ID == null)
        {
            if (other._ID != null)
            {
                return false;
            }
        }
        else if (!_ID.equals(other._ID))
        {
            return false;
        }

        return true;
    }

    /**
     * Hashcode - defined as a hash of ID properties only.
     */
    public int hashCode()
    {
        final int PRIME = 31;
        int result = 1;

        if ((_ID == null) )
        {
            result = System.identityHashCode(this);
        }
        else
        {
            result = PRIME * result + ((_ID == null) ? 0 : _ID.hashCode());
        }
        return result;
    }


	
	//////////////////////////////////////////////////////
	// JSON
	//////////////////////////////////////////////////////
	@Override
	public String retrieveJson(ObjectMapper objectMapper) {
		String objectJson = super.retrieveJson(objectMapper);

		// Properties
		objectJson += ",\"dataID\":";
		if (getDataID() == null)
			objectJson += "null";
		else {
			if (objectMapper == null)
				objectMapper = new ObjectMapper();
			try {
				objectJson += objectMapper.writeValueAsString(getDataID());
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
		objectJson += "";

		objectJson += ",\"dateCreated\":";
		if (getDateCreated() == null)
			objectJson += "null";
		else {
			objectJson += getDateCreated().getTime();
		}
		objectJson += "";

		objectJson += ",\"isDefault\":";
		if (getIsDefault() == null)
			objectJson += "null";
		else {
			objectJson += getIsDefault();
		}
		objectJson += "";

		// Source Relationships
		// Target Relationships
		
		return objectJson;
	}
	/*
	public Object get(String fieldName) {
		if (fieldName.equals("_id"))
			return getID();
		else if (fieldName.equals("ID"))
			return getID();
		else if (fieldName.equals("isDefault"))
			return getIsDefault();
		else if (fieldName.equals("dateCreated"))
			return getDateCreated();
		else if (fieldName.equals("dataID"))
			return getDataID();
		else
			return null;
	}
	
	public Object put(String fieldName, Object value) {
		try {
			if (fieldName.equals("_id"))
				setID((String) value);
			else if (fieldName.equals("ID"))
				setID((String) value);
			else if (fieldName.equals("isDefault"))
				setIsDefault((Boolean)value);
			else if (fieldName.equals("dateCreated"))
				setDateCreated((Date) value);
			else if (fieldName.equals("dataID"))
				setDataID((String) value);
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		return value;
	}
	
    public Object removeField( String key ){
    	put(key, null);
    	return null;
	}
	
    @SuppressWarnings("unchecked")
    public void putAll( Map m ){
        for ( Map.Entry entry : (Set<Map.Entry>)m.entrySet() ){
            put( entry.getKey().toString() , entry.getValue() );
        }
    }

    public void putAll( BSONObject o ){
        for ( String k : o.keySet() ){
            put( k , o.get( k ) );
        }
   }

    private boolean _isPartialObject;
    public void setPartialObject(boolean value) {
    	_isPartialObject = value;
    }

    public boolean isPartialObject(){
        return _isPartialObject;
    }

    public void markAsPartialObject(){
        _isPartialObject = true;
    }

	public boolean containsField(String fieldName) {
		if (fieldName.equals("ID") || fieldName.equals("_id") || fieldName.equals("isDefault") || fieldName.equals("dateCreated") || fieldName.equals("dataID"))
			return true;
		else
			return false;
	}
	
	public boolean containsKey(String key) {
		return containsField(key);
	}
	
	public Set<String> keySet() {
		HashSet<String> set = new HashSet<String>();
		set.add("ID");
		set.add("dataID");
		set.add("dateCreated");
		set.add("isDefault");
		
		return set;
	}
	
	public Map toMap() {
		HashMap<String, Object> map = new HashMap<String, Object>();
		map.put("ID", getID());
		map.put("dataID", getDataID());
		map.put("dateCreated", getDateCreated());
		map.put("isDefault", getIsDefault());
		
		return map;
		//return new LinkedHashMap<String, Object>(map);
	}*/
}

