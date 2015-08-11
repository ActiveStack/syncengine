/**
 * This is a generated class and is not intended for modification.  To customize behavior
 * of this value object you may modify the generated sub-class of this class - Email.java.
 */

package com.percero.agents.sync.vo;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;

import org.hibernate.annotations.GenericGenerator;


@MappedSuperclass
public class _Super_DeleteJournal extends BaseDataObject implements Serializable
{
	/*
     * Properties
     */
    private String _ID;
    private String _className;
    private String _classID;
    private Client _client;
    private Date _dateCreated;
    private Date _dateModified;

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


    @JoinColumn(name="client_ID")
    @org.hibernate.annotations.ForeignKey(name="FK_DeleteJournal_client_TO_Client")
    @ManyToOne(fetch=FetchType.LAZY,optional=false)
    public Client getClient()
    {
        return _client;
    }

    public void setClient(Client value)
    {
        _client = value;
    }


    @Column
    public String getClassName()
    {
        return _className;
    }

    public void setClassName(String value)
    {
        _className = value;
    }


    @Column
    public String getClassID()
    {
        return _classID;
    }

    public void setClassID(String value)
    {
        _classID = value;
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
    public Date getDateModified()
    {
        return _dateModified;
    }

    public void setDateModified(Date value)
    {
        _dateModified = value;
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
        if (!(obj instanceof _Super_DeleteJournal))
            return false;
        final _Super_DeleteJournal other = (_Super_DeleteJournal) obj;

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

