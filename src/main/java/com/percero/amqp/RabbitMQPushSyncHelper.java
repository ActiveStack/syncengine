package com.percero.amqp;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.log4j.Logger;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.amqp.AmqpIOException;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.listener.AbstractMessageListenerContainer;
import org.springframework.amqp.support.converter.ClassMapper;
import org.springframework.amqp.support.converter.DefaultClassMapper;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.percero.agents.sync.access.IAccessManager;
import com.percero.agents.sync.access.RedisKeyUtils;
import com.percero.agents.sync.datastore.ICacheDataStore;
import com.percero.agents.sync.services.IPushSyncHelper;
import com.percero.agents.sync.vo.BaseDataObject;
import com.percero.agents.sync.vo.IJsonObject;
import com.percero.agents.sync.vo.PushUpdateResponse;
import com.percero.agents.sync.vo.SyncResponse;
import com.rabbitmq.client.ShutdownSignalException;

import edu.emory.mathcs.backport.java.util.Arrays;

import org.slf4j.*;

@Component
public class RabbitMQPushSyncHelper implements IPushSyncHelper, ApplicationContextAware {
	
	private static Logger logger = Logger.getLogger(RabbitMQPushSyncHelper.class);

	public static final String DEFAULT_CHARSET = "UTF-8";

	private volatile String defaultCharset = DEFAULT_CHARSET;

	private ClassMapper classMapper = new DefaultClassMapper();
	
	@Autowired
	ObjectMapper objectMapper;
	
	@Autowired
	AmqpTemplate template;
	
	@Autowired @Value("$pf{gateway.rabbitmq.durable:false}")
	Boolean durableQueues = false;

	@Autowired
	IAccessManager accessManager;

	// RabbitMQ Components
	@Resource
	AmqpAdmin amqpAdmin;

	AbstractMessageListenerContainer rabbitMessageListenerContainer;
	@Resource @Qualifier("defaultListenerContainer")
	public void setRabbitMessageListenerContainer(AbstractMessageListenerContainer container){
		rabbitMessageListenerContainer = container;
	}

	// RabbitMQ environment variables.
	@Autowired @Value("$pf{gateway.rabbitmq.admin_port:15672}")
	int rabbitAdminPort = 15672;
	@Autowired @Value("$pf{gateway.rabbitmq.login:guest}")
	String rabbitLogin = "guest";
	@Autowired @Value("$pf{gateway.rabbitmq.password:guest}")
	String rabbitPassword = "guest";
	@Autowired @Value("$pf{gateway.rabbitmq.host:localhost}")
	String rabbitHost = null;
	@Autowired @Value("$pf{gateway.rabbitmq.queue_timeout:43200000}")	// 8 Hours
	long rabbitQueueTimeout = 43200000;

	@Autowired
	ICacheDataStore cacheDataStore;
	public void setCacheDataStore(ICacheDataStore cacheDataStore) {
		this.cacheDataStore = cacheDataStore;
	}

	@SuppressWarnings("rawtypes")
	protected void pushJsonToRouting(String objectJson, Class objectClass, String routingKey) {
		try{
            
            
			Message convertedMessage = toMessage(objectJson, objectClass, MessageProperties.CONTENT_TYPE_JSON);
			template.send(routingKey, convertedMessage);
		}
		catch(Exception e){
			logger.error(e.getMessage(), e);
		}
	}
	
	protected void pushMessageToRouting(Message convertedMessage, String routingKey) {
		try{
            
            
			template.send(routingKey, convertedMessage);
		}
		catch(Exception e){
			logger.error(e.getMessage(), e);
		}
	}
	
	@SuppressWarnings("rawtypes")
	protected void pushStringToRouting(String objectJson, Class objectClass, String routingKey) {
		try{
            
            
			Message convertedMessage = toMessage(objectJson, objectClass, MessageProperties.CONTENT_TYPE_BYTES);
			template.send(routingKey, convertedMessage);
		}
		catch(Exception e){
			logger.error(e.getMessage(), e);
		}
	}
	
	@SuppressWarnings("rawtypes")
	public final Message toMessage(String objectJson, Class objectClass, String contentEncoding)
			throws MessageConversionException {
                
                
		MessageProperties messageProperties = new MessageProperties();
		messageProperties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
		messageProperties.setContentEncoding(this.defaultCharset);

		return toMessage(objectJson, objectClass, messageProperties, contentEncoding);
	}
	
	@SuppressWarnings("rawtypes")
	public final Message toMessage(String objectJson, Class objectClass, MessageProperties messageProperties, String contentEncoding)
			throws MessageConversionException {
        
                
		Message message = createMessage(objectJson, objectClass, messageProperties, contentEncoding);
		return message;
	}

	@SuppressWarnings("rawtypes")
	protected Message createMessage(String aString, Class objectClass, MessageProperties messageProperties, String contentEncoding)
			throws MessageConversionException {
                
        
                
		byte[] bytes = null;
		try {
			String jsonString = aString;
			bytes = jsonString.getBytes(this.defaultCharset);
		} catch (UnsupportedEncodingException e) {
			throw new MessageConversionException("Failed to convert Message content", e);
		}
		if (bytes != null) {
			messageProperties.setContentLength(bytes.length);
		}

//		String objectClassName = objectClass.getName();
//		messageProperties.getHeaders().put("__TypeId__", objectClassName);
		classMapper.fromClass(objectClass, messageProperties);
		return new Message(bytes, messageProperties);
	}

	public void pushSyncResponseToClient(SyncResponse anObject, String clientId) {
        
        
		if (anObject != null && StringUtils.hasText(clientId)) {
			pushJsonToRouting(anObject.toJson(objectMapper), anObject.getClass(), clientId);
		}
	}
	
	@SuppressWarnings("rawtypes")
	public void pushSyncResponseToClients(SyncResponse syncResponse, Collection<String> clientIds) {
        
        
		if ( syncResponse != null && clientIds != null && !clientIds.isEmpty() ) {
			Class objectClass = syncResponse.getClass();
			
			Iterator<String> itrClientIds = clientIds.iterator();
			while (itrClientIds.hasNext()) {
				String nextClientId = itrClientIds.next();
				syncResponse.setClientId(nextClientId);
				String objectJson = syncResponse.toJson(objectMapper);
				pushJsonToRouting(objectJson, objectClass, nextClientId);
			}
		}
	}
	
	public void pushObjectToClients(Object anObject, Collection<String> listClients) {
        
        
		if (anObject != null && listClients != null && !listClients.isEmpty() ) {
			// Route to specific clients.
			// Optimization: create the JSON string of the object.
			String objectJson = null;
			if (anObject instanceof IJsonObject) {
				objectJson = ((IJsonObject)anObject).toJson();
			}
			
			PushUpdateResponse pushUpdateResponse = new PushUpdateResponse();
			pushUpdateResponse.setObjectList(new ArrayList<BaseDataObject>(1));
			pushUpdateResponse.getObjectList().add((BaseDataObject) anObject);
			
			Iterator<String> itrClients = listClients.iterator();
			while (itrClients.hasNext()) {
				String nextClient = itrClients.next();
				pushUpdateResponse.setClientId(nextClient);
				pushJsonToRouting(pushUpdateResponse.toJson(objectJson, objectMapper), PushUpdateResponse.class, nextClient);
			}
		}
	}

	@Override
	public void pushStringToRoute(String aString, String routeName) {
        
        
		if (StringUtils.hasText(routeName)) {
			pushStringToRouting(aString, String.class, routeName);
		}
	}
	
	@Override
	public Boolean removeClient(String clientId) {
        
        
		try {
			if (!cacheDataStore.getSetIsMember(RedisKeyUtils.eolClients(), clientId)) {
				logger.debug("RabbitMQ Removing Client " + clientId);
				Queue clientQueue = new Queue(clientId, durableQueues);
				amqpAdmin.declareQueue(clientQueue);
				
				// Remove ALL the messages from the queue, since this client is dead and gone.
				amqpAdmin.purgeQueue(clientId, true);
				
				// If this client hasn't already received an EOL message, send it now.
				pushJsonToRouting("{\"EOL\":true, \"clientId\":\"" + clientId + "\"}", String.class, clientId);
				cacheDataStore.addSetValue(RedisKeyUtils.eolClients(), clientId);
			}
		} catch(AmqpIOException e) {
			// Most likely due to queue already being deleted.
			if (e.getCause() instanceof IOException && e.getCause().getCause() instanceof ShutdownSignalException) {
				ShutdownSignalException sse = (ShutdownSignalException) e.getCause().getCause();
				String msg = e.getCause().getMessage();
				msg = sse.getMessage();
				
				if (msg.contains("reply-text=NOT_FOUND")) {
					// This would indicate that the queue no longer exists, so we can also remove it from the cache for final termination
					cacheDataStore.removeSetValue(RedisKeyUtils.eolClients(), clientId);
					return true;
				}
			}
			logger.debug("Unable to clear out AMQP queue: " + clientId + ": " + e.getMessage());
			return false;
		} catch(Exception e) {
			// Most likely due to queue already being deleted.
			logger.debug("Unable to clear out AMQP queue: " + clientId + ": " + e.getMessage());
			return false;
		}
		
		return true;
	}
	
	protected Boolean deleteQueue(String queue) {
        
        
		try {
			logger.debug("RabbitMQ Deleting Queue " + queue);
			Queue clientQueue = new Queue(queue, durableQueues);
			amqpAdmin.declareQueue(clientQueue);
			amqpAdmin.deleteQueue(queue);
		} catch(Exception e) {
			// Most likely due to queue already being deleted.
			logger.debug("Unable to clear out AMQP queue: " + queue + " (most likely because it no longer exists)", e);
			return false;
		}

		// Remove queue name from list of EOL Clients.
		cacheDataStore.removeSetValue(RedisKeyUtils.eolClients(), queue);
		
		return true;
	}
	
	@Override
	public Boolean renameClient(String thePreviousClientId, String clientId) {
        
        
		if (!StringUtils.hasText(thePreviousClientId)) {
			logger.warn("RabbitMQ renameClient previous client not set");
			return false;
		}
		else if (!StringUtils.hasText(clientId)) {
			logger.warn("RabbitMQ renameClient client not set");
			return false;
		}
		else if (clientId.equalsIgnoreCase(thePreviousClientId)) {
			logger.warn("RabbitMQ renameClient previous client same as client");
			return true;
		}
		
		// Attempt to move messages from the previous client queue to the new one.
		try {
			Message nextExistingMessage = null;
			
			// If we find an EOL Message, then we want to make sure it stays in
			// the previous queue.
			Message eolMessage = null;
			while ((nextExistingMessage = template.receive(thePreviousClientId)) != null) {
				JsonNode messageJsonNode = objectMapper.readTree(nextExistingMessage.getBody());
				if (messageJsonNode.has("EOL")) {
					// We found an EOL message, keep hold of it so we can resent
					// to the previous queue.
					eolMessage = nextExistingMessage;
				}
				else {
					template.send(clientId, nextExistingMessage);
				}
			}
			
			if (eolMessage != null) {
				// Make sure the EOL message is left intact in the old queue.
				template.send(thePreviousClientId, eolMessage);
			}
		} catch(AmqpIOException e) {
			// Most likely due to queue already being deleted.
			Boolean queueDoesNotExist = false;
			if (e.getCause() instanceof IOException && e.getCause().getCause() instanceof ShutdownSignalException) {
				ShutdownSignalException sse = (ShutdownSignalException) e.getCause().getCause();
				String msg = e.getCause().getMessage();
				msg = sse.getMessage();
				
				if (msg.contains("reply-text=NOT_FOUND")) {
					queueDoesNotExist = true;
				}
			}
			
			if (!queueDoesNotExist) {
				logger.debug("Unable to move messages from AMQP queue " + thePreviousClientId + " to " + clientId + ": " + e.getMessage());
			}
		} catch(Exception e) {
			// Most likely due to queue already being deleted.
			logger.debug("Unable to move messages from AMQP queue " + thePreviousClientId + " to " + clientId, e);
		}
		
		return removeClient(thePreviousClientId);
	}
	

	
	//////////////////////////////////////////////////
	//	SCHEDULED TASKS
	//////////////////////////////////////////////////
	
	private Boolean validatingQueues = false;
//	@Scheduled(fixedRate=600000)	// 10 Minutes
//	@Scheduled(fixedRate=30000)	// 30 Seconds
	@Scheduled(fixedRate=300000)	// 5 Minutes
	public void validateQueues() {
		
        
		synchronized (validatingQueues) {
			if (validatingQueues) {
				// Currently running.
				return;
			}
			else {
				validatingQueues = true;
			}
		}

		String host = rabbitHost;
		if (!StringUtils.hasText(host)) {
			// No Rabbit host configured?  Very strange, but no sense in moving forward here...
			logger.error("No RabbitMQ host configured?");
			return;
		}
		
		String uri = "http://" + host + ":" + rabbitAdminPort + "/api/queues/";
		
		DefaultHttpClient httpClient = new DefaultHttpClient();
		httpClient.getCredentialsProvider().setCredentials(new AuthScope(host, rabbitAdminPort), new UsernamePasswordCredentials(rabbitLogin, rabbitPassword));
		HttpGet httpGet = new HttpGet(uri);
		
		try {
			HttpResponse r = httpClient.execute(httpGet);
			StringWriter writer = new StringWriter();
			InputStream is = r.getEntity().getContent();
			String encoding = null;
			if (r.getEntity().getContentEncoding() != null) {
				encoding = r.getEntity().getContentEncoding().getValue();
				IOUtils.copy(is, writer, encoding);
			}
			else {
				IOUtils.copy(is, writer);
			}
			String theString = writer.toString();
			
			int numQueues = 0;
			Set<String> queueNamesToCheck = null;
			
			JsonParser parser = new JsonParser();
			JsonElement jsonQueues = parser.parse(theString);
			JsonArray jsonQueuesArray = jsonQueues.getAsJsonArray();
			
			if (jsonQueuesArray != null) {
				numQueues = jsonQueuesArray.size();
				logger.debug("Found " + numQueues + " RabbitMQ Queues to validate...");
				queueNamesToCheck = new HashSet<String>(numQueues - queueNames.size());
				
				Iterator<JsonElement> itrJsonQueuesArray = jsonQueuesArray.iterator();
				while (itrJsonQueuesArray.hasNext()) {
					JsonElement nextJsonQueue = itrJsonQueuesArray.next();
					JsonObject nextJsonQueueObject = nextJsonQueue.getAsJsonObject();
					
					JsonElement nextJsonQueueName = nextJsonQueueObject.get("name");
					String nextQueueName = null;
					if (nextJsonQueueName != null) {
						nextQueueName = nextJsonQueueName.getAsString();
					}
					else {
						continue;
					}

					if (cacheDataStore.getSetIsMember(RedisKeyUtils.eolClients(), nextQueueName)) {
						JsonElement nextJsonQueueMessages = nextJsonQueueObject.get("messages");
						int nextQueueMessages = 0;
						if (nextJsonQueueMessages != null) {
							nextQueueMessages = nextJsonQueueMessages.getAsInt();
							
							if (nextQueueMessages <= 0) {
								logger.debug("Deleting EOL empty queue " + nextQueueName);
								deleteQueue(nextQueueName);
								continue;
							}
						}
					}
					
					JsonElement nextJsonQueueConsumers = nextJsonQueueObject.get("consumers");
					if (nextJsonQueueConsumers != null) {
						// If the queue has consumers, then leave it alone.
						int numConsumers = nextJsonQueueConsumers.getAsInt();
						if (numConsumers == 0) {
							// If this queue is in the EOL list, then it can simply be deleted.
							if (cacheDataStore.getSetIsMember(RedisKeyUtils.eolClients(), nextQueueName)) {
								logger.debug("Deleting EOL no consumers queue " + nextQueueName);
								deleteQueue(nextQueueName);
								continue;
							}
						}
						else {
							// Queue has consumers, so leave alone for now.
							continue;
						}
					}
					
					JsonElement nextJsonQueueIdleSince = nextJsonQueueObject.get("idle_since");
					if (nextJsonQueueIdleSince != null) {
						try {
							String strIdleSince = nextJsonQueueIdleSince.getAsString();
							DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");
							DateTime dateTime = formatter.withOffsetParsed().parseDateTime(strIdleSince);
							if (dateTime != null) {
								DateTime currentDateTime = new DateTime(System.currentTimeMillis());
								currentDateTime = currentDateTime.toDateTime(dateTime.getZone());
								long timeDiffInMs = currentDateTime.toDate().getTime() - dateTime.toDate().getTime();
								if (timeDiffInMs < rabbitQueueTimeout) {
									// Queue has NOT timed out yet.
									continue;
								}
							}
						} catch(Exception e) {
							// Do nothing
							logger.debug("Error getting idle since for queue " + nextQueueName, e);
							continue;
						}
					}
					else {
						logger.debug("Unable to determine idle since time, ignoring queue " + nextQueueName);
						continue;
					}
					
					if (StringUtils.hasText(nextQueueName)) {
						// Check to see if this queue still valid.
						if (!queueNames.contains(nextQueueName)) {
							// Valid Queue, used by system.
							queueNamesToCheck.add(nextQueueName);
						}
					}
				}
				
				// Check to see if each queue name is a valid client.
				if (!queueNamesToCheck.isEmpty()) {
					Set<String> validClients = accessManager.validateClients(queueNamesToCheck);
					// Remove all valid clients from the queue names.
					queueNamesToCheck.removeAll(validClients);
					
					// Now delete the remaining INVALID queues.
					Iterator<String> itrQueuesToDelete = queueNamesToCheck.iterator();
					while (itrQueuesToDelete.hasNext()) {
						String nextQueueName = itrQueuesToDelete.next();
						logger.debug("RabbitMQ Logging out client " + nextQueueName);
						accessManager.logoutClient(nextQueueName, true);
					}
				}
			}

		} catch (ClientProtocolException e) {
			logger.debug(e);
		} catch (IOException e) {
			logger.debug(e);
		} catch (Exception e) {
			logger.warn(e);
		} finally {
			synchronized (validatingQueues) {
				validatingQueues = false;
			}
		}
		
		// Loop through EOL queues and delete any that now have no clients.
		
	}

	private Collection<String> queueNames = null;
	private ApplicationContext applicationContext = null;

	@SuppressWarnings("unchecked")
	@Override
	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {

		this.applicationContext = applicationContext;
		
		Map<String, Queue> queues = this.applicationContext.getBeansOfType(Queue.class);
		// Make sure these queue names are protected.
		String[] strSaveQueueNames = {"authenticateOAuthCode", "42", "authenticateOAuthAccessToken", "41",
		                              "authenticateUserAccount", "getServiceUsers", "getOAuthRequestToken",
		                              "getRegAppOAuths", "getRegisteredApplication", "getAllServiceProviders",
		                              "logoutUser", "testCall", "validateUserByToken", "17", "disconnectAuth",
		                              "reconnect", "connect", "hibernate", "upgradeClient", "disconnect", "logout", "create",
		                              "update", "processTransaction", "getChangeWatcher", "findById", "findByIds",
		                              "findByExample", "countAllByName", "getAllByName", "runQuery", "runProcess",
		                              "createObject", "putObject", "removeObject", "updatesReceived", "deletesReceived",
		                              "searchByExample", "delete", "getAccessor", "getHistory", "changeWatcher"};
		queueNames = new HashSet<String>(queues.size());
		queueNames.addAll( Arrays.asList(strSaveQueueNames) );
		
		Iterator<Map.Entry<String, Queue>> itrMapEntries = queues.entrySet().iterator();
		while (itrMapEntries.hasNext()) {
			Map.Entry<String, Queue> nextMapEntry = itrMapEntries.next();
			Queue nextQueue = nextMapEntry.getValue();
			queueNames.add(nextQueue.getName());
		}
	}

}