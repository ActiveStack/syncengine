package com.percero.agents.sync.services;

import java.util.Collection;

import com.percero.agents.sync.vo.ClassIDPair;
import com.percero.agents.sync.vo.SyncResponse;
import com.percero.framework.vo.IPerceroObject;

public interface IPushSyncHelper {
	
	//public void pushJsonToRouting(String objectJson, Class objectClass, String routingKey);
	public void pushSyncResponseToClient(SyncResponse anObject, String clientId);
	public void pushSyncResponseToClients(SyncResponse anObject, Collection<String> clientIds);
	
	//public void pushObjectToClient(Object anObject, String clientId);
	public void pushObjectToClients(Object anObject, Collection<String> listClients);
	
	public void pushStringToRoute(String aString, String routeName);

	public Boolean removeClient(String clientId);
	public Boolean renameClient(String thePreviousClientId, String clientId);
	void enqueueCheckChangeWatcher(ClassIDPair classIDPair, String[] fieldNames, String[] params);
	void enqueueCheckChangeWatcher(ClassIDPair classIDPair, String[] fieldNames, String[] params,
			IPerceroObject oldValue);
}