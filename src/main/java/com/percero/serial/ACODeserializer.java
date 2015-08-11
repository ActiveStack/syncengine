package com.percero.serial;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.JsonDeserializer;
import org.codehaus.jackson.map.TypeDeserializer;

import com.percero.agents.auth.vo.IAuthCachedObject;
import com.percero.agents.sync.metadata.MappedClass;
import com.percero.agents.sync.vo.ClassIDPair;


public class ACODeserializer extends JsonDeserializer<IAuthCachedObject>{
	private static Logger logger = Logger.getLogger("ACODeserializer");

	@SuppressWarnings("unchecked")
	@Override
	public IAuthCachedObject deserialize(JsonParser parser, DeserializationContext arg1) 
			throws IOException,JsonProcessingException {
		IAuthCachedObject result = null;
		
		try{
			ClassIDPair pair = parser.readValueAs(ClassIDPair.class);
			Class<? extends IAuthCachedObject> c = (Class<? extends IAuthCachedObject>) MappedClass.forName(pair.getClassName());
			result = c.newInstance();
			result.setID(pair.getID());
		} catch(Exception e){
			logger.error(e.getMessage(), e);
		}
		
		return result;
	}

	@SuppressWarnings("unchecked")
	@Override
    public Object deserializeWithType(JsonParser jp, DeserializationContext ctxt, TypeDeserializer typeDeserializer)
        throws IOException, JsonProcessingException
    {
		IAuthCachedObject result = null;
		
		try{
			//System.out.println(jp.getText());
			ClassIDPair pair = jp.readValueAs(ClassIDPair.class);
			Class<? extends IAuthCachedObject> c = (Class<? extends IAuthCachedObject>) MappedClass.forName(pair.getClassName());
			result = c.newInstance();
			result.setID(pair.getID());
		} catch(Exception e){
			logger.error(e.getMessage(), e);
		}
		
		return result;
    }
}
