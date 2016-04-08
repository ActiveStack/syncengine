package com.percero.agents.sync.services;

import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.HashSet;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.percero.agents.sync.access.IAccessManager;
import com.percero.agents.sync.vo.ClassIDPair;
import com.percero.example.Person;
import com.percero.framework.vo.IPerceroObject;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:spring/percero-spring-config.xml" })
public class TestSyncAgentService {
	
//	@Autowired
//	SyncAgentService syncAgentService;

	SyncAgentService mockService = null;
	IDataProvider dataProvider = null;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
		mockService = Mockito.mock(SyncAgentService.class);
		
		mockService.accessManager = Mockito.mock(IAccessManager.class);
		Mockito.when(mockService.accessManager.getClientUserId(Mockito.anyString())).thenReturn("USER_ID");

		mockService.dataProviderManager = Mockito.mock(IDataProviderManager.class);
		dataProvider = Mockito.mock(IDataProvider.class);
		Mockito.when(mockService.dataProviderManager.getDataProviderByName(Mockito.anyString())).thenReturn(dataProvider);
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testSystemDeleteObjectClassIDPairStringBoolean() throws Exception {
		Mockito.when(mockService.systemDeleteObject(Mockito.any(ClassIDPair.class), Mockito.anyString(), Mockito.anyBoolean())).thenCallRealMethod();
		mockService.systemDeleteObject(new ClassIDPair("1", Person.class.getCanonicalName()), "clientId", true);
		Mockito.verify(mockService, Mockito.times(1)).systemDeleteObject(Mockito.any(ClassIDPair.class), Mockito.anyString(), Mockito.anyBoolean(), Mockito.anyCollection());
	}

	@Test
	public void testSystemDeleteObjectClassIDPairStringBooleanCollectionOfClassIDPair() throws Exception {
		Mockito.when(mockService.systemDeleteObject(Mockito.any(ClassIDPair.class), Mockito.anyString(), Mockito.anyBoolean(), Mockito.anyCollection())).thenCallRealMethod();
		boolean result = mockService.systemDeleteObject(null, "clientId", true, null);

		Collection<ClassIDPair> deletedObjects = new HashSet<ClassIDPair>(1);
		deletedObjects.add(new ClassIDPair("1", Person.class.getCanonicalName()));
		result = mockService.systemDeleteObject(new ClassIDPair("1", Person.class.getCanonicalName()), "clientId", true, deletedObjects);
		assertTrue(result);
		
		// If object does NOT exist, then it should just be ignored.
		deletedObjects.clear();
		Mockito.when(dataProvider.findById(Mockito.any(ClassIDPair.class), Mockito.anyString())).thenReturn(null);
		result = mockService.systemDeleteObject(new ClassIDPair("1", Person.class.getCanonicalName()), "clientId", true, deletedObjects);
		assertTrue(result);

		Person person = new Person();
		Mockito.when(dataProvider.findById(Mockito.any(ClassIDPair.class), Mockito.anyString())).thenReturn(person);

		result = mockService.systemDeleteObject(new ClassIDPair("1", Person.class.getCanonicalName()), "clientId", true, deletedObjects);
		assertTrue(result);
		assertTrue(deletedObjects.contains(new ClassIDPair("1", Person.class.getCanonicalName())));
	}

}
