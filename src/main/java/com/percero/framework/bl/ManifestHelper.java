package com.percero.framework.bl;

import java.util.HashSet;
import java.util.Set;

public class ManifestHelper {

	private static IManifest manifest;
	public static IManifest getManifest() {
		return manifest;
	}

	public static void setManifest(IManifest value) {
		manifest = value;
	}

	@SuppressWarnings("rawtypes")
	public static Class findImplementingClass(Class theInterface) {
		
		for(Class nextClass : getManifest().getClassList()) {
			Class nextCheckClass = nextClass;
			while(nextCheckClass != null) {
				for(Class nextInterface : nextCheckClass.getInterfaces()) {
					if (nextInterface == theInterface)
						return nextClass;
				}
				nextCheckClass = nextCheckClass.getSuperclass();
			}
		}
		
		return null;
	}
	
	@SuppressWarnings("rawtypes")
	public static Set<Class> findImplementingClasses(Class theInterface) {
		
		Set<Class> result = new HashSet<Class>();
		
		for(Class nextClass : getManifest().getClassList()) {
			Class nextCheckClass = nextClass;
			while(nextCheckClass != null) {
				for(Class nextInterface : nextCheckClass.getInterfaces()) {
					if (nextInterface == theInterface)
						result.add(nextClass);
				}
				nextCheckClass = nextCheckClass.getSuperclass();
			}
		}
		
		return result;
	}
}
