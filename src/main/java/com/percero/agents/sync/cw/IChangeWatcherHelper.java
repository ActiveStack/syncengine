package com.percero.agents.sync.cw;

import java.util.Collection;

import com.percero.agents.sync.services.IDataProviderManager;
import com.percero.agents.sync.services.ISyncAgentService;
import com.percero.agents.sync.vo.ClassIDPair;

public interface IChangeWatcherHelper {

	public ISyncAgentService getSyncAgentService();
	public IDataProviderManager getDataProviderManager();
	public Object get(String fieldName, ClassIDPair classIdPair);
	public Object get(String fieldName, ClassIDPair classIdPair, String clientId);
	public Object get(String fieldName, ClassIDPair classIdPair, String[] params);
	public Object get(String fieldName, ClassIDPair classIdPair, String[] params, String clientId);
	public Object calculate(String fieldName, ClassIDPair classIdPair);
	public Object calculate(String fieldName, ClassIDPair classIdPair, String[] params);
	public void recalculate(String fieldName, ClassIDPair classIdPair, Collection<String> clientIds, String[] params, Long requestTimestamp);
}
