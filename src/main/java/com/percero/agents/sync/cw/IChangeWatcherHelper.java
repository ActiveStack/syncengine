package com.percero.agents.sync.cw;

import com.percero.framework.vo.IPerceroObject;

import java.util.Collection;

public interface IChangeWatcherHelper {

	public Object process(String category, String subCategory, String fieldName, IPerceroObject oldValue);
	public Object process(String category, String subCategory, String fieldName, String[] params, IPerceroObject oldValue);
	public Object reprocess(String category, String subCategory, String fieldName, Collection<String> clientIds, String[] params, Long requestTimestamp, IPerceroObject oldValue);

}
