package com.percero.agents.sync.datastore;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;

import com.percero.agents.sync.services.PerceroRedisTemplate;

@RunWith(MockitoJUnitRunner.class)
public class RedisClusterCacheDataStoreTest {
	
	@Mock
	private PerceroRedisTemplate template;
	
	@InjectMocks
	private RedisClusterCacheDataStore redisCacheDataStore;
	
	@Mock
	private SetOperations<String, Object> setOperations;

	@Mock
	private ListOperations<String, Object> listOperations;

	@Mock
	private HashOperations<String, Object, Object> hashOperations;

	@Mock
	private ValueOperations<String, Object> valueOperations;

	@Mock
	private ZSetOperations<String, Object> zsetOperations;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
//		redisCacheDataStore = new RedisCacheDataStore();
		
		Mockito.when(template.opsForSet()).thenReturn(setOperations);
		Mockito.when(template.opsForList()).thenReturn(listOperations);
		Mockito.when(template.opsForHash()).thenReturn(hashOperations);
		Mockito.when(template.opsForValue()).thenReturn(valueOperations);
		Mockito.when(template.opsForZSet()).thenReturn(zsetOperations);
	}

	@SuppressWarnings("unchecked")
	@After
	public void tearDown() throws Exception {
		// Confirm that no non-cluster safe operations have been performed. 
		
		// Sets
		Mockito.verify(setOperations, Mockito.never()).union(Mockito.anyString(), Mockito.anyCollection());
		Mockito.verify(setOperations, Mockito.never()).union(Mockito.anyString(), Mockito.anyString());
		Mockito.verify(setOperations, Mockito.never()).unionAndStore(Mockito.anyString(), Mockito.anyCollection(), Mockito.anyString());
		Mockito.verify(setOperations, Mockito.never()).unionAndStore(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
		Mockito.verify(setOperations, Mockito.never()).difference(Mockito.anyString(), Mockito.anyCollection());
		Mockito.verify(setOperations, Mockito.never()).difference(Mockito.anyString(), Mockito.anyString());
		Mockito.verify(setOperations, Mockito.never()).differenceAndStore(Mockito.anyString(), Mockito.anyCollection(), Mockito.anyString());
		Mockito.verify(setOperations, Mockito.never()).differenceAndStore(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
		Mockito.verify(setOperations, Mockito.never()).intersect(Mockito.anyString(), Mockito.anyCollection());
		Mockito.verify(setOperations, Mockito.never()).intersect(Mockito.anyString(), Mockito.anyString());
		Mockito.verify(setOperations, Mockito.never()).intersectAndStore(Mockito.anyString(), Mockito.anyCollection(), Mockito.anyString());
		Mockito.verify(setOperations, Mockito.never()).intersectAndStore(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
		Mockito.verify(setOperations, Mockito.never()).move(Mockito.anyString(), Mockito.anyObject(), Mockito.anyString());
		Mockito.verify(setOperations, Mockito.never()).scan(Mockito.anyString(), Mockito.any(ScanOptions.class));

		// ZSets
		Mockito.verify(zsetOperations, Mockito.never()).unionAndStore(Mockito.anyString(), Mockito.anyCollection(), Mockito.anyString());
		Mockito.verify(zsetOperations, Mockito.never()).unionAndStore(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
		Mockito.verify(zsetOperations, Mockito.never()).intersectAndStore(Mockito.anyString(), Mockito.anyCollection(), Mockito.anyString());
		Mockito.verify(zsetOperations, Mockito.never()).intersectAndStore(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
		Mockito.verify(zsetOperations, Mockito.never()).scan(Mockito.anyString(), Mockito.any(ScanOptions.class));
		
		// Hashes
		Mockito.verify(hashOperations, Mockito.never()).scan(Mockito.anyString(), Mockito.any(ScanOptions.class));

		// Value
		Mockito.verify(valueOperations, Mockito.never()).multiGet(Mockito.anyCollection());
		
	}

	@Test
	public void testExpire() {
		String key = "KEY";
		long timeout = 1000;
		TimeUnit timeUnit = TimeUnit.MILLISECONDS;
		
		redisCacheDataStore.expire(key, timeout, timeUnit);
		Mockito.verify(template, Mockito.never()).expire(Mockito.anyString(), Mockito.anyLong(), Mockito.any(TimeUnit.class));
		
		redisCacheDataStore.expire(key, timeout, timeUnit, false);
		Mockito.verify(template, Mockito.never()).expire(Mockito.anyString(), Mockito.anyLong(), Mockito.any(TimeUnit.class));
		
		Collection<String> keys = new ArrayList<String>();
		keys.add("KEY_1");
		keys.add("KEY_2");
		
		redisCacheDataStore.expire(keys, timeout, timeUnit);
		Mockito.verify(template, Mockito.never()).expire(Mockito.anyString(), Mockito.anyLong(), Mockito.any(TimeUnit.class));
		
		redisCacheDataStore.expire(keys, timeout, timeUnit, false);
		Mockito.verify(template, Mockito.never()).expire(Mockito.anyString(), Mockito.anyLong(), Mockito.any(TimeUnit.class));

		
		redisCacheDataStore.expire(key, timeout, timeUnit, true);
		Mockito.verify(template, Mockito.times(1)).expire(Mockito.anyString(), Mockito.anyLong(), Mockito.any(TimeUnit.class));

		// Test exit from expire loop.
		Mockito.when(template.expire(Mockito.anyString(), Mockito.anyLong(), Mockito.any(TimeUnit.class))).thenReturn(false);
		redisCacheDataStore.expire(keys, timeout, timeUnit, true);
		Mockito.verify(template, Mockito.times(2)).expire(Mockito.anyString(), Mockito.anyLong(), Mockito.any(TimeUnit.class));

		// Test complete expire loop
		Mockito.when(template.expire(Mockito.anyString(), Mockito.anyLong(), Mockito.any(TimeUnit.class))).thenReturn(true);
		redisCacheDataStore.expire(keys, timeout, timeUnit, true);
		Mockito.verify(template, Mockito.times(4)).expire(Mockito.anyString(), Mockito.anyLong(), Mockito.any(TimeUnit.class));
	}

}
