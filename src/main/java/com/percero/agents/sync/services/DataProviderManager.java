package com.percero.agents.sync.services;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class DataProviderManager implements IDataProviderManager {

	@Autowired
	ApplicationContext appContext;
	public void setAppContext(ApplicationContext value) {
		appContext = value;
	}
	
	@Autowired
	SyncAgentDataProvider defaultDataProvider;
	public void setDefaultDataProvider(SyncAgentDataProvider value) {
		defaultDataProvider = value;
		defaultDataProvider.initialize();
		addDataProvider(defaultDataProvider);
	}
	
	private Map<String, IDataProvider> dataProvidersByName = new HashMap<String, IDataProvider>();

	public void addDataProvider(IDataProvider theDataProvider) {
		if (!dataProvidersByName.containsKey(theDataProvider.getName()))
			dataProvidersByName.put(theDataProvider.getName(), theDataProvider);
	}

	public IDataProvider getDataProviderByName(String aName) {
		if (!StringUtils.hasText(aName))
			return defaultDataProvider;

		if (!dataProvidersByName.containsKey(aName)) {
			// Attempt to get the bean from the ApplicationContext.
			IDataProvider dataProvider = (IDataProvider) appContext.getBean(aName);
			dataProvider.initialize();
			addDataProvider(dataProvider);
			return dataProvider;
		} else
			return dataProvidersByName.get(aName);
	}

}
