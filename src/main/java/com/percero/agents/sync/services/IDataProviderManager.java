package com.percero.agents.sync.services;

public interface IDataProviderManager {

	public void addDataProvider(IDataProvider theDataProvider);
	public IDataProvider getDataProviderByName(String aName);
}
