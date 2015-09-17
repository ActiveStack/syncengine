package com.percero.agents.sync.metadata;

import java.util.Collection;

public interface IMappedClassManager {

	void addMappedClass(MappedClass theMappedClass);
	MappedClass getMappedClassByClassName(String aClassName);
	Collection<MappedClass> getAllMappedClasses();
}
