package com.percero.serial.map;

import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.stereotype.Component;

/**
 * Subclassing ObjectMapper to make it keep from exploding when there is a missing property
 * (which is what it does by default unless you make the config setting like so).
 * @author jonnysamps
 *
 */
@Component
public class SafeObjectMapper extends ObjectMapper{

	public SafeObjectMapper(){
		this.disableDefaultTyping();
		//this.enableDefaultTyping();
		//this.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
		super.getDeserializationConfig().without(    
				DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES);
		/**
		super.getDeserializationConfig().set(    
				DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		*/
	}
}
