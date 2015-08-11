/**
 * This is a generated class and is not intended for modification.  To customize behavior
 * of this value object you may modify the generated sub-class of this class - Email.java.
 */

package com.percero.agents.sync.vo;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Index;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import com.percero.hibernate.types.DateBigIntType;


/*
@org.hibernate.annotations.Table(
		appliesTo="ObjectModJournal",
		indexes = {@Index(name="idx_class_id", columnNames={"className", "classID","dateModified","userId"})}
)*/
@MappedSuperclass
public class _Super_ObjectModJournal extends BaseDataObject implements Serializable
{
	/*
     * Properties
     */
    private String _ID;
    private String _className;
    private String _classID;
    private String _transactionId;
    private Date _dateModified;
    private String _userId;

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
    @Index(name="idxObject")
    public String getClassName()
    {
        return _className;
    }

    public void setClassName(String value)
    {
        _className = value;
    }


    @Column
    @Index(name="idxTrans")
    public String getTransactionId()
    {
    	return _transactionId;
    }
    
    public void setTransactionId(String value)
    {
    	_transactionId = value;
    }
    
    
    @Column
    @Index(name="idxObject")
    public String getClassID()
    {
        return _classID;
    }

    public void setClassID(String value)
    {
        _classID = value;
    }


    @Column
    @Index(name="idxObject")
    @Type(type="bigIntDate")
    public Date getDateModified()
    {
        return _dateModified;
    }

    public void setDateModified(Date value)
    {
        _dateModified = value;
    }

    @Index(name="idxUserId")
    @Column
    public String getUserId()
    {
        return _userId;
    }

    public void setUserId(String value)
    {
        _userId = value;
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
        if (!(obj instanceof _Super_ObjectModJournal))
            return false;
        final _Super_ObjectModJournal other = (_Super_ObjectModJournal) obj;

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

