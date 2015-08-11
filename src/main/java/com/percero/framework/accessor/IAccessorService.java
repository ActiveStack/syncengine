package com.percero.framework.accessor;

import java.util.List;
import java.util.Map;

import com.percero.framework.metadata.IMappedClass;



public interface IAccessorService {

	public Object testCall(String aParam);
	public Accessor getAccessor(String userId, String className, String classId);
	@SuppressWarnings("rawtypes")
	public Accessor getReadAccessor(String userId, Class theClass, String classId, IMappedClass mappedClass) throws Exception;
	@SuppressWarnings("rawtypes")
	public Map<String, Accessor> getReadAccessors(List<String> userIds, Class theClass, String classId, IMappedClass mappedClass) throws Exception;
	public Object pushMessage(String userId, String userToken, String deviceId, String className, String classId, Object message);
}
