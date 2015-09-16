//package com.percero.agents.sync.dao;
//
//import java.util.Collection;
//import java.util.Date;
//import java.util.List;
//
//import org.hibernate.SQLQuery;
//
//public class ActiveStackHibernateSqlQuery implements IActiveStackSqlQuery {
//
//	private SQLQuery query;
//	
//	public ActiveStackHibernateSqlQuery(SQLQuery query) {
//		this.query = query;
//	}
//	
//	public String getQueryString() {
//		return getQuery().getQueryString();
//	}
//
//	@Override
//	public String[] getNamedParameters() {
//		return getQuery().getNamedParameters();
//	}
//	
//	public IActiveStackSqlQuery setString(int position, String val) {
//		getQuery().setString(position, val);
//		return this;
//	}
//
//	public IActiveStackSqlQuery setString(String name, String val) {
//		getQuery().setString(name, val);
//		return this;
//	}
//	
//	public IActiveStackSqlQuery setDate(int position, Date val) {
//		getQuery().setDate(position, val);
//		return this;
//	}
//	
//	public IActiveStackSqlQuery setDate(String name, Date val) {
//		getQuery().setDate(name, val);
//		return this;
//	}
//	
//	public IActiveStackSqlQuery setParameter(int position, Object val) {
//		getQuery().setParameter(position, val);
//		return this;
//	}
//	
//	public IActiveStackSqlQuery setParameter(String name, Object val) {
//		getQuery().setParameter(name, val);
//		return this;
//	}
//	
//	public IActiveStackSqlQuery setParameterList(String name, Collection<?> vals) {
//		getQuery().setParameterList(name, vals);
//		return this;
//	}
//	
//	public IActiveStackSqlQuery setParameterList(String name, Object[] vals) {
//		getQuery().setParameterList(name, vals);
//		return this;
//	}
//	
//	public List<?> list() {
//		return getQuery().list();
//	}
//	
//	public Object uniqueResult() {
//		return getQuery().uniqueResult();
//	}
//	
//	public int executeUpdate() {
//		return getQuery().executeUpdate();
//	}
//	
//	
//	public SQLQuery getQuery() {
//		return query;
//	}
//
//	public void setQuery(SQLQuery query) {
//		this.query = query;
//	}
//
//}
