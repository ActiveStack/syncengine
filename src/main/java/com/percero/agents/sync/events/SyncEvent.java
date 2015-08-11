package com.percero.agents.sync.events;

import org.springframework.context.ApplicationEvent;

public class SyncEvent extends ApplicationEvent {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7396569546631708255L;
	public static final String SYNC_AGENT_INITIALIZED = "syncAgentInitialized";
	
	public SyncEvent(Object source,  String aType) {
		super(source);
		type = aType;
	}

	public String type = "";
}
