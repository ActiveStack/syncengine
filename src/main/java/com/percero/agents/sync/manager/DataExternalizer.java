package com.percero.agents.sync.manager;

import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Collection;

import org.apache.log4j.Logger;

import com.percero.agents.sync.metadata.IMappedClassManager;
import com.percero.agents.sync.metadata.MappedClass;
import com.percero.agents.sync.metadata.MappedClassManagerFactory;
import com.percero.agents.sync.metadata.MappedField;
import com.percero.agents.sync.vo.ClassIDPair;
import com.percero.framework.vo.IPerceroObject;

public class DataExternalizer {
	
	private static Logger logger = Logger.getLogger(DataExternalizer.class);

	public static void readExternal(ObjectInput input, Object object) {
		IMappedClassManager mcm = MappedClassManagerFactory.getMappedClassManager();
		MappedClass mappedClass = mcm.getMappedClassByClassName(object.getClass().getName());
		
		for(MappedField nextMappedField : mappedClass.externalizableFields) {
			try {
				nextMappedField.readExternalField(input, object);
			} catch(Exception e) {
				logger.error("Error externalizing " + mappedClass.className + "." + nextMappedField.getField().getName(), e);
			}
		}
	}
	
	public static void writeExternal(ObjectOutput output, Object object) {
		IMappedClassManager mcm = MappedClassManagerFactory.getMappedClassManager();
		MappedClass mappedClass = mcm.getMappedClassByClassName(object.getClass().getName());
		
		for(MappedField nextMappedField : mappedClass.externalizableFields) {
			try {
				nextMappedField.writeExternalField(output, object);
			} catch(Exception e) {
				logger.error("Error externalizing " + mappedClass.className + "." + nextMappedField.getField().getName(), e);
			}
		}
	}
	
	@SuppressWarnings("rawtypes")
	public static Object[] getClassIDPairArray(Collection theCollection) {
		Object[] objectArray;
		if (theCollection == null || theCollection.size() == 0) {
			objectArray = new Object[0];
		} else {
			objectArray = new Object[theCollection.size()];

			try {
				int i = 0;
				for(Object nextObject : theCollection) {
					if (nextObject instanceof IPerceroObject) {
						// TODO: Allow some sort of parameter to turn this optimization off.
						//	i.e. Don't convert to a ClassIDPair, just simply include the entire object as-is.
						// Create ClassIDPair object and pass that instead of the entire object.
						IPerceroObject nextPerceroObject = (IPerceroObject) nextObject;
						ClassIDPair nextClassIdPair = new ClassIDPair(nextPerceroObject.getID(), nextObject.getClass().getName());
						objectArray[i] = nextClassIdPair;
					} else {
						objectArray[i] = nextObject;
					}
					i++;
				}
			} catch(Exception e) {
				logger.error("Error converting list of objects to array of ClassIDPairs", e);
			}
		}
		
		return objectArray;
	}
}
