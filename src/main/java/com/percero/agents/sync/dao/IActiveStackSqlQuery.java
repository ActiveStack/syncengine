//package com.percero.agents.sync.dao;
//
//import java.util.Collection;
//import java.util.Date;
//import java.util.List;
//
//public interface IActiveStackSqlQuery {
//
//	public String getQueryString();
//	public String[] getNamedParameters();
//	public IActiveStackSqlQuery setString(int position, String val);
//	public IActiveStackSqlQuery setString(String name, String val);
//	public IActiveStackSqlQuery setDate(int position, Date val);
//	public IActiveStackSqlQuery setDate(String name, Date val);
//	public IActiveStackSqlQuery setParameter(int position, Object val);
//	public IActiveStackSqlQuery setParameter(String name, Object val);
//	public IActiveStackSqlQuery setParameterList(String name, Collection<?> vals);
//	public IActiveStackSqlQuery setParameterList(String name, Object[] vals);
//	public List<?> list();
//	public Object uniqueResult();
//	public int executeUpdate();
//
//}
