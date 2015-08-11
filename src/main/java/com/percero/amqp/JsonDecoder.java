package com.percero.amqp;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.percero.agents.sync.metadata.MappedClass;

@Component
public class JsonDecoder implements IDecoder {
	
	@Autowired
	ObjectMapper om;
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Object decode(byte[] data) throws Exception{
		JsonNode node = om.readTree(data);
		Class c = MappedClass.forName(node.get("cn").getTextValue());
		Object o = om.readValue(node, c);
		return o;
	}
}
