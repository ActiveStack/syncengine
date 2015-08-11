package com.percero.agents.sync.helpers;

import java.util.Collection;
import java.util.Map;

import org.apache.log4j.Logger;

import com.percero.agents.sync.metadata.MappedField;
import com.percero.agents.sync.vo.ClassIDPair;

public class PostPutTask implements Runnable {

	private static final Logger log = Logger.getLogger(PostPutTask.class);

	public PostPutTask(PostPutHelper thePutHelper, ClassIDPair theClassIdPair, String thePusherUserId, String thePusherClientId, Boolean thePushToUser, Map<ClassIDPair, Collection<MappedField>> theChangedFields) {
		helper = thePutHelper;
		classIdPair = theClassIdPair;
		pusherUserId = thePusherUserId;
		pusherClientId = thePusherClientId;
		pushToUser = thePushToUser;
		changedFields = theChangedFields;
	}
	
	private PostPutHelper helper;
	private ClassIDPair classIdPair;
	private String pusherUserId;
	private String pusherClientId;
	private Boolean pushToUser;
	private Map<ClassIDPair, Collection<MappedField>> changedFields;
	
	public void run() {
		try {
			helper.postPutObject(classIdPair, pusherUserId, pusherClientId, pushToUser, changedFields);
		} catch(Exception e) {
			log.error("Error running process", e);
		}
	}
}
