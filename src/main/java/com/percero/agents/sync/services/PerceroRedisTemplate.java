package com.percero.agents.sync.services;

import java.util.Set;

import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

public class PerceroRedisTemplate extends RedisTemplate<String, Object> {

	public PerceroRedisTemplate() {
		RedisSerializer<String> stringSerializer = new StringRedisSerializer();
		setKeySerializer(stringSerializer);
		//setValueSerializer(stringSerializer);
		setHashKeySerializer(stringSerializer);
		//setHashValueSerializer(stringSerializer);
	}

	/**
	 * Constructs a new <code>StringRedisTemplate</code> instance ready to be used. 
	 *
	 * @param connectionFactory connection factory for creating new connections
	 */
	public PerceroRedisTemplate(RedisConnectionFactory connectionFactory) {
		this();
		setConnectionFactory(connectionFactory);
		afterPropertiesSet();
	}
	
	public Set<String> scanForKeys(final String pattern) {
		return super.keys(pattern);
/*		Set<String> result = new HashSet<String>();
		
		final ScanOptions scanOptions = (new ScanOptions.ScanOptionsBuilder()).match(pattern).count(1000).build();
		Cursor<byte[]> scanResult = execute(new RedisCallback<Cursor<byte[]>>() {

			public Cursor<byte[]> doInRedis(RedisConnection connection) {
				return connection.scan(scanOptions);
			}
		}, true);
		
		while (scanResult.hasNext()) {
			try {
				byte[] nextResult = scanResult.next();
				Object nextObjectResult = getKeySerializer().deserialize(nextResult);
				result.add((String)nextObjectResult);
			} catch(NoSuchElementException nsee) {
				// If we get here it means that there were no records in the current iteration of the scan
			} catch(ClassCastException cce) {
				logger.error(cce);
//				try {
//					scanResult.next();
//				} catch(Exception e) {}
			}
		}
		return result;*/
	}
	
	
/*	protected RedisConnection preProcessConnection(RedisConnection connection, boolean existingConnection) {
		return new DefaultStringRedisConnection(connection);
	}*/
}
