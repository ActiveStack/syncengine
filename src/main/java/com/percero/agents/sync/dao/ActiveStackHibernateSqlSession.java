//package com.percero.agents.sync.dao;
//
//import org.hibernate.Session;
//
//public class ActiveStackHibernateSqlSession implements IActiveStackSqlSession {
//
//	private Session session;
//	
//	public ActiveStackHibernateSqlSession(Session session) {
//		this.session = session;
//	}
//	
//	public IActiveStackSqlQuery createSQLQuery(String queryString) {
//		ActiveStackHibernateSqlQuery query = new ActiveStackHibernateSqlQuery(getSession().createSQLQuery(queryString));
//		return query;
//	}
//
//	public void close() {
//		if (getSession() != null) {
//			getSession().close();
//		}
//	}
//
//	public Session getSession() {
//		return session;
//	}
//
//	public void setSession(Session session) {
//		this.session = session;
//	}
//}
