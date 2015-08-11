package com.percero.amqp;

import java.io.IOException;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.SerializerProvider;

import com.percero.agents.sync.vo.BaseDataObject;
import com.percero.agents.sync.vo.ClassIDPair;

public class OneToSerializer extends JsonSerializer<BaseDataObject> {

	@Override
	public void serialize(BaseDataObject ob, JsonGenerator gen, SerializerProvider provider) throws IOException, JsonProcessingException {
		// TODO Auto-generated method stub
		gen.writeObject(new ClassIDPair(ob.getID(), ob.getClass().getName()));
	}
}
