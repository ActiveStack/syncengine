package com.percero.agents.sync.cw;

import com.percero.agents.sync.vo.ClassIDPair;

public interface IChangeWatcherValueHelper {

	public Object get(String fieldName, ClassIDPair classIdPair);
	public Object get(String fieldName, ClassIDPair classIdPair, String clientId);
	public Object get(String fieldName, ClassIDPair classIdPair, String[] params);
	public Object get(String fieldName, ClassIDPair classIdPair, String[] params, String clientId);

}
