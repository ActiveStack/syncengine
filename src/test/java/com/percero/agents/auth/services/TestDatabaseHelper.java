package com.percero.agents.auth.services;

import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.classic.Session;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import com.percero.agents.auth.vo.BasicAuthCredential;
import com.percero.agents.auth.vo.ServiceUser;
import com.percero.agents.auth.vo.UserIdentifier;

public class TestDatabaseHelper {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}
	
	DatabaseHelper databaseHelper = null;
	Query query = null;
	SQLQuery sqlQuery = null;
	Session session = null;
	Transaction tx = null;

	@Before
	public void setUp() throws Exception {
		databaseHelper = Mockito.mock(DatabaseHelper.class);
		databaseHelper.sessionFactoryAuth = Mockito.mock(SessionFactory.class);
		
		session = Mockito.mock(Session.class);
		query = Mockito.mock(Query.class);
		sqlQuery = Mockito.mock(SQLQuery.class);
		tx = Mockito.mock(Transaction.class);
		
		Mockito.when(databaseHelper.sessionFactoryAuth.openSession()).thenReturn(session);
		Mockito.when(session.createQuery(Mockito.anyString())).thenReturn(query);
		Mockito.when(session.createSQLQuery(Mockito.anyString())).thenReturn(sqlQuery);
		Mockito.when(session.beginTransaction()).thenReturn(tx);
		
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testRegisterUserExceptionsAndSuccess() {
		
		try {
			Mockito.when(databaseHelper.registerUser(Mockito.any(BasicAuthCredential.class), Mockito.anyString())).thenCallRealMethod();
		} catch (AuthException e1) {
			// Do nothing here.
		}
		
		try {
			Mockito.when(databaseHelper.registerUser(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenCallRealMethod();
		} catch (AuthException e1) {
			// Do nothing here.
		}
		
		Mockito.when(databaseHelper.getServiceUser(Mockito.anyString(), Mockito.anyString())).thenReturn(new ServiceUser());
		
		ServiceUser serviceUser = null;
		String paradigm = "TEST";

		// Test missing data.
		BasicAuthCredential credential = new BasicAuthCredential();
		try {
			serviceUser = databaseHelper.registerUser(credential, paradigm);
			// If we get here, then we didn't catch the missing data.
			fail("Did not catch missing data");
		} catch (AuthException e) {
			// Make sure we have the appropriate failure reported.
			if (!AuthException.INVALID_DATA.equalsIgnoreCase(e.getDetail())) {
				fail("Incorrect failure.  Should be " + AuthException.INVALID_DATA + ", but was " + e.getDetail() + " instead");
			}
		}
		
		// Test duplicate user name.
		List dupUserNamesList = new ArrayList();
		dupUserNamesList.add(new Object());
		Mockito.when(query.list()).thenReturn(dupUserNamesList);
		
		credential.setUsername("USER");
		credential.setPassword("PASSWORD");
		try {
			serviceUser = databaseHelper.registerUser(credential, paradigm);
			// If we get here, then we didn't catch the missing data.
			fail("Did not catch missing data");
		} catch (AuthException e) {
			// Make sure we have the appropriate failure reported.
			if (!AuthException.DUPLICATE_USER_NAME.equalsIgnoreCase(e.getDetail())) {
				fail("Incorrect failure.  Should be " + AuthException.DUPLICATE_USER_NAME + ", but was " + e.getDetail() + " instead");
			}
		}
		
		// Test invalid User Identifier.
		dupUserNamesList.clear();
		Mockito.when(query.list()).thenReturn(dupUserNamesList);
		Mockito.when(query.uniqueResult()).thenReturn(null);
		
		credential.setUsername("USER");
		credential.setPassword("PASSWORD");
		try {
			serviceUser = databaseHelper.registerUser(credential, paradigm);
			// If we get here, then we didn't catch the missing data.
			fail("Did not catch missing data");
		} catch (AuthException e) {
			// Make sure we have the appropriate failure reported.
			if (!AuthException.INVALID_USER_IDENTIFIER.equalsIgnoreCase(e.getDetail())) {
				fail("Incorrect failure.  Should be " + AuthException.INVALID_USER_IDENTIFIER + ", but was " + e.getDetail() + " instead");
			}
		}
		
		// Test invalid User Password.
		UserIdentifier userIdentifier = new UserIdentifier();
		dupUserNamesList.clear();
		Mockito.when(query.list()).thenReturn(dupUserNamesList);
		Mockito.when(query.uniqueResult()).thenReturn(userIdentifier);
		Mockito.when(sqlQuery.executeUpdate()).thenReturn(0);
		
		credential.setUsername("USER");
		credential.setPassword("PASSWORD");
		try {
			serviceUser = databaseHelper.registerUser(credential, paradigm);
			// If we get here, then we didn't catch the missing data.
			fail("Did not catch missing data");
		} catch (AuthException e) {
			// Make sure we have the appropriate failure reported.
			if (!AuthException.INVALID_USER_PASSWORD.equalsIgnoreCase(e.getDetail())) {
				fail("Incorrect failure.  Should be " + AuthException.INVALID_USER_PASSWORD + ", but was " + e.getDetail() + " instead");
			}
		}
		
		// Test Valid User.
		dupUserNamesList.clear();
		Mockito.when(query.list()).thenReturn(dupUserNamesList);
		Mockito.when(sqlQuery.executeUpdate()).thenReturn(1);
		
		credential.setUsername("USER");
		credential.setPassword("PASSWORD");
		try {
			serviceUser = databaseHelper.registerUser(credential, paradigm);
			
			if (serviceUser == null) {
				// If we get here, then we didn't catch the missing data.
				fail("Did not create ServiceUser");
			}
		} catch (AuthException e) {
			fail("Failed to create User: " + e.getDetail());
		}
	}

}
