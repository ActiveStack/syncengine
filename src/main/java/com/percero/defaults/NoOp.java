package com.percero.defaults;

import org.apache.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.stereotype.Component;

/**
 * DO NOT DELLETE: 
 * This no operation class is used to initialize the context when running flyway objects ... 
 *
 */
@Component
public class NoOp {
	private static final Logger log = Logger.getLogger(NoOp.class.getName());

	NoOp() {

	}
	
	public static void main( String[] args )
	{
		ApplicationContext context = new ClassPathXmlApplicationContext(new String[] {"spring/percero-spring-config.xml","spring/*.xml"});
		log.debug(context.toString()+"(no op)");
		System.exit(0);
	}
}
