package com.percero.amqp;

import org.joda.time.Instant;

/**
 * Holds basic information about a Message Queue, such as name and number of
 * messages.
 * 
 * @author Collin Brown
 *
 */
public class QueueProperties {

	String queueName = null;
	int numMessages = 0;
	int numConsumers = 0;
	Instant dateTimeIdleSince = null;
	boolean isEolClientsMember = false;

	public String getQueueName() {
		return queueName;
	}
	public void setQueueName(String queueName) {
		this.queueName = queueName;
	}
	public int getNumMessages() {
		return numMessages;
	}
	public void setNumMessages(int numMessages) {
		this.numMessages = numMessages;
	}
	public int getNumConsumers() {
		return numConsumers;
	}
	public void setNumConsumers(int numConsumers) {
		this.numConsumers = numConsumers;
	}
	public Instant getDateTimeIdleSince() {
		return dateTimeIdleSince;
	}
	public void setDateTimeIdleSince(Instant dateTimeIdleSince) {
		this.dateTimeIdleSince = dateTimeIdleSince;
	}
	public boolean isEolClientsMember() {
		return isEolClientsMember;
	}
	public void setEolClientsMember(boolean isEolClientsMember) {
		this.isEolClientsMember = isEolClientsMember;
	}
	public QueueProperties() {
		// TODO Auto-generated constructor stub
	}

}
