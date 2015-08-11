package com.percero.agents.sync.metadata;

public interface IMappedClassManager {

	public void addMappedClass(MappedClass theMappedClass);
	public MappedClass getMappedClassByClassName(String aClassName);
}
