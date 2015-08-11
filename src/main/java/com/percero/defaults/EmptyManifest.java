package com.percero.defaults;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.percero.framework.bl.IManifest;

@Component
public class EmptyManifest implements IManifest {

	@Override
	public List<Class> getClassList() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Object> getObjectList() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, Class> getUuidMap() {
		// TODO Auto-generated method stub
		return null;
	}

}
