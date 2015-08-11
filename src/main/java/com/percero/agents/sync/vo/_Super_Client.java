/**
 * This is a generated class and is not intended for modification.  To customize behavior
 * of this value object you may modify the generated sub-class of this class - Person.java.
 */

package com.percero.agents.sync.vo;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Index;


@MappedSuperclass
public class _Super_Client extends BaseDataObject implements Serializable
{
	public static final Integer IDENTIFIER = 4;

	/*
     * Properties
     */
    private String _ID;
    private Date _dateCreated;
    private Date _dateLastLogout;
    private String _belongsToUserId;
    private String _clientId;
    private String _deviceId;
    private Boolean _isLoggedIn;
    private String _deviceType;

    /*
     * Property getters and setters
     */


    @Id
	@GeneratedValue(generator="system-uuid")
	@GenericGenerator(name="system-uuid", strategy = "uuid")
	@Column(unique = true)
    public String getID()
    {
        return _ID;
    }

    public void setID(String value)
    {
        _ID = value;
    }


    @Temporal(TemporalType.TIMESTAMP)
    @Column
    public Date getDateCreated()
    {
        return _dateCreated;
    }

    public void setDateCreated(Date value)
    {
        _dateCreated = value;
    }


    @Temporal(TemporalType.TIMESTAMP)
    @Column
    public Date getDateLastLogout()
    {
        return _dateLastLogout;
    }

    public void setDateLastLogout(Date value)
    {
        _dateLastLogout = value;
    }


    @Index(name="idxUserId")
    @Column
    public String getBelongsToUserId()
    {
        return _belongsToUserId;
    }

    public void setBelongsToUserId(String value)
    {
        _belongsToUserId = value;
    }


    @Column
    public String getClientId()
    {
        return _clientId;
    }

    public void setClientId(String value)
    {
        _clientId = value;
    }
    
    
    @Column
    public String getDeviceId()
    {
    	return _deviceId;
    }
    
    public void setDeviceId(String value)
    {
    	_deviceId = value;
    }


    @Column
    public Boolean getIsLoggedIn()
    {
        return _isLoggedIn;
    }

    public void setIsLoggedIn(Boolean value)
    {
        _isLoggedIn = value;
    }


    @Column
    public String getDeviceType()
    {
        return _deviceType;
    }

    public void setDeviceType(String value)
    {
        _deviceType = value;
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
        if (!(obj instanceof _Super_Client))
            return false;
        final _Super_Client other = (_Super_Client) obj;

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
}

