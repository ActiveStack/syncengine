package com.percero.amqp;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.log4j.Logger;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Instant;
import org.joda.time.MutableDateTime;
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
import org.springframework.util.StringUtils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.percero.agents.sync.access.IAccessManager;
import com.percero.agents.sync.access.RedisKeyUtils;
import com.percero.agents.sync.cw.CheckChangeWatcherMessage;
import com.percero.agents.sync.datastore.ICacheDataStore;
import com.percero.agents.sync.services.IPushSyncHelper;
import com.percero.agents.sync.vo.BaseDataObject;
import com.percero.agents.sync.vo.ClassIDPair;
import com.percero.agents.sync.vo.IJsonObject;
import com.percero.agents.sync.vo.PushUpdateResponse;
import com.percero.agents.sync.vo.SyncResponse;
import com.percero.framework.vo.IPerceroObject;
import com.rabbitmq.client.ShutdownSignalException;

import edu.emory.mathcs.backport.java.util.Arrays;

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
	@Value("$pf{gateway.rabbitmq.admin_port:15672}")
	int rabbitAdminPort;
	@Value("$pf{gateway.rabbitmq.login:guest}")
	String rabbitLogin;
	@Value("$pf{gateway.rabbitmq.password:guest}")
	String rabbitPassword;
	@Value("$pf{gateway.rabbitmq.host:localhost}")
	String rabbitHost;
	@Value("$pf{gateway.rabbitmq.queue_timeout:43200000}")	// 8 Hours
	long rabbitQueueTimeout;

	@Autowired
	ICacheDataStore cacheDataStore;
	public void setCacheDataStore(ICacheDataStore cacheDataStore) {
		this.cacheDataStore = cacheDataStore;
	}

	@SuppressWarnings("rawtypes")
	protected void pushJsonToRouting(String objectJson, Class objectClass, String routingKey) {
		try {
			Message convertedMessage = toMessage(objectJson, objectClass, MessageProperties.CONTENT_TYPE_JSON);
			template.send(routingKey, convertedMessage);
		}
		catch(Exception e) {
			logger.error(e.getMessage(), e);
		}
	}
	
	protected void pushMessageToRouting(Message convertedMessage, String routingKey) {
		try {
			template.send(routingKey, convertedMessage);
		}
		catch(Exception e) {
			logger.error(e.getMessage(), e);
		}
	}
	
	@SuppressWarnings("rawtypes")
	protected void pushStringToRouting(String objectJson, Class objectClass, String routingKey) {
		try {
			Message convertedMessage = toMessage(objectJson, objectClass, MessageProperties.CONTENT_TYPE_BYTES);
			template.send(routingKey, convertedMessage);
		}
		catch(Exception e) {
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
	public void enqueueCheckChangeWatcher(ClassIDPair classIDPair, String[] fieldNames, String[] params){
		enqueueCheckChangeWatcher(classIDPair, fieldNames, params, null);
	}
	
	@Override
	public void enqueueCheckChangeWatcher(ClassIDPair classIDPair, String[] fieldNames, String[] params, IPerceroObject oldValue){
		CheckChangeWatcherMessage message = new CheckChangeWatcherMessage();
		message.classIDPair = classIDPair;
		message.fieldNames = fieldNames;
		message.params = params;
		if(oldValue != null)
			message.oldValueJson = ((BaseDataObject)oldValue).toJson();
		template.convertAndSend("checkChangeWatcher", message);
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
	
	Boolean validatingQueues = false;
//	@Scheduled(fixedRate=600000)	// 10 Minutes
//	@Scheduled(fixedRate=30000)	// 30 Seconds
	@Scheduled(fixedRate=300000)	// 5 Minutes
	public void runValidateQueues() {
		validateQueues();
	}
	
	public boolean validateQueues() {
		
		synchronized (validatingQueues) {
			if (validatingQueues) {
				// Currently running.
				return false;
			}
			else {
				validatingQueues = true;
			}
		}

		try {
			Set<QueueProperties> queuesToCheck = retrieveQueueProperties(false);	// We only want NON system queues.
			Set<String> queueNamesToCheck = new HashSet<String>();
				
			for(QueueProperties queueProperties : queuesToCheck) {
				if (checkQueueForDeletion(queueProperties)) {
					deleteQueue(queueProperties.queueName);
				}
				else if (checkQueueForLogout(queueProperties)) {
					// This queue did not pass the test to be deleted, add
					// it to the list of valid client queue names to check
					// for logout.
					queueNamesToCheck.add(queueProperties.queueName);
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
		
		return true;
	}
	
	protected Set<QueueProperties> retrieveQueueProperties(boolean includeSystemQueues) throws JsonSyntaxException, ClientProtocolException, IOException {
		Set<QueueProperties> queuesToCheck = new HashSet<QueueProperties>();
		
		JsonParser parser = new JsonParser();
		JsonElement jsonQueues = parser.parse(retrieveQueuesJsonListAsString());
		JsonArray jsonQueuesArray = jsonQueues.getAsJsonArray();
		
		if (jsonQueuesArray != null) {
			int numQueues = 0;
			numQueues = jsonQueuesArray.size();
			logger.debug("Found " + numQueues + " RabbitMQ Queues to validate...");
			
			Iterator<JsonElement> itrJsonQueuesArray = jsonQueuesArray.iterator();
			while (itrJsonQueuesArray.hasNext()) {
				JsonElement nextJsonQueue = itrJsonQueuesArray.next();
				JsonObject nextJsonQueueObject = nextJsonQueue.getAsJsonObject();

				QueueProperties queueProperties = new QueueProperties();
				
				JsonElement nextJsonQueueName = nextJsonQueueObject.get("name");
				if (nextJsonQueueName != null) {
					queueProperties.queueName = nextJsonQueueName.getAsString();
					if (!StringUtils.hasText(queueProperties.queueName) || (!includeSystemQueues && queueNames.contains(queueProperties.queueName))) {
						// No name OR System Queue -> Ignore
						continue;
					}
				}
				else {
					// No queue name, so we can't really do much...
					continue;
				}

				JsonElement nextJsonQueueMessages = nextJsonQueueObject.get("messages");
				if (nextJsonQueueMessages != null) {
					queueProperties.numMessages = nextJsonQueueMessages.getAsInt();
				}
				
				JsonElement nextJsonQueueConsumers = nextJsonQueueObject.get("consumers");
				if (nextJsonQueueConsumers != null) {
					// If the queue has consumers, then leave it alone.
					queueProperties.numConsumers = nextJsonQueueConsumers.getAsInt();
				}
				
				JsonElement nextJsonQueueIdleSince = nextJsonQueueObject.get("idle_since");
				if (nextJsonQueueIdleSince != null) {
					try {
						String strIdleSince = nextJsonQueueIdleSince.getAsString();
						DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");
						MutableDateTime dateTime = formatter.withOffsetParsed().parseMutableDateTime(strIdleSince);
						if (dateTime != null) {
							dateTime.setZoneRetainFields(DateTimeZone.UTC);
							queueProperties.dateTimeIdleSince = dateTime.toInstant();
						}
					} catch(Exception e) {
						logger.debug("Error getting idle since for queue " + queueProperties.queueName, e);
					}
				}
				else {
					logger.debug("Unable to determine idle since time, ignoring queue " + queueProperties.queueName);
				}
				
				queueProperties.isEolClientsMember = cacheDataStore.getSetIsMember(RedisKeyUtils.eolClients(), queueProperties.queueName);

				queuesToCheck.add(queueProperties);
			}
		}
		
		return queuesToCheck;
	}
	
	protected String retrieveQueuesJsonListAsString() throws ClientProtocolException, IOException {
		String host = rabbitHost;
		if (!StringUtils.hasText(host)) {
			// No Rabbit host configured?  Very strange, but no sense in moving forward here...
			logger.error("No RabbitMQ host configured?");
			return "";
		}
		
		String uri = "http://" + host + ":" + rabbitAdminPort + "/api/queues/";
		return issueHttpCall(uri, new AuthScope(host, rabbitAdminPort), new UsernamePasswordCredentials(rabbitLogin, rabbitPassword));
	}
	
	protected String issueHttpCall(String uri, AuthScope authScope, Credentials credentials) throws ClientProtocolException, IOException {
		DefaultHttpClient httpClient = new DefaultHttpClient();
		httpClient.getCredentialsProvider().setCredentials(authScope , credentials);
		HttpGet httpGet = new HttpGet(uri);
	
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
		return writer.toString();
	}
	
	protected boolean checkQueueForDeletion(QueueProperties queueProperties) {
		return checkQueueForDeletion(queueProperties.queueName, queueProperties.numMessages, queueProperties.numConsumers, queueProperties.dateTimeIdleSince, queueProperties.isEolClientsMember);
	}

	/**
	 *  For each existing queue, the queue can be deleted when the following are true:
	 *	1. The queue is NOT a system queue {@link #queueNames}
	 *		1. The queue is in the list of EOL Clients {@link RedisKeyUtils#eolClients()} AND
	 *			1. There are NO messages left in the queue
	 *			OR
	 *			2. There are NO consumers of the queue
	 *		OR
	 *		2. The queue has been idle for at least {@link #rabbitQueueTimeout} milliseconds AND
	 *			the queue has no consumers and has contains an EOL message
	 *			In this case, we are assuming that this is a queue for a client that has gone away
	 *			and is never coming back.  This timeout should be in the number of days/weeks range.
	 *
	 * @param queueName
	 * @param numMessages
	 * @param numConsumers
	 * @param dateTimeIdleSince
	 * @param isEolClientsMember
	 * @return
	 */
	protected boolean checkQueueForDeletion(String queueName, int numMessages, int numConsumers, Instant dateTimeIdleSince, boolean isEolClientsMember) {
		if (queueNames.contains(queueName)) {
			return false;
		}

		if (isEolClientsMember && (numMessages == 0 || numConsumers == 0)) {
			logger.debug("Deleting EOL empty queue " + queueName + ": EOL Client with " + numMessages + " Messages and " + numConsumers + " Consumers");
			return true;
		}
		
		if (numConsumers == 0 && numMessages > 0 && queueHasTimedOut(queueName, dateTimeIdleSince)) {
			// If this queue has an EOL message and has no
			// consumers, then we can safely delete it.
			// If we have gotten here then client is NOT in
			// the EOL list, thus it is old and can safely
			// be deleted.
			Message nextExistingMessage = null;
			
			// If we find an EOL Message, 
			Message eolMessage = null;
			List<Message> messagesToRequeue = new ArrayList<>();
			while ((nextExistingMessage = template.receive(queueName)) != null) {
				try {
					JsonNode messageJsonNode = objectMapper.readTree(nextExistingMessage.getBody());
					if (messageJsonNode.has("EOL") && messageJsonNode.get("EOL").getBooleanValue()) {
						// We found an EOL message -> This queue to be deleted.
						eolMessage = nextExistingMessage;
						break;
					}
					else {
						// Re-queue this message...
						messagesToRequeue.add(nextExistingMessage);
					}
				} catch (IOException e) {
					logger.warn("Error reading queue " + queueName + " message, unable to process");
				}
			}
			
			if (eolMessage != null) {
				logger.debug("Deleting EOL no consumers queue " + queueName + ": EOL Client WITH EOL Message");
				return true;
			}
			else {
				// Need to re-queue the messages.
				for(Message nextMessage : messagesToRequeue) {
					template.send(queueName, nextMessage);
				}
			}
		}

		return false;
	}

	protected boolean checkQueueForLogout(QueueProperties queueProperties) {
		return checkQueueForLogout(queueProperties.queueName, queueProperties.numMessages, queueProperties.numConsumers, queueProperties.dateTimeIdleSince, queueProperties.isEolClientsMember);
	}
	
	/**
	 * If a queue meets this criteria, then it should be further investigated for automatic logout.
	 *	1. The queue is NOT a system queue {@link #queueNames}
	 *	2. Is NOT a member of EOL Clients list
	 *	3. Has NO consumers
	 *	4. The queue has been idle for at least {@link #rabbitQueueTimeout} milliseconds
	 *		In this case, we are assuming that this is a queue for a client that has gone away
	 *		and is never coming back.  This timeout should be in the number of days/weeks range.
	 *
	 * @param queueName
	 * @param numMessages
	 * @param numConsumers
	 * @param dateTimeIdleSince
	 * @param isEolClientsMember
	 * @return
	 */
	protected boolean checkQueueForLogout(String queueName, int numMessages, int numConsumers, Instant dateTimeIdleSince, boolean isEolClientsMember) {
		if (queueNames.contains(queueName)) {
			return false;
		}

		if (!isEolClientsMember) {
			if (numConsumers <= 0) {
				if (queueHasTimedOut(queueName, dateTimeIdleSince)) {
					// Queue HAS timed out.
					logger.debug("Queue Timed Out: " + queueName);
					return true;
				}
			}
		}
		
		return false;
	}

	/**
	 *	If the queue has been idle for at least {@link #rabbitQueueTimeout} milliseconds then returns true.
	 *
	 * @param queueName
	 * @param dateTimeIdleSince
	 * @return
	 */
	protected boolean queueHasTimedOut(String queueName, Instant dateTimeIdleSince) {
		if (queueNames.contains(queueName)) {
			// System queues can NEVER timeout.
			return false;
		}

		boolean queueHasTimedOut = false;
		if (dateTimeIdleSince != null) {
			DateTime currentDateTime = new DateTime(System.currentTimeMillis());
			currentDateTime = currentDateTime.toDateTime(dateTimeIdleSince.getZone());
			long timeDiffInMs = currentDateTime.toDate().getTime() - dateTimeIdleSince.toDate().getTime();
			if (timeDiffInMs > rabbitQueueTimeout) {
				// Queue HAS timed out.
				logger.debug("Queue Timed Out: " + queueName);

				queueHasTimedOut = true;
			}
		}
		return queueHasTimedOut;
	}

	Collection<String> queueNames = null;
	private ApplicationContext applicationContext = null;

	@SuppressWarnings("unchecked")
	@Override
	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {

		this.applicationContext = applicationContext;
		
		Map<String, Queue> queues = this.applicationContext.getBeansOfType(Queue.class);
		// Make sure these queue names are protected.
		String[] strSaveQueueNames = { "authenticateOAuthCode", "42", "register", "authenticate", "reauthenticate",
				"authenticateOAuthAccessToken", "41", "authenticateUserAccount", "getServiceUsers",
				"getOAuthRequestToken", "getRegAppOAuths", "getRegisteredApplication", "getAllServiceProviders",
				"logoutUser", "testCall", "validateUserByToken", "17", "disconnectAuth", "reconnect", "connect",
				"hibernate", "upgradeClient", "disconnect", "logout", "create", "update", "processTransaction",
				"getChangeWatcher", "findById", "findByIds", "findByExample", "countAllByName", "getAllByName",
				"runQuery", "runProcess", "createObject", "putObject", "removeObject", "updatesReceived",
				"deletesReceived", "searchByExample", "delete", "getAccessor", "getHistory", "changeWatcher" };
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