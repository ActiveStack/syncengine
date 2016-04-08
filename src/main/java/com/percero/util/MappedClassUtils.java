package com.percero.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MappedClassUtils {

	@SuppressWarnings("rawtypes")
	private static final Map<Class, List<Field>> classFieldsByClass = new ConcurrentHashMap<Class, List<Field>>();
	
	@SuppressWarnings("rawtypes")
	public static List<Field> getClassFields(Class theClass) {
		List<Field> fieldList = classFieldsByClass.get(theClass);
		if (fieldList == null) {
			fieldList = new ArrayList<Field>();
			classFieldsByClass.put(theClass, fieldList);
			
			Field[] theFields = theClass.getDeclaredFields();
			for(Field nextField : theFields) {
				boolean isStatic = Modifier.STATIC == (nextField.getModifiers() & Modifier.STATIC);
				if (!isStatic)
					fieldList.add(nextField);
			}
			
			if (theClass.getSuperclass() != null)
				fieldList.addAll(getClassFields(theClass.getSuperclass()));
		}
		
		return fieldList;
	}
	
	@SuppressWarnings("rawtypes")
	private static final Map<Class, Map<String, Method>> classGetterMethods = new ConcurrentHashMap<Class, Map<String, Method>>();
	
	@SuppressWarnings("rawtypes")
	private static Map<String, Method> retrieveClassMethods(Class theClass) {
		Map<String, Method> classMethods = classGetterMethods.get(theClass);
		if (classMethods == null) {
			classMethods = new ConcurrentHashMap<String, Method>();
			classGetterMethods.put(theClass, classMethods);
			
			Method[] theMethods = theClass.getMethods();
			for(Method nextMethod : theMethods) {
				// We are storing lower case name for easy compare
				classMethods.put(nextMethod.getName().toLowerCase(), nextMethod);
			}
		}
		
		return classMethods;
	}

	@SuppressWarnings("rawtypes")
	public static Method getFieldGetters(Class theClass, Field theField) {
		Method theMethod = null;
		String theModifiedFieldName = theField.getName();
		if (theModifiedFieldName.indexOf("_") == 0)
			theModifiedFieldName = theModifiedFieldName.substring(1);
		
		String getterMethodName = (new StringBuilder("get").append(theModifiedFieldName.toLowerCase())).toString();
		
		Map<String, Method> methods = retrieveClassMethods(theClass);
		if (methods != null && !methods.isEmpty()) {
			theMethod = methods.get(getterMethodName);
		}
		
		return theMethod;
	}

	@SuppressWarnings("rawtypes")
	public static Method getFieldSetters(Class theClass, Field theField) {
		Method theMethod = null;
		String theModifiedFieldName = theField.getName();
		if (theModifiedFieldName.indexOf("_") == 0)
			theModifiedFieldName = theModifiedFieldName.substring(1);
		
		String setterMethodName = (new StringBuilder("set").append(theModifiedFieldName.toLowerCase())).toString();
		
		Map<String, Method> methods = retrieveClassMethods(theClass);
		if (methods != null && !methods.isEmpty()) {
			theMethod = methods.get(setterMethodName);
		}
		
		return theMethod;
	}
	
}
