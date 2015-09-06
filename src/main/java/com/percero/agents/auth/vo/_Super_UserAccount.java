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

import org.hibernate.annotations.GenericGenerator;

@MappedSuperclass
public class _Super_UserAccount implements Serializable, IAuthCachedObject
{
    /**
	 * 
	 */
	private static final long serialVersionUID = -8609865324496621497L;
	/*
     * Properties
     */
    private String _ID;
    private Date _dateCreated;
    private Date _dateModified;
    private User _user;
    private String _accessToken;
    private String _refreshToken;
    private String _accountId;
	private Boolean _isSupended;
	private Boolean _isAdmin;
	private String _authProviderID;

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


    @JoinColumn(name="user_ID")
    @org.hibernate.annotations.ForeignKey(name="FK_UserAccount_user_TO_User")
    @ManyToOne(fetch=FetchType.LAZY)
    public User getUser()
    {
        return _user;
    }

    public void setUser(User value)
    {
        _user = value;
    }

    @Column
    public String getAccessToken()
    {
        return _accessToken;
    }

    public void setAccessToken(String value)
    {
        _accessToken = value;
    }


    @Column
    public String getRefreshToken()
    {
        return _refreshToken;
    }

    public void setRefreshToken(String value)
    {
        _refreshToken = value;
    }


    @Column
    public String getAccountId()
    {
        return _accountId;
    }

    public void setAccountId(String value)
    {
        _accountId = value;
    }


    @Column
	public Boolean getIsAdmin() {
		return _isAdmin;
	}
	public void setIsAdmin(Boolean isAdmin) {
		this._isAdmin = isAdmin;
	}

    @Column
	public Boolean getIsSupended() {
		return _isSupended;
	}
	public void setIsSupended(Boolean isSupended) {
		this._isSupended = isSupended;
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
        if (!(obj instanceof _Super_UserAccount))
            return false;
        final _Super_UserAccount other = (_Super_UserAccount) obj;

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

	public String getAuthProviderID() {
		return _authProviderID;
	}

	public void setAuthProviderID(String providerID) {
		this._authProviderID = providerID;
	}
}

