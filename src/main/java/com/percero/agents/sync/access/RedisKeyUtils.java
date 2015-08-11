package com.percero.agents.sync.access;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.springframework.util.StringUtils;


public abstract class RedisKeyUtils {

	static final String UID = "uid:";
	static final String CHANGE_WATCHER_PREFIX = "cw:cf:";
	static final String ACCESS_JOURNAL_PREFIX = "j:a:";
	static final String TIMESTAMP = "t";
	static final String RESULT = "r";
	
	static final int INT_64 = 64;
	static final int INT_128 = 128;
	static final int INT_168 = 168;

	public static String uid(String uid) {
		return UID + uid;
	}
	
	public static String clientsLoggedIn() {
		return "c:li";
	}

	public static String clientsHibernated() {
		return "c:h";
	}
	
	public static String clientsPersistent() {
		return "c:p";
	}
	
	public static String clientsNonPersistent() {
		return "c:np";
	}
	
	public static String client(String clientId) {
		return (new StringBuilder(INT_64).append("c:").append(clientId)).toString();
	}

	public static String clientUser(String userId) {
		return (new StringBuilder(INT_64).append("c:u:").append(userId)).toString();
	}
	
	public static String userDeviceHash(String userId) {
		return (new StringBuilder(INT_64).append("u:").append(userId).append(":d")).toString();
	}
	
	public static String deviceHash(String deviceId) {
		return (new StringBuilder(INT_64).append("d:").append(deviceId)).toString();
	}
	
	public static String clientUserId(String key) {
		String[] tStrs = key.split(":");
		if (tStrs.length == 3) {
			return tStrs[2];
		}
		else {
			return null;
		}
	}
	
//	public static String transJournalClientId(String key) {
//		String[] tStrs = key.split(":");
//		if (tStrs.length == 4) {
//			return tStrs[2];
//		}
//		else {
//			return null;
//		}
//	}
//	
//	public static String transJournalTransactionId(String key) {
//		String[] tStrs = key.split(":");
//		if (tStrs.length == 4) {
//			return tStrs[3];
//		}
//		else {
//			return null;
//		}
//	}
	
	public static String deleteJournal(String clientId) {
		return (new StringBuilder(INT_64).append("j:d:").append(clientId)).toString();
	}
	
	public static String updateJournal(String clientId) {
		return (new StringBuilder(INT_64).append("j:u:").append(clientId)).toString();
	}
	
//	public static String transactionJournal(String clientId, String transactionId) {
//		return (new StringBuilder("j:t:").append(clientId).append(":").append(transactionId)).toString();
//	}
	
	public static String accessJournal(String className, String classId) {
		return (new StringBuilder(INT_128).append(ACCESS_JOURNAL_PREFIX).append(className).append(":").append(classId)).toString();
	}
	
	public static String accessJournal(String classNameAndId) {
		return (new StringBuilder(INT_128).append(ACCESS_JOURNAL_PREFIX).append(classNameAndId)).toString();
	}
	
	public static String accessJournalPrefix() {
		return ACCESS_JOURNAL_PREFIX;
	}
	
	public static String clientAccessJournal(String clientId) {
		return (new StringBuilder(INT_64).append("c:").append(ACCESS_JOURNAL_PREFIX).append(clientId)).toString();
	}
	
	public static String historicalObject(String className, String classId) {
		return (new StringBuilder(INT_128).append("ho:").append(className).append(":").append(classId)).toString();
	}
	
	public static String objectId(String className, String classId) {
		return (new StringBuilder(INT_128).append(className).append(":").append(classId)).toString();
	}
	
	public static String objectModJournal(String className, String classId) {
		return (new StringBuilder(INT_128).append("omj:").append(className).append(":").append(classId)).toString();
	}
	
	public static String dataRecord() {
		return "dr";
	}
	
	public static String changeWatcherClass(String className, String classId) {
		return (new StringBuilder(INT_128).append(CHANGE_WATCHER_PREFIX).append(className).append(":").append(classId)).toString();
	}
	
	public static String changeWatcherValueTimestamp(String fieldName, String[] params) {
		return (new StringBuilder(INT_128).append(fieldWithParams(fieldName, params)).append(":").append(TIMESTAMP)).toString();
	}
	
	public static String changeWatcherValueResult(String fieldName, String[] params) {
		return (new StringBuilder(INT_128).append(fieldWithParams(fieldName, params)).append(":").append(RESULT)).toString();
	}
	
	public static String changeWatcher(String className, String classId, String fieldName) {
		return (new StringBuilder(INT_128).append(CHANGE_WATCHER_PREFIX).append(className).append(":").append(classId).append(":").append(field(fieldName))).toString();
//		return (new StringBuilder(CHANGE_WATCHER_PREFIX).append(className).append(":").append(classId).append(":").append(fieldName)).toString();
	}
	
	public static String changeWatcherWithParams(String className, String classId, String fieldName, String[] params) {
		StringBuilder result = new StringBuilder(INT_168).append(CHANGE_WATCHER_PREFIX).append(className).append(":").append(classId).append(":").append(fieldWithParams(fieldName, params));
//		StringBuilder result = new StringBuilder(CHANGE_WATCHER_PREFIX).append(className).append(":").append(classId).append(":").append(fieldName);
//		//	nextParam.replaceAll("[ *%()]g", "-");
//		for(int i=0; i<params.length; i++) {
//			if (StringUtils.hasText(params[i])) {
//				try {
//					result = result.append(":").append(URLEncoder.encode(params[i], "UTF-8"));
//				} catch (UnsupportedEncodingException e) {
//					e.printStackTrace();
//				}
//			}
//		}
		
		return result.toString();
	}
	
	public static String field(String fieldName) {
		return fieldName;
	}
	
	public static String fieldWithParams(String fieldName, String[] params) {
		StringBuilder result = new StringBuilder(INT_128).append(fieldName);
		//	nextParam.replaceAll("[ *%()]g", "-");
		if (params != null) {
			for(int i=0; i<params.length; i++) {
				if (StringUtils.hasText(params[i])) {
					try {
						result = result.append(":").append(URLEncoder.encode(params[i], "UTF-8"));
					} catch (UnsupportedEncodingException e) {
						e.printStackTrace();
					}
				}
			}
		}
		
		return result.toString();
	}
	
	public static String clientWatcher(String changeWatcherId) {
		return (new StringBuilder(INT_128).append("client:").append(changeWatcherId)).toString();
	}
	
	public static String fieldWatcher(String changeWatcherId) {
		return (new StringBuilder(INT_128).append("cw:fld:").append(changeWatcherId)).toString();
	}
	
	public static String objectWatchedFields(String className, String classId) {
		return (new StringBuilder(INT_128).append("cw:obj:").append(className)).append(":").append(classId).toString();
	}
	
	public static String watcherField(String changeWatcherId) {
		return (new StringBuilder(INT_128).append("cw:watcher:").append(changeWatcherId)).toString();
	}
	
	public static String watcherClient(String clientId) {
		return (new StringBuilder(INT_64).append("cw:client:").append(clientId)).toString();
	}
	
	public static String changeWatcherResult(String changeWatcherId) {
		return (new StringBuilder(INT_128).append("cw:result:").append(changeWatcherId)).toString();
	}
	
	public static String changeWatcherTimestamp(String changeWatcherId) {
		return (new StringBuilder(INT_128).append("cw:time:").append(changeWatcherId)).toString();
	}
	
	public static String classIdPair(String className, String classId) {
		return (new StringBuilder(INT_128).append(className).append(":").append(classId)).toString();
	}

}
