package com.percero.agents.sync.vo;

import java.io.IOException;
import java.util.List;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;



public class RunProcessResponse extends SyncResponse {

	private Object result;
	public Object getResult() {
		return result;
	}
	public void setResult(Object result) {
		this.result = result;
	}
	
	@SuppressWarnings("rawtypes")
	@Override
	public String retrieveBaseJson(ObjectMapper objectMapper) {
		StringBuilder objectJsonBuilder = new StringBuilder(super.retrieveBaseJson(objectMapper)).append(",\"result\":");
//		String objectJson = super.retrieveBaseJson(objectMapper) + ",\"result\":";
		
		if (getResult() != null) {
			if (objectMapper == null)
				objectMapper = new ObjectMapper();

			if (getResult() instanceof List) {
				List listResult = (List) getResult();
//				objectJson += "[";
				objectJsonBuilder.append("[");
				int counter = 0;
				for(Object nextObject : listResult) {
					if (counter > 0) {
						objectJsonBuilder.append(",");
//						objectJson += ",";
					}
					if (nextObject instanceof BaseDataObject) {
//						objectJson += ((BaseDataObject)nextObject).toJson(objectMapper);
						objectJsonBuilder.append( ((BaseDataObject)nextObject).toJson(objectMapper) );
					}
					else {
						try {
//							objectJson += objectMapper.writeValueAsString(nextObject);
							objectJsonBuilder.append( objectMapper.writeValueAsString(nextObject) );
						} catch (JsonGenerationException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
//							objectJson += "null";
							objectJsonBuilder.append("null");
						} catch (JsonMappingException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
//							objectJson += "null";
							objectJsonBuilder.append("null");
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
//							objectJson += "null";
							objectJsonBuilder.append("null");
						}
					}
					counter++;
				}
//				objectJson += "]";
				objectJsonBuilder.append("]");
 			}
			else {
				try {
					Object theResult = getResult();
					if (theResult instanceof BaseDataObject) {
//						objectJson += ((BaseDataObject)theResult).toJson(objectMapper);
						objectJsonBuilder.append( ((BaseDataObject)theResult).toJson(objectMapper) );
					}
					else {
//						objectJson += objectMapper.writeValueAsString(getResult());
						objectJsonBuilder.append( objectMapper.writeValueAsString(getResult()) );
					}
				} catch (JsonGenerationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
//					objectJson += "null";
					objectJsonBuilder.append("null");
				} catch (JsonMappingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
//					objectJson += "null";
					objectJsonBuilder.append("null");
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
//					objectJson += "null";
					objectJsonBuilder.append("null");
				}
			}
		}
		else {
//			objectJson += "null";
			objectJsonBuilder.append("null");
		}
		return objectJsonBuilder.toString();
	}
}
