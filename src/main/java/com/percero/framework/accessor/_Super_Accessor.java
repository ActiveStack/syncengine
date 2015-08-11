package com.percero.framework.accessor;

import java.io.Serializable;
import javax.persistence.*;

import org.codehaus.jackson.map.ObjectMapper;

@MappedSuperclass
public class _Super_Accessor implements Serializable
{
    /**
	 * 
	 */
	private static final long serialVersionUID = -7229190351041225995L;
	/*
     * Properties
     */
    private Integer _ID;
    private Boolean _canRead;
    private Boolean _canUpdate;
    private Boolean _canDelete;
    private Boolean _canCreate;

    /*
     * Property getters and setters
     */


    @Column
    @Id
    @GeneratedValue()
    public Integer getID()
    {
        return _ID;
    }

    public void setID(Integer value)
    {
        _ID = value;
    }


    @Column
    public Boolean getCanRead()
    {
        return _canRead;
    }

    public void setCanRead(Boolean value)
    {
        _canRead = value;
    }


    @Column
    public Boolean getCanUpdate()
    {
        return _canUpdate;
    }

    public void setCanUpdate(Boolean value)
    {
        _canUpdate = value;
    }


    @Column
    public Boolean getCanDelete()
    {
        return _canDelete;
    }

    public void setCanDelete(Boolean value)
    {
        _canDelete = value;
    }


    @Column
    public Boolean getCanCreate()
    {
        return _canCreate;
    }

    public void setCanCreate(Boolean value)
    {
        _canCreate = value;
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
        if (!(obj instanceof _Super_Accessor))
            return false;
        final _Super_Accessor other = (_Super_Accessor) obj;

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
	
	public String retrieveJson(ObjectMapper objectMapper) {
		String objectJson = "\"cn\":\"" + getClass().getCanonicalName() + "\"," + 
				"\"ID\":\"" + getID() + "\"," +
				"\"canCreate\":" + (getCanCreate() == null ? "null" : getCanCreate()) + "," +
				"\"canDelete\":" + (getCanDelete() == null ? "null" : getCanDelete()) + "," +
				"\"canRead\":" + (getCanRead() == null ? "null" : getCanRead()) + "," +
				"\"canUpdate\":" + (getCanUpdate() == null ? "null" : getCanUpdate());
		
		return objectJson;
	}
}

