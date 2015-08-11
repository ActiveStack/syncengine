package com.percero.framework.accessor;

import org.codehaus.jackson.map.ObjectMapper;

public interface IAccessor {

	public int toInt();
	
	public Boolean getCanRead();
	public void setCanRead(Boolean value);
	public Boolean getCanCreate();
	public void setCanCreate(Boolean value);
	public Boolean getCanDelete();
	public void setCanDelete(Boolean value);
	public Boolean getCanUpdate();
	public void setCanUpdate(Boolean value);
	
	public String retrieveJson(ObjectMapper objectMapper);
}
