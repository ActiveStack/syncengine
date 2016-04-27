/**
 * This is a generated class and is not intended for modification.  To customize behavior
 * of this value object you may modify the generated sub-class of this class - User.java.
 */

package com.percero.agents.auth.vo;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.OneToMany;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@MappedSuperclass
@SuppressWarnings("rawtypes")
public class _Super_User implements Serializable, IAuthCachedObject
{
    /**
	 * 
	 */
	private static final long serialVersionUID = 8475549452096610513L;
	/*
     * Properties
     */
    private String _ID;
    private String _firstName;
    private String _lastName;
    private Date _dateCreated;
    private Date _dateModified;
    private List _userAccounts;

    /*
     * Property getters and setters
     */


    @Id
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
    public String getFirstName()
    {
    	return _firstName;
    }
    
    public void setFirstName(String value)
    {
    	_firstName = value;
    }
    
    @Column
    public String getLastName()
    {
    	return _lastName;
    }
    
    public void setLastName(String value)
    {
    	_lastName = value;
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


    @OneToMany(fetch=FetchType.LAZY, targetEntity=UserAccount.class, mappedBy="user")
    public List getUserAccounts()
    {
        return _userAccounts;
    }

    public void setUserAccounts(List value)
    {
        _userAccounts = value;
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
        if (!(obj instanceof _Super_User))
            return false;
        final _Super_User other = (_Super_User) obj;

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

