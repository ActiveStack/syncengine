package com.percero.agents.sync.helpers;

import org.apache.log4j.Logger;

import com.percero.framework.vo.IPerceroObject;

public class PostDeleteTask implements Runnable {

	private static final Logger log = Logger.getLogger(PostDeleteTask.class);

	public PostDeleteTask(PostDeleteHelper theDeleteHelper, IPerceroObject thePerceroObject, String thePusherUserId, String thePusherClientId, Boolean thePushToUser) {
		helper = theDeleteHelper;
		perceroObject = thePerceroObject;
		pusherUserId = thePusherUserId;
		pusherClientId = thePusherClientId;
		pushToUser = thePushToUser;
	}
	
	private PostDeleteHelper helper;
	private IPerceroObject perceroObject;
	private String pusherUserId;
	private String pusherClientId;
	private Boolean pushToUser;
	
	public void run() {
		try {
			helper.postDeleteObject(perceroObject, pusherUserId, pusherClientId, pushToUser);
		} catch(Exception e) {
			log.error("Error running process", e);
		}
	}
}
