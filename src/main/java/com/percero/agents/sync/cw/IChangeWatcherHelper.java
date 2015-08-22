package com.percero.agents.sync.cw;

import java.util.Collection;

public interface IChangeWatcherHelper {

	public void process(String category, String subCategory, String fieldName);
	public void process(String category, String subCategory, String fieldName, String[] params);
	public void reprocess(String category, String subCategory, String fieldName, Collection<String> clientIds, String[] params, Long requestTimestamp);

}
