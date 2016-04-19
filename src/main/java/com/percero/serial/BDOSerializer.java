package com.percero.serial;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.SerializerProvider;

import com.percero.agents.sync.metadata.MappedClass;
import com.percero.agents.sync.metadata.MappedClassManagerFactory;
import com.percero.agents.sync.metadata.MappedField;
import com.percero.agents.sync.metadata.MappedFieldList;
import com.percero.agents.sync.metadata.MappedFieldPerceroObject;
import com.percero.agents.sync.vo.BaseDataObject;
import com.percero.agents.sync.vo.ClassIDPair;
import com.percero.framework.vo.IPerceroObject;


public class BDOSerializer extends JsonSerializer<BaseDataObject>{

	@SuppressWarnings("unchecked")
	@Override
	public void serialize(BaseDataObject ob, JsonGenerator gen, SerializerProvider arg2) 
			throws IOException,JsonProcessingException {
		ClassIDPair pair = new ClassIDPair(ob.getID(), ob.getClass().getName());
		MappedClass mp = MappedClassManagerFactory.getMappedClassManager().getMappedClassByClassName(ob.getClass().getName());
		if (mp != null && mp.nonLazyLoadingFields.size() > 0)
		{
			for(MappedField nextMappedField : mp.nonLazyLoadingFields) {
				if (nextMappedField instanceof MappedFieldList) {
					try {
						List<ClassIDPair> listToAdd = new ArrayList<ClassIDPair>();
						List<IPerceroObject> nextFieldList = (List<IPerceroObject>) nextMappedField.getGetter().invoke(ob);
						for(IPerceroObject nextListObject : nextFieldList) {
							listToAdd.add(new ClassIDPair(nextListObject.getID(), nextListObject.getClass().getName()));
						}
						pair.addProperty(nextMappedField.getField().getName(), listToAdd);
					} catch(Exception e) {
						// Do nothing, for now.
					}
				} else if (nextMappedField instanceof MappedFieldPerceroObject) {
					try {
						IPerceroObject nextFieldPerceroObject = (IPerceroObject) nextMappedField.getGetter().invoke(ob);
						ClassIDPair nextFieldPair = new ClassIDPair(nextFieldPerceroObject.getID(), nextFieldPerceroObject.getClass().getName());
						pair.addProperty(nextMappedField.getField().getName(), nextFieldPair);
					} catch(Exception e) {
						// Do nothing, for now.
					}
				} else {
					try {
						Object nextFieldValue = nextMappedField.getGetter().invoke(ob);
						pair.addProperty(nextMappedField.getField().getName(), nextFieldValue);
					} catch(Exception e) {
						// Do nothing, for now.
					}
				}
			}
		}
		gen.writeObject(pair);		
	}
}
