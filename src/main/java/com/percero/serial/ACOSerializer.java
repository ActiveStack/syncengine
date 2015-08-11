package com.percero.serial;

import java.io.IOException;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.SerializerProvider;

import com.percero.agents.auth.vo.IAuthCachedObject;
import com.percero.agents.sync.vo.ClassIDPair;


public class ACOSerializer extends JsonSerializer<IAuthCachedObject>{

	@Override
	public void serialize(IAuthCachedObject ob, JsonGenerator gen, SerializerProvider arg2) 
			throws IOException,JsonProcessingException {
		gen.writeObject(new ClassIDPair(ob.getID(), ob.getClass().getName()));		
	}
}
