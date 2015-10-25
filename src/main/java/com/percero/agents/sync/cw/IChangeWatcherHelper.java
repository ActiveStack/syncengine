package com.percero.agents.sync.cw;

import java.util.Collection;

public interface IChangeWatcherHelper {

	public Object process(String category, String subCategory, String fieldName);
	public Object process(String category, String subCategory, String fieldName, String[] params);
	public Object reprocess(String category, String subCategory, String fieldName, Collection<String> clientIds, String[] params, Long requestTimestamp);

}
