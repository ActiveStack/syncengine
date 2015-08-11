/**
 * This is a generated class and is not intended for modification.  To customize behavior
 * of this value object you may modify the generated sub-class of this class - Email.java.
 */

package com.percero.agents.access.vo;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;

import org.hibernate.annotations.GenericGenerator;

import com.percero.agents.sync.vo.BaseDataObject;


@MappedSuperclass
public class _Super_RightsClass extends BaseDataObject implements Serializable
{
	/*
     * Properties
     */
    private String _ID;
    private String _toPropertyChain;
    private ModelClass _fromModelClass;
    private ModelClass _toModelClass;

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


    @JoinColumn(name="fromModelClass_ID")
    @org.hibernate.annotations.ForeignKey(name="FK_ClassRights_from_ModelClass")
    @ManyToOne(fetch=FetchType.LAZY,optional=false)
    public ModelClass getFromModelClass()
    {
        return _fromModelClass;
    }

    public void setFromModelClass(ModelClass value)
    {
        _fromModelClass = value;
    }


    @JoinColumn(name="toModelClass_ID")
    @org.hibernate.annotations.ForeignKey(name="FK_ClassRights_to_ModelClass")
    @ManyToOne(fetch=FetchType.LAZY,optional=false)
    public ModelClass getToModelClass()
    {
    	return _toModelClass;
    }
    
    public void setToModelClass(ModelClass value)
    {
    	_toModelClass = value;
    }
    
    
    @Column
    public String getToPropertyChain()
    {
        return _toPropertyChain;
    }

    public void setToPropertyChain(String value)
    {
        _toPropertyChain = value;
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
        if (!(obj instanceof _Super_RightsClass))
            return false;
        final _Super_RightsClass other = (_Super_RightsClass) obj;

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

