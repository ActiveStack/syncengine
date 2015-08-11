package com.percero.amqp.handlers;

import org.apache.log4j.Logger;

import com.percero.agents.sync.vo.SyncRequest;

public class MessageHandlerTask implements Runnable {

	private static final Logger log = Logger.getLogger(MessageHandlerTask.class);

	public MessageHandlerTask(SyncMessageHandler theHandler, SyncRequest theRequest, String theReplyTo) {
		handler = theHandler;
		request = theRequest;
		replyTo = theReplyTo;
	}
	
	private SyncMessageHandler handler;
	private SyncRequest request;
	private String replyTo;
	
	public void run() {
		try {
			handler.run(request, replyTo);
		} catch(Exception e) {
			log.error("Error processing message", e);
		}
	}
}
