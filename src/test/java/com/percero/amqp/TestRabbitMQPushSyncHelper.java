package com.percero.amqp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.codehaus.jackson.map.ObjectMapper;
import org.joda.time.Duration;
import org.joda.time.Instant;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Message;

import com.google.gson.JsonSyntaxException;
import com.percero.agents.sync.access.IAccessManager;
import com.percero.agents.sync.datastore.ICacheDataStore;

public class TestRabbitMQPushSyncHelper {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}
	
	RabbitMQPushSyncHelper pushSyncHelper;
	final String jsonMessages = "[{\"memory\":21920,\"messages\":0,\"messages_details\":{\"rate\":0.0},\"messages_ready\":0,\"messages_ready_details\":{\"rate\":0.0},\"messages_unacknowledged\":0,\"messages_unacknowledged_details\":{\"rate\":0.0},\"idle_since\":\"2016-03-11 0:33:28\",\"consumer_utilisation\":null,\"policy\":null,\"exclusive_consumer_tag\":null,\"consumers\":2,\"recoverable_slaves\":null,\"state\":\"running\",\"messages_ram\":0,\"messages_ready_ram\":0,\"messages_unacknowledged_ram\":0,\"messages_persistent\":0,\"message_bytes\":0,\"message_bytes_ready\":0,\"message_bytes_unacknowledged\":0,\"message_bytes_ram\":0,\"message_bytes_persistent\":0,\"head_message_timestamp\":null,\"disk_reads\":0,\"disk_writes\":0,\"backing_queue_status\":{\"mode\":\"default\",\"q1\":0,\"q2\":0,\"delta\":[\"delta\",\"undefined\",0,\"undefined\"],\"q3\":0,\"q4\":0,\"len\":0,\"target_ram_count\":\"infinity\",\"next_seq_id\":0,\"avg_ingress_rate\":0.0,\"avg_egress_rate\":0.0,\"avg_ack_ingress_rate\":0.0,\"avg_ack_egress_rate\":0.0},\"name\":\"17\",\"vhost\":\"/\",\"durable\":true,\"auto_delete\":false,\"exclusive\":false,\"arguments\":{},\"node\":\"testnode\"},{\"memory\":21920,\"messages\":0,\"messages_details\":{\"rate\":0.0},\"messages_ready\":0,\"messages_ready_details\":{\"rate\":0.0},\"messages_unacknowledged\":0,\"messages_unacknowledged_details\":{\"rate\":0.0},\"idle_since\":\"2016-03-11 0:33:28\",\"consumer_utilisation\":null,\"policy\":null,\"exclusive_consumer_tag\":null,\"consumers\":0,\"recoverable_slaves\":null,\"state\":\"running\",\"messages_ram\":0,\"messages_ready_ram\":0,\"messages_unacknowledged_ram\":0,\"messages_persistent\":0,\"message_bytes\":0,\"message_bytes_ready\":0,\"message_bytes_unacknowledged\":0,\"message_bytes_ram\":0,\"message_bytes_persistent\":0,\"head_message_timestamp\":null,\"disk_reads\":0,\"disk_writes\":0,\"backing_queue_status\":{\"mode\":\"default\",\"q1\":0,\"q2\":0,\"delta\":[\"delta\",\"undefined\",0,\"undefined\"],\"q3\":0,\"q4\":0,\"len\":0,\"target_ram_count\":\"infinity\",\"next_seq_id\":0,\"avg_ingress_rate\":0.0,\"avg_egress_rate\":0.0,\"avg_ack_ingress_rate\":0.0,\"avg_ack_egress_rate\":0.0},\"name\":\"41\",\"vhost\":\"/\",\"durable\":true,\"auto_delete\":false,\"exclusive\":false,\"arguments\":{},\"node\":\"testnode\"},{\"memory\":21920,\"messages\":0,\"messages_details\":{\"rate\":0.0},\"messages_ready\":0,\"messages_ready_details\":{\"rate\":0.0},\"messages_unacknowledged\":0,\"messages_unacknowledged_details\":{\"rate\":0.0},\"idle_since\":\"2016-03-11 0:33:27\",\"consumer_utilisation\":null,\"policy\":null,\"exclusive_consumer_tag\":null,\"consumers\":0,\"recoverable_slaves\":null,\"state\":\"running\",\"messages_ram\":0,\"messages_ready_ram\":0,\"messages_unacknowledged_ram\":0,\"messages_persistent\":0,\"message_bytes\":0,\"message_bytes_ready\":0,\"message_bytes_unacknowledged\":0,\"message_bytes_ram\":0,\"message_bytes_persistent\":0,\"head_message_timestamp\":null,\"disk_reads\":0,\"disk_writes\":0,\"backing_queue_status\":{\"mode\":\"default\",\"q1\":0,\"q2\":0,\"delta\":[\"delta\",\"undefined\",0,\"undefined\"],\"q3\":0,\"q4\":0,\"len\":0,\"target_ram_count\":\"infinity\",\"next_seq_id\":0,\"avg_ingress_rate\":0.0,\"avg_egress_rate\":0.0,\"avg_ack_ingress_rate\":0.0,\"avg_ack_egress_rate\":0.0},\"name\":\"42\",\"vhost\":\"/\",\"durable\":true,\"auto_delete\":false,\"exclusive\":false,\"arguments\":{},\"node\":\"testnode\"},{\"memory\":21848,\"message_stats\":{\"ack\":1361,\"ack_details\":{\"rate\":0.0},\"deliver\":1361,\"deliver_details\":{\"rate\":0.0},\"deliver_get\":1361,\"deliver_get_details\":{\"rate\":0.0},\"publish\":1362,\"publish_details\":{\"rate\":0.0}},\"messages\":1,\"messages_details\":{\"rate\":0.0},\"messages_ready\":1,\"messages_ready_details\":{\"rate\":0.0},\"messages_unacknowledged\":0,\"messages_unacknowledged_details\":{\"rate\":0.0},\"idle_since\":\"2016-03-11 0:33:31\",\"consumer_utilisation\":null,\"policy\":null,\"exclusive_consumer_tag\":null,\"consumers\":0,\"recoverable_slaves\":null,\"state\":\"running\",\"messages_ram\":1,\"messages_ready_ram\":1,\"messages_unacknowledged_ram\":0,\"messages_persistent\":0,\"message_bytes\":191,\"message_bytes_ready\":191,\"message_bytes_unacknowledged\":0,\"message_bytes_ram\":191,\"message_bytes_persistent\":0,\"head_message_timestamp\":null,\"disk_reads\":0,\"disk_writes\":0,\"backing_queue_status\":{\"mode\":\"default\",\"q1\":0,\"q2\":0,\"delta\":[\"delta\",\"undefined\",0,\"undefined\"],\"q3\":0,\"q4\":1,\"len\":1,\"target_ram_count\":\"infinity\",\"next_seq_id\":1362,\"avg_ingress_rate\":0.0005576266319825132,\"avg_egress_rate\":6.819506093702665e-109,\"avg_ack_ingress_rate\":6.819506093702665e-109,\"avg_ack_egress_rate\":6.824693279456112e-109},\"name\":\"aa275b1eeb0f340a6430180c9a703795\",\"vhost\":\"/\",\"durable\":false,\"auto_delete\":false,\"exclusive\":false,\"arguments\":{},\"node\":\"testnode\"}]";

	@Before
	public void setUp() throws Exception {
		pushSyncHelper = Mockito.mock(RabbitMQPushSyncHelper.class);
		pushSyncHelper.template = Mockito.mock(AmqpTemplate.class);
		pushSyncHelper.objectMapper = new ObjectMapper();
		pushSyncHelper.accessManager = Mockito.mock(IAccessManager.class);
		pushSyncHelper.cacheDataStore = Mockito.mock(ICacheDataStore.class);
	}

	@After
	public void tearDown() throws Exception {
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testValidateQueues() {

		// Should not run when it is currently being run.
		pushSyncHelper.validatingQueues = true;
		boolean result = pushSyncHelper.validateQueues();
		assertFalse(result);
		pushSyncHelper.validatingQueues = false;
		
		// Setup QueueProperties to check
		final Set<QueueProperties> queuePropertiesSet = new HashSet<QueueProperties>();
		
		QueueProperties queueProperties = new QueueProperties();
		queueProperties.setQueueName("QUEUE_1");
		queuePropertiesSet.add(queueProperties);
		QueueProperties queueProperties2 = new QueueProperties();
		queueProperties2.setQueueName("QUEUE_2");
		queuePropertiesSet.add(queueProperties2);
		
		Mockito.when(pushSyncHelper.validateQueues()).thenCallRealMethod();
		try {
			Mockito.when(pushSyncHelper.retrieveQueueProperties(Mockito.anyBoolean()))
					.thenAnswer(new Answer<Set<QueueProperties>>() {
						@Override
						public Set<QueueProperties> answer(InvocationOnMock invocation) throws Throwable {
							return queuePropertiesSet;
						}
					});
		} catch (JsonSyntaxException | IOException e) {
			fail(e.getMessage());
		}
		
		Set<String> validClients = new HashSet<String>();
		try {
			Mockito.when(pushSyncHelper.accessManager.validateClients(Mockito.anyCollection())).thenReturn(validClients);
		} catch (Exception e) {
			fail(e.getMessage());
		}

		// If queue is a valid client, then do NOT log them out.
		try {
			Mockito.verify(pushSyncHelper.accessManager, Mockito.never()).logoutClient(Mockito.anyString(), Mockito.anyBoolean());
		} catch (Exception e) {
			fail(e.getMessage());
		}

		validClients.add("QUEUE_1");
		validClients.add("QUEUE_2");
		Mockito.when(pushSyncHelper.checkQueueForDeletion(Mockito.any(QueueProperties.class))).thenReturn(true).thenReturn(false);
		Mockito.when(pushSyncHelper.checkQueueForLogout(Mockito.any(QueueProperties.class))).thenReturn(true);

		result = pushSyncHelper.validateQueues();
		assertTrue(result);
		// Make sure we finish cleanly.
		assertFalse(pushSyncHelper.validatingQueues);
		
		Mockito.verify(pushSyncHelper, Mockito.times(2)).checkQueueForDeletion(Mockito.any(QueueProperties.class));
		Mockito.verify(pushSyncHelper, Mockito.times(1)).checkQueueForLogout(Mockito.any(QueueProperties.class));
		Mockito.verify(pushSyncHelper, Mockito.times(1)).deleteQueue(Mockito.anyString());
		try {
			Mockito.verify(pushSyncHelper.accessManager, Mockito.never()).logoutClient(Mockito.anyString(), Mockito.anyBoolean());
		} catch (Exception e) {
			fail(e.getMessage());
		}

		// If queue is NOT a valid client, then DO log them out.
		validClients.clear();
		Mockito.when(pushSyncHelper.checkQueueForDeletion(Mockito.any(QueueProperties.class))).thenReturn(true).thenReturn(false);
		Mockito.when(pushSyncHelper.checkQueueForLogout(Mockito.any(QueueProperties.class))).thenReturn(true);
		result = pushSyncHelper.validateQueues();
		assertTrue(result);
		// Make sure we finish cleanly.
		assertFalse(pushSyncHelper.validatingQueues);
		
		Mockito.verify(pushSyncHelper, Mockito.times(4)).checkQueueForDeletion(Mockito.any(QueueProperties.class));
		Mockito.verify(pushSyncHelper, Mockito.times(2)).checkQueueForLogout(Mockito.any(QueueProperties.class));
		Mockito.verify(pushSyncHelper, Mockito.times(2)).deleteQueue(Mockito.anyString());
		try {
			Mockito.verify(pushSyncHelper.accessManager, Mockito.times(1)).logoutClient(Mockito.anyString(), Mockito.anyBoolean());
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testCheckQueueForDeletion() {
		String queueName = "QUEUE";
		int numMessages = 0;
		int numConsumers = 0;
		Instant dateTimeIdleSince = Instant.now().minus(Duration.standardDays(7).getMillis());
		boolean isEolClientsMember = false;
		
		Mockito.when(pushSyncHelper.checkQueueForDeletion(Mockito.anyString(), Mockito.anyInt(), Mockito.anyInt(), Mockito.any(Instant.class), Mockito.anyBoolean())).thenCallRealMethod();
		Mockito.when(pushSyncHelper.queueHasTimedOut(Mockito.anyString(), Mockito.any(Instant.class))).thenCallRealMethod();
		
		// If queueName is in QueueNames, then false
		pushSyncHelper.queueNames = new HashSet<>();
		pushSyncHelper.queueNames.add(queueName);
		boolean result = pushSyncHelper.checkQueueForDeletion(queueName, numMessages, numConsumers, dateTimeIdleSince, isEolClientsMember);
		assertFalse(result);
		
		// If no consumers and IS EOL client with no messages, then true
		// if isEolClientsMember AND (numMessages <= 0 OR numConsumers == 0), then true
		pushSyncHelper.queueNames = new HashSet<>();
		isEolClientsMember = true;
		numConsumers = 0;
		numMessages = 5;
		result = pushSyncHelper.checkQueueForDeletion(queueName, numMessages, numConsumers, dateTimeIdleSince, isEolClientsMember);
		assertTrue(result);
		
		numConsumers = 2;
		numMessages = 0;
		result = pushSyncHelper.checkQueueForDeletion(queueName, numMessages, numConsumers, dateTimeIdleSince, isEolClientsMember);
		assertTrue(result);
		
		numConsumers = 0;
		numMessages = 0;
		result = pushSyncHelper.checkQueueForDeletion(queueName, numMessages, numConsumers, dateTimeIdleSince, isEolClientsMember);
		assertTrue(result);
		
		// If NOT EOL Client Member AND Consumers, then false
		isEolClientsMember = false;
		numConsumers = 5;
		result = pushSyncHelper.checkQueueForDeletion(queueName, numMessages, numConsumers, dateTimeIdleSince, isEolClientsMember);
		assertFalse(result);
		
		// If numConsumers == 0 AND numMessages > 0 AND queue has NOT timed out, then false
		isEolClientsMember = false;
		numMessages = 12;
		numConsumers = 0;
		dateTimeIdleSince = Instant.now();
		pushSyncHelper.rabbitQueueTimeout = 50 * 1000;	// 50 s
		result = pushSyncHelper.checkQueueForDeletion(queueName, numMessages, numConsumers, dateTimeIdleSince, isEolClientsMember);
		assertFalse(result);
		
		// If numConsumers == 0 AND numMessages > 0 AND queue HAS timed out AND queue contains NO EOL message, then false
		isEolClientsMember = false;
		numMessages = 12;
		numConsumers = 0;
		dateTimeIdleSince = Instant.now().minus(Duration.standardDays(7).getMillis());
		pushSyncHelper.rabbitQueueTimeout = 500;	// 500 ms
		result = pushSyncHelper.checkQueueForDeletion(queueName, numMessages, numConsumers, dateTimeIdleSince, isEolClientsMember);
		assertFalse(result);
		
		// If numConsumers == 0 AND numMessages > 0 AND queue HAS timed out AND queue contains EOL message, then true
		isEolClientsMember = false;
		numMessages = 12;
		numConsumers = 0;
		Message eolMessage = new Message("{\"EOL\": true}".getBytes(), null);
		Mockito.when(pushSyncHelper.template.receive(Mockito.anyString())).thenReturn(eolMessage);
		result = pushSyncHelper.checkQueueForDeletion(queueName, numMessages, numConsumers, dateTimeIdleSince, isEolClientsMember);
		assertTrue(result);
		
		// Valid messages should be requeued.
		isEolClientsMember = false;
		numMessages = 12;
		numConsumers = 0;
		Message notEolMessage = new Message("{\"OTHER\": true}".getBytes(), null);
		Mockito.when(pushSyncHelper.template.receive(Mockito.anyString())).thenReturn(notEolMessage).thenReturn(null);
		Mockito.verify(pushSyncHelper.template, Mockito.never()).send(Mockito.anyString(), Mockito.any(Message.class));
		result = pushSyncHelper.checkQueueForDeletion(queueName, numMessages, numConsumers, dateTimeIdleSince, isEolClientsMember);
		Mockito.verify(pushSyncHelper.template, Mockito.atLeastOnce()).send(Mockito.anyString(), Mockito.any(Message.class));
		assertFalse(result);
	}

	@Test
	public void testCheckQueueForLogout() {
		String queueName = "QUEUE";
		int numMessages = 0;
		int numConsumers = 0;
		Instant dateTimeIdleSince = Instant.now().minus(Duration.standardDays(7).getMillis());
		boolean isEolClientsMember = false;

		Mockito.when(pushSyncHelper.checkQueueForLogout(Mockito.anyString(), Mockito.anyInt(), Mockito.anyInt(), Mockito.any(Instant.class), Mockito.anyBoolean())).thenCallRealMethod();
		Mockito.when(pushSyncHelper.queueHasTimedOut(Mockito.anyString(), Mockito.any(Instant.class))).thenCallRealMethod();
		
		// If queueName is in QueueNames, then false
		pushSyncHelper.queueNames = new HashSet<>();
		pushSyncHelper.queueNames.add(queueName);
		boolean result = pushSyncHelper.checkQueueForLogout(queueName, numMessages, numConsumers, dateTimeIdleSince, isEolClientsMember);
		assertFalse(result);
		
		// If EOL Client Member, then false
		pushSyncHelper.queueNames = new HashSet<>();
		isEolClientsMember = true;
		result = pushSyncHelper.checkQueueForLogout(queueName, numMessages, numConsumers, dateTimeIdleSince, isEolClientsMember);
		assertFalse(result);
		
		// If NOT EOL Client Member AND numConsumers > 0, then false
		pushSyncHelper.queueNames = new HashSet<>();
		isEolClientsMember = false;
		numConsumers = 5;
		result = pushSyncHelper.checkQueueForLogout(queueName, numMessages, numConsumers, dateTimeIdleSince, isEolClientsMember);
		assertFalse(result);
		
		// If NOT EOL Client Member AND numConsumers <= 0 AND dateTimeIdleSince IS NULL, then false
		pushSyncHelper.queueNames = new HashSet<>();
		isEolClientsMember = false;
		numConsumers = 0;
		dateTimeIdleSince = null;
		result = pushSyncHelper.checkQueueForLogout(queueName, numMessages, numConsumers, dateTimeIdleSince, isEolClientsMember);
		assertFalse(result);
		
		// If NOT EOL Client Member AND numConsumers <= 0 AND time difference > rabbitQueueTimeout, then true
		pushSyncHelper.queueNames = new HashSet<>();
		isEolClientsMember = false;
		numConsumers = 0;
		dateTimeIdleSince = Instant.now().minus(Duration.standardDays(7).getMillis());
		pushSyncHelper.rabbitQueueTimeout = 500;	// 500 ms
		result = pushSyncHelper.checkQueueForLogout(queueName, numMessages, numConsumers, dateTimeIdleSince, isEolClientsMember);
		assertTrue(result);
		
		// If NOT EOL Client Member AND numConsumers <= 0 AND time difference < rabbitQueueTimeout, then false
		pushSyncHelper.queueNames = new HashSet<>();
		isEolClientsMember = false;
		numConsumers = 0;
		dateTimeIdleSince = Instant.now();
		pushSyncHelper.rabbitQueueTimeout = 50 * 1000;	// 50 s
		result = pushSyncHelper.checkQueueForLogout(queueName, numMessages, numConsumers, dateTimeIdleSince, isEolClientsMember);
		assertFalse(result);
	}
	
	@Test
	public void testQueueHasTimedOut() {
		String queueName = "QUEUE";
		Instant dateTimeIdleSince = Instant.now().minus(Duration.standardDays(7).getMillis());

		Mockito.when(pushSyncHelper.queueHasTimedOut(Mockito.anyString(), Mockito.any(Instant.class))).thenCallRealMethod();

		// If queueName is in QueueNames, then false
		pushSyncHelper.queueNames = new HashSet<>();
		pushSyncHelper.queueNames.add(queueName);
		boolean result = pushSyncHelper.queueHasTimedOut(queueName, dateTimeIdleSince);
		assertFalse(result);

		// If time difference > rabbitQueueTimeout, then true
		pushSyncHelper.queueNames = new HashSet<>();
		dateTimeIdleSince = Instant.now().minus(Duration.standardDays(7).getMillis());
		pushSyncHelper.rabbitQueueTimeout = 500;	// 500 ms
		result = pushSyncHelper.queueHasTimedOut(queueName, dateTimeIdleSince);
		assertTrue(result);
		
		// If time difference < rabbitQueueTimeout, then false
		pushSyncHelper.queueNames = new HashSet<>();
		dateTimeIdleSince = Instant.now();
		pushSyncHelper.rabbitQueueTimeout = 50 * 1000;	// 50 s
		result = pushSyncHelper.queueHasTimedOut(queueName, dateTimeIdleSince);
		assertFalse(result);
	}
	
	@Test
	public void testRetrieveQueuesJsonListAsString() {
		
		try {
			Mockito.when(pushSyncHelper.retrieveQueuesJsonListAsString()).thenCallRealMethod();

			// If NO rabbit host, then should return an empty string
			pushSyncHelper.rabbitHost = null;
			String result = pushSyncHelper.retrieveQueuesJsonListAsString();
			assertEquals("", result);
			
			pushSyncHelper.rabbitHost = "";
			result = pushSyncHelper.retrieveQueuesJsonListAsString();
			assertEquals("", result);
			
			pushSyncHelper.rabbitHost = "localhost";
			pushSyncHelper.rabbitAdminPort = 15672;
			pushSyncHelper.rabbitLogin = "guest";
			pushSyncHelper.rabbitPassword = "guest";
			
			final String uri = "http://" + pushSyncHelper.rabbitHost + ":" + pushSyncHelper.rabbitAdminPort + "/api/queues/";
			Mockito.when(pushSyncHelper.issueHttpCall(Mockito.anyString(), Mockito.any(AuthScope.class), Mockito.any(Credentials.class))).then(new Answer<String>() {
				@Override
				public String answer(InvocationOnMock invocation) throws Throwable {
					if (uri.equals(invocation.getArguments()[0])) {
						return jsonMessages;
					}
					else {
						return "";
					}
				}
			});
			
			String jsonList = pushSyncHelper.retrieveQueuesJsonListAsString();
			
			assertEquals(jsonMessages, jsonList);
		} catch (IOException e) {
			fail(e.getMessage());
		}
		
	}
	
	@Test
	public void testRetrieveQueueProperties() {
		// TODO: Get a valid Rabbit Message JSON response (by setting rabbitHost and port), then use that as seed data for this method.
		pushSyncHelper.rabbitHost = "localhost";
		pushSyncHelper.rabbitAdminPort = 15672;
		pushSyncHelper.rabbitLogin = "guest";
		pushSyncHelper.rabbitPassword = "guest";
		pushSyncHelper.queueNames = new HashSet<String>();
		pushSyncHelper.queueNames.add("41");
		
		Mockito.when(pushSyncHelper.cacheDataStore.getSetIsMember(Mockito.anyString(), Mockito.anyObject())).thenAnswer(new Answer<Boolean>() {
			@Override
			public Boolean answer(InvocationOnMock invocation) throws Throwable {
				if("42".equals(invocation.getArguments()[1])) {
					return true;
				}
				else {
					return false;
				}
			}
		});
		
		try {
			Mockito.when(pushSyncHelper.retrieveQueuesJsonListAsString()).thenReturn(jsonMessages);
			Mockito.when(pushSyncHelper.retrieveQueueProperties(Mockito.anyBoolean())).thenCallRealMethod();
			Set<QueueProperties> queueProperties = pushSyncHelper.retrieveQueueProperties(false);
			assertNotNull(queueProperties);
			// There are 4 queues in the JSON, but we do NOT want the queue named "41" since that has been registered as a "system" queue.
			assertEquals(3, queueProperties.size());
			
			for(QueueProperties qp : queueProperties) {
				if ("42".equals(qp.getQueueName())) {
					assertEquals(0, qp.getNumConsumers());
					assertEquals(0, qp.getNumMessages());
					assertTrue(qp.isEolClientsMember());
					assertEquals(new Instant(1457656407000l), qp.getDateTimeIdleSince());
				}
				else if ("17".equals(qp.getQueueName())) {
					assertEquals(2, qp.getNumConsumers());
					assertEquals(0, qp.getNumMessages());
					assertFalse(qp.isEolClientsMember());
					assertEquals(new Instant(1457656408000l), qp.getDateTimeIdleSince());
				}
				else if ("aa275b1eeb0f340a6430180c9a703795".equals(qp.getQueueName())) {
					assertEquals(0, qp.getNumConsumers());
					assertEquals(1, qp.getNumMessages());
					assertFalse(qp.isEolClientsMember());
					assertEquals(new Instant(1457656411000l), qp.getDateTimeIdleSince());
				}
			}
			
		} catch (IOException e) {
			fail(e.getMessage());
		}
		
	}

}
