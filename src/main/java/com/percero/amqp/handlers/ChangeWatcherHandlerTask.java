package com.percero.amqp.handlers;

import org.apache.log4j.Logger;

public class ChangeWatcherHandlerTask implements Runnable {

	private static final Logger log = Logger.getLogger(ChangeWatcherHandlerTask.class);

	public ChangeWatcherHandlerTask(String changeWatcherId) {
		this.changeWatcherId = changeWatcherId;
	}
	
	private String changeWatcherId;
	
	public void run() {
		try {
			ChangeWatcherHandler handler = new ChangeWatcherHandler();
			handler.handleChangeWatcher(changeWatcherId);
		} catch(Exception e) {
			log.error("Error processing message", e);
		}
	}
}
