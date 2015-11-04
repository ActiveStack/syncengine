package com.percero.agents.sync.cw;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.percero.agents.sync.metadata.MappedClass;


@Component
public class ChangeWatcherHelperFactory implements IChangeWatcherHelperFactory, ApplicationContextAware {

	private static final Logger log = Logger.getLogger(ChangeWatcherHelperFactory.class);

	ApplicationContext context;

	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		context = applicationContext;
	}

	private static IChangeWatcherHelperFactory currentInstance;
	
	public static IChangeWatcherHelperFactory getInstance() {
		return currentInstance;
	}
	
	public ChangeWatcherHelperFactory() {
		currentInstance = this;
	}
	
	public void registerChangeWatcherHelper(String category, IChangeWatcherHelper changeWatcherHelper) {
		IChangeWatcherHelper existingHelper = helperCache.get(category);
		if (existingHelper == null) {
			helperCache.put(category, changeWatcherHelper);
		}
	}

	private Map<String, IChangeWatcherHelper> helperCache = new HashMap<String, IChangeWatcherHelper>();
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public IChangeWatcherHelper getHelper(String category) {
		IChangeWatcherHelper helper = null;
		if (!helperCache.containsKey(category)) {
			// Get the appropriate Helper Component.
			try {
				Class clazz = MappedClass.forName(category);
				while (clazz != null) {
					try {
						// Get the class name only.
						String theClassName = clazz.getCanonicalName();
						int lastIndex = theClassName.lastIndexOf('.');
						if (lastIndex >= 0)
							theClassName = theClassName.substring(lastIndex+1);
						String helperClassName = (new StringBuilder(this.getClass().getPackage().getName()).append(".").append(theClassName).append("CWHelper")).toString();
						//String helperClassName = (new StringBuilder(className).append("CWHelper")).toString();
						helper = (IChangeWatcherHelper) context.getBean(StringUtils.uncapitalize(theClassName) + "CWHelper", MappedClass.forName(helperClassName));
						if (helper != null) {
							helperCache.put(category, helper);
							return helper;
						}
					} catch(Exception e) {
						// This class doesn't seem to have a CW Helper, continue up super class chain.
					}
					
					clazz = clazz.getSuperclass();
				}
			} catch(Exception e) {
//				log.error("Unable to get ChangeWatcherHelper for " + category, e);
			}
			
			// If no result, then set that up in the helperCache.
			helperCache.put(category, null);
			
			return null;
		}
		else {
			helper = helperCache.get(category);
			return helper;
		}
	}
}
