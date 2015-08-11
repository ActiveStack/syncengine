package com.percero.agents.sync.helpers;

import org.apache.log4j.Logger;

import com.percero.framework.vo.IPerceroObject;

public class PostCreateTask implements Runnable {

	private static final Logger log = Logger.getLogger(PostCreateTask.class);

	public PostCreateTask(PostCreateHelper theCreateHelper, IPerceroObject thePerceroObject, String thePusherUserId, String thePusherClientId, Boolean thePushToUser) {
		helper = theCreateHelper;
		perceroObject = thePerceroObject;
		pusherUserId = thePusherUserId;
		pusherClientId = thePusherClientId;
		pushToUser = thePushToUser;
	}
	
	private PostCreateHelper helper;
	private IPerceroObject perceroObject;
	private String pusherUserId;
	private String pusherClientId;
	private Boolean pushToUser;
	
	public void run() {
		try {
			helper.postCreateObject(perceroObject, pusherUserId, pusherClientId, pushToUser);
		} catch(Exception e) {
			log.error("Error running process", e);
		}
	}
}
