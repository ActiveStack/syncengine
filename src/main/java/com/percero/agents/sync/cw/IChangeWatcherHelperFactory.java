package com.percero.agents.sync.cw;


public interface IChangeWatcherHelperFactory {

	public IChangeWatcherHelper getHelper(String className);
	public void registerChangeWatcherHelper(String category, IChangeWatcherHelper changeWatcherHelper);
}
