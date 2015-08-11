/**
 * This is a generated class and is not intended for modification.  To customize behavior
 * of this value object you may modify the generated sub-class of this class - ServiceApplication.java.
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

import org.hibernate.annotations.GenericGenerator;

@MappedSuperclass
public class _Super_SvcAppRole implements Serializable, IAuthCachedObject
{
    /**
	 * 
	 */
	private static final long serialVersionUID = 6326717954130425327L;
	/*
     * Properties
     */
    private String _ID;
    private String _type;
    private String _value;
    private Boolean _isRequired;
    private String authProvider;

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
    public String getType()
    {
        return _type;
    }

    public void setType(String value)
    {
        _type = value;
    }


    @Column
    public String getValue()
    {
        return _value;
    }

    public void setValue(String value)
    {
        _value = value;
    }


    @Column
    public Boolean getIsRequired()
    {
        return _isRequired;
    }

    public void setIsRequired(Boolean value)
    {
        _isRequired = value;
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
        if (!(obj instanceof _Super_SvcAppRole))
            return false;
        final _Super_SvcAppRole other = (_Super_SvcAppRole) obj;

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

    @Column
	public String getAuthProvider() {
		return authProvider;
	}

	public void setAuthProvider(String authProvider) {
		this.authProvider = authProvider;
	}
}

