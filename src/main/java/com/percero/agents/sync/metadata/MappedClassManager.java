package com.percero.agents.sync.metadata;

import java.util.Collection;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public class MappedClassManager implements IMappedClassManager {

	private Map<String, MappedClass> mappedClassesByName = new ConcurrentHashMap<String, MappedClass>();

	public void addMappedClass(MappedClass theMappedClass) {
		if (!mappedClassesByName.containsKey(theMappedClass.className))
			mappedClassesByName.put(theMappedClass.className, theMappedClass);
	}

	public MappedClass getMappedClassByClassName(String aClassName) {
		MappedClass mc = mappedClassesByName.get(aClassName);
		if (mc == null) {
			Random rnd = new Random();
			mc = new MappedClass(rnd.nextInt(), aClassName);
			addMappedClass(mc);
		}
		
		return mc;
	}

	@Override
	public Collection<MappedClass> getAllMappedClasses() {
		return mappedClassesByName.values();
	}

}
