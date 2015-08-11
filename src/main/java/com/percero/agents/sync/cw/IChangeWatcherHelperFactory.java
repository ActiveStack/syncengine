package com.percero.agents.sync.cw;


public interface IChangeWatcherHelperFactory {

	public IChangeWatcherHelper getHelper(String className);
}
