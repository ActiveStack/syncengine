package com.percero.framework.metadata;


public interface IMappedClass {

	public IMappedQuery getCreateQuery();
	public void setCreateQuery(IMappedQuery value);
	public IMappedQuery getUpdateQuery();
	public void setUpdateQuery(IMappedQuery value);
	public IMappedQuery getReadQuery();
	public void setReadQuery(IMappedQuery value);
	public IMappedQuery getDeleteQuery();
	public void setDeleteQuery(IMappedQuery value);
}
