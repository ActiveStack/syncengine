/**
 * This is a generated class and is not intended for modification.  To customize behavior
 * of this value object you may modify the generated sub-class of this class - UserAccount.java.
 */

package com.percero.agents.auth.vo;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.codehaus.jackson.map.annotate.JsonDeserialize;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Index;

import com.percero.framework.vo.IPerceroObject;
import com.percero.serial.ACODeserializer;
import com.percero.serial.ACOSerializer;

@MappedSuperclass
public class _Super_UserToken implements Serializable, IAuthCachedObject, IPerceroObject
{
    /**
	 * 
	 */
	private static final long serialVersionUID = 4162375157109950353L;
	/**
	 * 
	 */
//	private static final long serialVersionUID = -644652983257798379L;
	/*
     * Properties
     */
    private String _ID;
    private Date _dateCreated;
    private Date _dateModified;
    private Date _lastLogin;
    private User _user;
    private String _clientId;
    private String _deviceId;
    private String _token;

    public String classVersion() {
    	return "1.2.2";
    }

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
    public Date getDateModified()
    {
        return _dateModified;
    }

    public void setDateModified(Date value)
    {
        _dateModified = value;
    }


    @Temporal(TemporalType.TIMESTAMP)
    @Column
    public Date getLastLogin()
    {
        return _lastLogin;
    }

    public void setLastLogin(Date value)
    {
        _lastLogin = value;
    }


    @JsonSerialize(using=ACOSerializer.class)
    @JsonDeserialize(using=ACODeserializer.class)
    @JoinColumn(name="user_ID")
    @org.hibernate.annotations.ForeignKey(name="FK_UserToken_user_TO_User")
    @ManyToOne(fetch=FetchType.LAZY)
    public User getUser()
    {
        return _user;
    }

    public void setUser(User value)
    {
        _user = value;
    }


    @Index(name="idx_clientId")
    @Column
    public String getClientId()
    {
        return _clientId;
    }

    public void setClientId(String value)
    {
        _clientId = value;
    }


    @Index(name="idx_deviceId")
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
    public String getToken()
    {
        return _token;
    }

    public void setToken(String value)
    {
        _token = value;
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
        if (!(obj instanceof _Super_UserToken))
            return false;
        final _Super_UserToken other = (_Super_UserToken) obj;

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

