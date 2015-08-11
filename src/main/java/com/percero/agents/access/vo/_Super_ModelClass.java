/**
 * This is a generated class and is not intended for modification.  To customize behavior
 * of this value object you may modify the generated sub-class of this class - Email.java.
 */

package com.percero.agents.access.vo;

import java.io.Serializable;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.OneToMany;

import org.hibernate.annotations.GenericGenerator;

import com.percero.agents.sync.vo.BaseDataObject;


@MappedSuperclass
public class _Super_ModelClass extends BaseDataObject implements Serializable
{
	/*
     * Properties
     */
    private String _ID;
    private String _className;
    private String _classID;
    private List<RightsClass> _fromModelClasses;
    private List<RightsClass> _toModelClasses;

    /*
     * Property getters and setters
     */


    @Id
	@GeneratedValue(generator="system-uuid")
	@GenericGenerator(name="system-uuid", strategy = "uuid")
	@Column(unique = true)
    public final String getID()
    {
        return _ID;
    }

    public final void setID(String value)
    {
        _ID = value;
    }


    @Column
    public final String getClassName()
    {
        return _className;
    }

    public final void setClassName(String value)
    {
        _className = value;
    }


    @Column
    public final String getClassID()
    {
        return _classID;
    }

    public final void setClassID(String value)
    {
        _classID = value;
    }


    @OneToMany(fetch=FetchType.LAZY, targetEntity=RightsClass.class, mappedBy="fromModelClass")
	public final List<RightsClass> getFromModelClasses() {
		return this._fromModelClasses;
	}
	public final void setFromModelClasses(List<RightsClass> value) {
		this._fromModelClasses = value;
	}

    @OneToMany(fetch=FetchType.LAZY, targetEntity=RightsClass.class, mappedBy="toModelClass")
	public final List<RightsClass> getToModelClasses() {
		return this._toModelClasses;
	}
	public final void setToModelClasses(List<RightsClass> value) {
		this._toModelClasses = value;
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
        if (!(obj instanceof _Super_ModelClass))
            return false;
        final _Super_ModelClass other = (_Super_ModelClass) obj;

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

