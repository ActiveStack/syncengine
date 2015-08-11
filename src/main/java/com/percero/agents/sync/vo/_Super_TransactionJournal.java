/**
 * This is a generated class and is not intended for modification.  To customize behavior
 * of this value object you may modify the generated sub-class of this class - TransactionJournal.java.
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
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.annotations.GenericGenerator;


@MappedSuperclass
public class _Super_TransactionJournal extends BaseDataObject implements Serializable
{
	/*
     * Properties
     */
    private String _ID;
    private String _transactionId;
    private Date _dateProcessed;
    private String _clientId;
    private Boolean _result;

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
    public Date getDateProcessed()
    {
        return _dateProcessed;
    }

    public void setDateProcessed(Date value)
    {
        _dateProcessed = value;
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
    public String getTransactionId()
    {
    	return _transactionId;
    }
    
    public void setTransactionId(String value)
    {
    	_transactionId = value;
    }
    
    @Column
    public Boolean getResult()
    {
    	return _result;
    }
    
    public void setResult(Boolean value)
    {
    	_result = value;
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
        if (!(obj instanceof _Super_TransactionJournal))
            return false;
        final _Super_TransactionJournal other = (_Super_TransactionJournal) obj;

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

