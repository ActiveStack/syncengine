package com.percero.agents.sync.hibernate;

import java.util.Date;
import java.util.List;

import org.hibernate.criterion.Example.PropertySelector;
import org.hibernate.type.BooleanType;
import org.hibernate.type.IntegerType;
import org.hibernate.type.StringType;
import org.hibernate.type.TimestampType;
import org.hibernate.type.Type;

import com.percero.framework.vo.IPerceroObject;

@SuppressWarnings("serial")
public class BaseDataObjectPropertySelector implements PropertySelector {
	
	public BaseDataObjectPropertySelector(List<String> excludeProps) {
		excludeProperties = excludeProps;
	}
	
	public List<String> excludeProperties = null;

	public boolean include(Object propertyValue, String propertyName, Type type) {
		if (propertyValue == null) {
			return false;
		} else if (excludeProperties != null && excludeProperties.contains(propertyName)) {
			return false;
		}

		if (type instanceof StringType) {
			if (((String)propertyValue).length() == 0) {
				return false;
			} else {
				return true;
			}
		} else if (type.isEntityType()) {
			if (propertyValue instanceof IPerceroObject) {
				if (((IPerceroObject)propertyValue).getID() != null && !((IPerceroObject)propertyValue).getID().equals("") && !((IPerceroObject)propertyValue).getID().equals("0")) {
					return true;
				} else {
					return false;
				}
			} else {
				return false;
			}
		} else if (type instanceof IntegerType) {
			if (((Integer)propertyValue) != 0) {
				return true;
			} else {
				return false;
			}
		} else if (type instanceof TimestampType) {
			if (((Date)propertyValue).getTime() != 0) {
				return true;
			} else {
				return false;
			}
		} else if (type instanceof BooleanType) {
			return false;
		}
		return true;
	}

}
