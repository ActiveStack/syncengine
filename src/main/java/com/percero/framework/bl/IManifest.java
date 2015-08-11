package com.percero.framework.bl;

import java.util.List;
import java.util.Map;

public interface IManifest {

	@SuppressWarnings("rawtypes")
	public List<Class> getClassList();
	public List<Object> getObjectList();
	@SuppressWarnings("rawtypes")
	public Map<String, Class> getUuidMap();
}
