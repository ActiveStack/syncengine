package com.percero.agents.sync.services;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class DataProviderManager implements IDataProviderManager {
	
	private static DataProviderManager instance = null;
	public static DataProviderManager getInstance() {
		return instance;
	}
	
	public DataProviderManager() {
		instance = this;
	}

	@Autowired
	ApplicationContext appContext;
	public void setAppContext(ApplicationContext value) {
		appContext = value;
	}
	
//	@Autowired
	IDataProvider defaultDataProvider;
	public void setDefaultDataProvider(IDataProvider value) {
		defaultDataProvider = value;
		defaultDataProvider.initialize();
		addDataProvider(defaultDataProvider);
	}
	public IDataProvider getDefaultDataProvider() {
		if (defaultDataProvider == null) {
			// Check to see if there are ANY data providers.
			if (dataProvidersByName.size() > 0) {
				Iterator<IDataProvider> itrDataProviders = dataProvidersByName.values().iterator();
				if (itrDataProviders.hasNext()) {
					return itrDataProviders.next();
				}
			}
		}
		
		return defaultDataProvider;
	}
	
	private Map<String, IDataProvider> dataProvidersByName = new HashMap<String, IDataProvider>();

	public void addDataProvider(IDataProvider theDataProvider) {
		if (!dataProvidersByName.containsKey(theDataProvider.getName()))
			dataProvidersByName.put(theDataProvider.getName(), theDataProvider);
	}

	public IDataProvider getDataProviderByName(String aName) {
		if (!StringUtils.hasText(aName))
			return getDefaultDataProvider();

		if (!dataProvidersByName.containsKey(aName)) {
			// Attempt to get the bean from the ApplicationContext.
			try {
				IDataProvider dataProvider = (IDataProvider) appContext.getBean(aName);
				dataProvider.initialize();
				addDataProvider(dataProvider);
				return dataProvider;
			} catch(Exception e) {
				// If no data provider is found, then assume the default.
				return getDefaultDataProvider();
			}
		} else
			return dataProvidersByName.get(aName);
	}

}
