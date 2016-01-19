package com.percero.amqp.handlers;

import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.percero.agents.sync.access.IAccessManager;
import com.percero.amqp.IDecoder;

@Component
public class ChangeWatcherHandler {

	public static final String CHANGE_WATCHER = "changeWatcher";
	
	public ChangeWatcherHandler() {
		routingKey = CHANGE_WATCHER;
	}
	
	String routingKey;
	
	@Autowired
	IAccessManager accessManager;
	@Autowired
	AmqpAdmin amqpAdmin;
	@Autowired
	AmqpTemplate template;
	@Autowired
	IDecoder decoder;

	public Boolean handleChangeWatcher(String changeWatcherId) throws Exception {
		accessManager.recalculateChangeWatcher(changeWatcherId, null);
		return true;
	}
}
