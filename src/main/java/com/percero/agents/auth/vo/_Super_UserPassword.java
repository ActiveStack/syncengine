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
public class _Super_UserPassword implements Serializable, IAuthCachedObject
{
    /**
	 * 
	 */
	private static final long serialVersionUID = 8475549452096610513L;
	/*
     * Properties
     */
    private String _ID;
    //private String _userName;
    //private String _email;
    private String _password;
    private UserIdentifier _userIdentifier;

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

/*
    @Column
    public String getUserName()
    {
        return _userName;
    }

    public void setUserName(String value)
    {
        _userName = value;
    }


    @Column
    public String getEmail()
    {
    	return _email;
    }
    
    public void setEmail(String value)
    {
    	_email = value;
    }
    */
    
    @Column
    public String getPassword()
    {
        return _password;
    }

    public void setPassword(String value)
    {
        _password = value;
    }
  

    @JsonSerialize(using=ACOSerializer.class)
    @JsonDeserialize(using=ACODeserializer.class)
    @JoinColumn(name="userIdentifier_ID")
    @org.hibernate.annotations.ForeignKey(name="FK_UserPassword_userId_TO_UserId")
    @ManyToOne(fetch=FetchType.LAZY)
    public UserIdentifier getUserIdentifier()
    {
        return _userIdentifier;
    }

    public void setUserIdentifier(UserIdentifier value)
    {
        _userIdentifier = value;
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
        if (!(obj instanceof _Super_UserPassword))
            return false;
        final _Super_UserPassword other = (_Super_UserPassword) obj;

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

