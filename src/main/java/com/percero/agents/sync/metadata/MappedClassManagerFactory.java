package com.percero.agents.sync.metadata;

public class MappedClassManagerFactory {

	private static IMappedClassManager mappedClassManager;
	public static IMappedClassManager getMappedClassManager() {
		if (mappedClassManager == null) {
			mappedClassManager = new MappedClassManager();
		}
		
		return mappedClassManager;
	}
}
