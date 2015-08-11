package com.percero.agents.sync.metadata;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class EntityImplementation {

	public MappedClass mappedClass = null;
	public Class<?> entityInterfaceClass = null;
	public List<RelationshipImplementation> relationshipImplementations = new ArrayList<RelationshipImplementation>();
	public List<PropertyImplementation> propertyImplementations = new ArrayList<PropertyImplementation>();

	public RelationshipImplementation findRelationshipImplementationBySourceVarName(String sourceVarName) {
		Iterator<RelationshipImplementation> itrRelImpls = relationshipImplementations.iterator();
		while (itrRelImpls.hasNext()) {
			RelationshipImplementation nextRelImpl = itrRelImpls.next();
			if (nextRelImpl.sourceVarName.equals(sourceVarName)) {
				return nextRelImpl;
			}
		}
		
		return null;
	}
	
	public PropertyImplementation findPropertyImplementationByName(String name) {
		Iterator<PropertyImplementation> itrPropImpls = propertyImplementations.iterator();
		while (itrPropImpls.hasNext()) {
			PropertyImplementation nextPropImpl = itrPropImpls.next();
			if (nextPropImpl.propertyName.equals(name)) {
				return nextPropImpl;
			}
		}
		
		return null;
	}
}
