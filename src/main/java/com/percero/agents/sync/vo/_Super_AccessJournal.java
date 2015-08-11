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
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Index;
import org.hibernate.annotations.Type;

import com.percero.hibernate.types.DateBigIntType;


@MappedSuperclass
public class _Super_AccessJournal extends BaseDataObject implements Serializable
{
	/*
     * Properties
     */
    private String _ID;
    private String _className;
    private String _classID;
    private String _userID;
    private Date _dateAccessed;

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


/**    @JoinColumn(name="person_ID")
    @org.hibernate.annotations.ForeignKey(name="FK_Email_person_TO_Person")
    @ManyToOne(fetch=FetchType.LAZY)
    public Person getPerson()
    {
        return _person;
    }

    public void setPerson(Person value)
    {
        _person = value;
    }**/
    @Column
    @Index(name="idxObject")
    @Type(type="bigIntDate")
    public Date getDateAccessed()
    {
        return _dateAccessed;
    }

    public void setDateAccessed(Date value)
    {
        _dateAccessed = value;
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
    public String getUserID()
    {
        return _userID;
    }

    public void setUserID(String value)
    {
        _userID = value;
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
        if (!(obj instanceof _Super_AccessJournal))
            return false;
        final _Super_AccessJournal other = (_Super_AccessJournal) obj;

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

