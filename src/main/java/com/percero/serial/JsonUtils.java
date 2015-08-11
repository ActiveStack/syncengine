package com.percero.serial;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.percero.agents.sync.metadata.MappedClass;
import com.percero.framework.vo.IPerceroObject;

public class JsonUtils {
	
	private static Logger log = Logger.getLogger(JsonUtils.class);
	
	public static byte[] getJsonByteArray(JsonObject jsonObject, String elementName) {
		JsonElement jsonElement = jsonObject.get(elementName);
		if (jsonElement != null && !jsonElement.isJsonNull()) {
			try {
				JsonPrimitive jsonPrimitive = jsonElement.getAsJsonPrimitive();
				String jsonString = jsonPrimitive.getAsString();
				byte[] byteArray = Base64.decodeBase64(jsonString);
				return byteArray;
			} catch(IllegalStateException ise) {
				log.error("Unable to parse JSON Byte Array", ise);
				return null;
			} catch(Exception e) {
				log.error("Unable to parse JSON Byte Array", e);
				return null;
			}
		}
		else
			return null;
	}
	
	public static String getJsonString(JsonObject jsonObject, String elementName) {
		try {
			// It is faster to catch the exception than check for JsonNull.
			return jsonObject.get(elementName).getAsString();
//			JsonElement jsonElement = jsonObject.get(elementName);
//			if (jsonElement != null && !jsonElement.isJsonNull()) {
//				return jsonElement.getAsString();
//			}
//			else {
//				return null;
//			}
		} catch(UnsupportedOperationException uoe) {
			// Unsupported Operation -> Element is NULL.
			return null;
		} catch(NullPointerException npe) {
			// Null Pointer -> Element does not exist.
			return null;
		}
	}
	
	public static Boolean getJsonBoolean(JsonObject jsonObject, String elementName) {
		try {
			return jsonObject.get(elementName).getAsBoolean();
		} catch(UnsupportedOperationException uoe) {
			// Unsupported Operation -> Element is NULL.
			return false;
		} catch(NullPointerException npe) {
			// Null Pointer -> Element does not exist.
			return false;
		}
//		JsonElement jsonElement = jsonObject.get(elementName);
//		if (jsonElement != null && !jsonElement.isJsonNull())
//			return jsonElement.getAsBoolean();
//		else
//			return false;
	}
	
	public static Double getJsonDouble(JsonObject jsonObject, String elementName) {
		try {
			return jsonObject.get(elementName).getAsDouble();
		} catch(UnsupportedOperationException uoe) {
			// Unsupported Operation -> Element is NULL.
			return null;
		} catch(NullPointerException npe) {
			// Null Pointer -> Element does not exist.
			return null;
		}
//		JsonElement jsonElement = jsonObject.get(elementName);
//		if (jsonElement != null && !jsonElement.isJsonNull())
//			return jsonElement.getAsDouble();
//		else
//			return null;
	}
	
	public static Integer getJsonInteger(JsonObject jsonObject, String elementName) {
		try {
			return jsonObject.get(elementName).getAsInt();
		} catch(UnsupportedOperationException uoe) {
			// Unsupported Operation -> Element is NULL.
			return null;
		} catch(NullPointerException npe) {
			// Null Pointer -> Element does not exist.
			return null;
		}
//		JsonElement jsonElement = jsonObject.get(elementName);
//		if (jsonElement != null && !jsonElement.isJsonNull())
//			return jsonElement.getAsInt();
//		else
//			return null;
	}
	
	public static Date getJsonDate(JsonObject jsonObject, String elementName) {
		try {
			return new Date(jsonObject.get(elementName).getAsLong());
		} catch(UnsupportedOperationException uoe) {
			// Unsupported Operation -> Element is NULL.
			return null;
		} catch(NullPointerException npe) {
			// Null Pointer -> Element does not exist.
			return null;
		}
//		JsonElement jsonElement = jsonObject.get(elementName);
//		if (jsonElement != null && !jsonElement.isJsonNull())
//			return new Date(jsonElement.getAsLong());
//		else
//			return null;
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static <T extends IPerceroObject> T getJsonPerceroObject(JsonObject jsonObject, String elementName) {
		JsonElement jsonElement = jsonObject.get(elementName);
		if (jsonElement != null && !jsonElement.isJsonNull()) {
			JsonObject jsonClassIdPair = jsonElement.getAsJsonObject();
			if (jsonClassIdPair != null && !jsonClassIdPair.isJsonNull()) {
				String className = getJsonString(jsonClassIdPair, "className");
				String classId = getJsonString(jsonClassIdPair, "ID");
				Class clazz = null;
				T perceroObject = null;
				try {
					clazz = MappedClass.forName(className);
					perceroObject = (T) clazz.newInstance();
					perceroObject.setID(classId);
				} catch (ClassNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (InstantiationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				return perceroObject;
			}
			else
				return null;
		}
		else
			return null;
	}
	
	@SuppressWarnings({ "rawtypes" })
	public static List<? extends IPerceroObject> getJsonListPerceroObject(JsonObject jsonObject, String elementName) {
		List<IPerceroObject> result = null;
		try {
//			JsonElement jsonElement = jsonObject.get(elementName);
//			if (jsonElement != null && !jsonElement.isJsonNull()) {
//				JsonArray jsonArray = jsonElement.getAsJsonArray();
//				if (jsonArray != null && !jsonArray.isJsonNull()) {
//					Iterator<JsonElement> itrElements = jsonArray.iterator();
			JsonArray jsonArray = jsonObject.get(elementName).getAsJsonArray();
			result = new ArrayList<IPerceroObject>(jsonArray.size());	// Create a new ArrayList the expected size of the Json Array.
			Iterator<JsonElement> itrElements = jsonArray.iterator();
			
			while (itrElements.hasNext()) {
				try {
					JsonElement nextJsonElement = itrElements.next();
					if (nextJsonElement != null) {// && !nextJsonElement.isJsonNull()) {
						JsonObject nextJsonObject = nextJsonElement.getAsJsonObject();
						String className = getJsonString(nextJsonObject, "className");
						String classId = getJsonString(nextJsonObject, "ID");
						
						if (className != null && classId != null) {
							Class clazz = null;
							IPerceroObject perceroObject = null;
							try {
								clazz = MappedClass.forName(className);
								perceroObject = (IPerceroObject) clazz.newInstance();
								perceroObject.setID(classId);
							} catch (ClassNotFoundException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							} catch (InstantiationException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							} catch (IllegalAccessException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							
							if (perceroObject != null)
								result.add(perceroObject);
						}
					}
				} catch(UnsupportedOperationException uoe) {
					// Unsupported Operation -> Element is NULL.
				} catch(NullPointerException npe) {
					// Null Pointer -> Element does not exist.
				}
			}
		} catch(UnsupportedOperationException uoe) {
			// Unsupported Operation -> Element is NULL.
		} catch(NullPointerException npe) {
			// Null Pointer -> Element does not exist.
		} finally {
			if (result == null) {
				result = new ArrayList<IPerceroObject>(0);
			}
		}

		return result;
	}

}
