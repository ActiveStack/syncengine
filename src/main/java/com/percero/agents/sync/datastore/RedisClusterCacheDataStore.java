package com.percero.agents.sync.datastore;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.springframework.data.redis.core.SetOperations;
import org.springframework.stereotype.Component;

@Component
public class RedisClusterCacheDataStore extends RedisCacheDataStore /*implements IRedisClusterCacheDataStore*/ {

	/* (non-Javadoc)
	 * @see com.percero.agents.sync.datastore.IRedisDataStore#setUnionAndStore(java.lang.String, java.lang.String, java.lang.String)
	 */
	/* (non-Javadoc)
	 * @see com.percero.agents.sync.datastore.IRedisClusterCacheDataStore#setUnionAndStore(java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public Long setUnionAndStore(String key, String otherKey, String destKey) {
		SetOperations<String, Object> setOps = template.opsForSet();
		Set<Object> existingSet = setOps.members(key);
		existingSet.addAll(setOps.members(otherKey));

		Iterator<Object> itr = existingSet.iterator();
		while(itr.hasNext()) {
			setOps.add(destKey, itr.next());
		}
		
		return setOps.size(destKey);
	}
	
	/* (non-Javadoc)
	 * @see com.percero.agents.sync.datastore.IRedisDataStore#setUnionAndStore(java.lang.String, java.util.Collection, java.lang.String)
	 */
	@Override
	public Long setUnionAndStore(String key, Collection<String> otherKeys, String destKey) {
		SetOperations<String, Object> setOps = template.opsForSet();
		Set<Object> existingSet = setOps.members(key);
		
		Iterator<String> itrKeys = otherKeys.iterator();
		while(itrKeys.hasNext()) {
			String nextKey = itrKeys.next();
			
			existingSet.addAll(setOps.members(nextKey));
			
			Iterator<Object> itr = existingSet.iterator();
			while(itr.hasNext()) {
				setOps.add(destKey, itr.next());
			}
		}
		
		return setOps.size(destKey);
	}
	
	
	/* (non-Javadoc)
	 * @see com.percero.agents.sync.datastore.IRedisDataStore#getSetIntersect(java.lang.String, java.util.Collection)
	 */
	@Override
	public Set<Object> getSetIntersect( final String key, Collection<String> otherKeys ) {
		SetOperations<String, Object> setOps = template.opsForSet();
		
		Set<Object> result = setOps.members(key);
		
		Iterator<String> itrKeys = otherKeys.iterator();
		
		while(itrKeys.hasNext()) {
			String nextKey = itrKeys.next();
			Set<Object> otherSet = setOps.members(nextKey);
			
			Iterator<Object> itr = result.iterator();
			
			while(itr.hasNext()) {
				Object nextObject = itr.next();
				if (!otherSet.contains(nextObject)) {
					itr.remove();
				}
			}
		}
		
		return result;
	}
	
	/* (non-Javadoc)
	 * @see com.percero.agents.sync.datastore.IRedisDataStore#getSetIntersect(java.lang.String, java.lang.String)
	 */
	@Override
	public Set<Object> getSetIntersect( final String key, String otherKey ) {
		SetOperations<String, Object> setOps = template.opsForSet();
		
		Set<Object> theSet = setOps.members(key);
		Set<Object> otherSet = setOps.members(otherKey);
		Set<Object> result = new HashSet<Object>();
		
		Iterator<Object> itr = theSet.iterator();
		
		while(itr.hasNext()) {
			Object nextObject = itr.next();
			if (otherSet.contains(nextObject)) {
				result.add(nextObject);
			}
		}
		
		return result;
	}
	
	/* (non-Javadoc)
	 * @see com.percero.agents.sync.datastore.IRedisDataStore#getSetUnion(java.lang.String, java.util.Collection)
	 */
	@Override
	public Set<Object> getSetUnion( final String key, final Collection<String> otherKeys ) {
		SetOperations<String, Object> setOps = template.opsForSet();
		
		Set<Object> theSet = setOps.members(key);
		Iterator<String> itr = otherKeys.iterator();
		while(itr.hasNext()) {
			theSet.addAll(setOps.members(itr.next()));
		}
		return theSet;
	}
	
	/* (non-Javadoc)
	 * @see com.percero.agents.sync.datastore.IRedisDataStore#getSetUnion(java.lang.String, java.lang.String)
	 */
	@Override
	public Set<Object> getSetUnion( final String key, final String otherKey ) {
		SetOperations<String, Object> setOps = template.opsForSet();
		
		Set<Object> theSet = setOps.members(key);
		theSet.addAll(setOps.members(otherKey));
		return theSet;
	}
	
}
