/**
 * This is a generated class and is not intended for modification.  To customize behavior
 * of this value object you may modify the generated sub-class of this class - User.java.
 */

package com.percero.agents.auth.vo;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;

import org.codehaus.jackson.map.annotate.JsonDeserialize;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.hibernate.annotations.GenericGenerator;

import com.percero.serial.ACODeserializer;
import com.percero.serial.ACOSerializer;

@MappedSuperclass
public class _Super_UserIdentifier implements Serializable, IAuthCachedObject
{
    /**
	 * 
	 */
	private static final long serialVersionUID = 3948649452096604937L;
	/*
     * Properties
     */
    private String _ID;
    private String _userIdentifier;
    private String _type;
    private User _user;

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


    @Column
    public String getUserIdentifier()
    {
        return _userIdentifier;
    }

    public void setUserIdentifier(String value)
    {
        _userIdentifier = value;
    }


    @Column
    public String getType()
    {
    	return _type;
    }
    
    public void setType(String value)
    {
    	_type = value;
    }
    

    @JsonSerialize(using=ACOSerializer.class)
    @JsonDeserialize(using=ACODeserializer.class)
    @JoinColumn(name="user_ID")
    @org.hibernate.annotations.ForeignKey(name="FK_UserIdentifier_user_TO_User")
    @ManyToOne(fetch=FetchType.LAZY)
    public User getUser()
    {
        return _user;
    }

    public void setUser(User value)
    {
        _user = value;
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
        if (!(obj instanceof _Super_UserIdentifier))
            return false;
        final _Super_UserIdentifier other = (_Super_UserIdentifier) obj;

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

