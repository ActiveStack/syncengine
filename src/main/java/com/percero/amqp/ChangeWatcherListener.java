package com.percero.amqp;

import org.apache.log4j.Logger;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.ChannelAwareMessageListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.percero.agents.sync.access.IAccessManager;
import com.percero.agents.sync.cw.ChangeWatcherReporting;
import com.percero.agents.sync.cw.IChangeWatcherHelperFactory;
import com.percero.amqp.handlers.ChangeWatcherHandlerTask;
import com.percero.amqp.handlers.GetChangeWatcherHandler;
import com.rabbitmq.client.Channel;

/**
 * This class supplies the main method that creates the spring context
 * and then all processing is invoked asynchronously by messaging.
 * 
 * This class' onMessage function will be invoked when the process receives a 'test' message from the broker
 * @author Collin Brown
 *
 */
@Component("changeWatcherListener")
public class ChangeWatcherListener implements ChannelAwareMessageListener {

	@Autowired
	AmqpTemplate template;
	@Autowired
	AmqpAdmin amqpAdmin;
	@Autowired
	IDecoder decoder;
	@Autowired
	IAccessManager accessManager;
	
	@Autowired @Qualifier("executorWithCallerRunsPolicy")
	TaskExecutor taskExecutor;

	@Autowired
	IChangeWatcherHelperFactory changeWatcherHelperFactory;
	public void setChangeWatcherHelperFactory(IChangeWatcherHelperFactory value) {
		changeWatcherHelperFactory = value;
	}

	@Autowired
	GetChangeWatcherHandler getChangeWatcherHandler;

	
	public static final String PROCESS_TRANSACTION = "processTransaction";

	private static Logger logger = Logger.getLogger(ChangeWatcherListener.class);
	

	/**
	 * Message handling function
	 */
	@Transactional
	public void onMessage(Message message, Channel channel) {	
		Object ob;
		String changeWatcherId = null;
		try {
			taskExecutor = null;
			ob = new String(message.getBody());
			
			if (ob instanceof String) {
				changeWatcherId = (String) ob;
			}
		} catch (Exception e) {
			logger.error("Unable to process message", e);
		}

		if (changeWatcherId != null) {
			ChangeWatcherReporting.externalRequestsCounter++;
			if (taskExecutor != null) {
				taskExecutor.execute(new ChangeWatcherHandlerTask(changeWatcherId));
			} else {
				accessManager.recalculateChangeWatcher(changeWatcherId, null);
			}
		}
	}


	/**
	 * Main function for starting the process
	 */
	@SuppressWarnings("resource")
	public static void main(String[] args){
		ApplicationContext context =
				new ClassPathXmlApplicationContext(new String[] {"spring-listener.xml"});
		System.out.println(context.toString());
	}
}
