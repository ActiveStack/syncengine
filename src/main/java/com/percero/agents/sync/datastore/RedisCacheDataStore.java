package com.percero.agents.sync.datastore;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

import com.percero.agents.sync.services.PerceroRedisTemplate;

import edu.emory.mathcs.backport.java.util.Collections;

public class RedisCacheDataStore implements ICacheDataStore {

	private static Logger log = Logger.getLogger(RedisCacheDataStore.class);
	
	@Autowired
	protected PerceroRedisTemplate template;
	
	/* (non-Javadoc)
	 * @see com.percero.agents.sync.datastore.IRedisCacheDataStore#expire(java.lang.String, long, java.util.concurrent.TimeUnit)
	 */
	@Override
	public Boolean expire( final String key, final long timeout, final TimeUnit timeUnit ) {
		return expire(key, timeout, timeUnit, false);
	}
	 
	/* (non-Javadoc)
	 * @see com.percero.agents.sync.datastore.IRedisCacheDataStore#expire(java.util.Collection, long, java.util.concurrent.TimeUnit)
	 */
	@Override
	public Boolean expire( final Collection<String> keys, final long timeout, final TimeUnit timeUnit ) {
		return expire(keys, timeout, timeUnit, false);
	}
	
	/* (non-Javadoc)
	 * @see com.percero.agents.sync.datastore.IRedisCacheDataStore#expire(java.lang.String, long, java.util.concurrent.TimeUnit, java.lang.Boolean)
	 */
	@Override
	public Boolean expire( final String key, final long timeout, final TimeUnit timeUnit, Boolean forceNow ) {
		if (forceNow) {
			return template.expire(key, timeout, timeUnit);
		}
		else {
			expiresToBeWritten.put(key, new PendingExpire(key, timeout, timeUnit));
			return true;
		}
	}
	
	/* (non-Javadoc)
	 * @see com.percero.agents.sync.datastore.IRedisCacheDataStore#expire(java.util.Collection, long, java.util.concurrent.TimeUnit, java.lang.Boolean)
	 */
	@Override
	public Boolean expire( final Collection<String> keys, final long timeout, final TimeUnit timeUnit, Boolean forceNow ) {
		if (forceNow) {
			Iterator<String> itrKeys = keys.iterator();
			while (itrKeys.hasNext()) {
				if (!template.expire(itrKeys.next(), timeout, timeUnit)) {
					return false;
				}
			}
			return true;
		}
		else {
			Iterator<String> itrKeys = keys.iterator();
			while (itrKeys.hasNext()) {
				String nextKey = itrKeys.next();
				expiresToBeWritten.put(nextKey, new PendingExpire(nextKey, timeout, timeUnit));
			}
			return true;
		}
	}
	
	
	@SuppressWarnings("unchecked")
	private Map<String, PendingExpire> expiresToBeWritten = Collections.synchronizedMap(new HashMap<String, PendingExpire>());
	/* (non-Javadoc)
	 * @see com.percero.agents.sync.datastore.IRedisCacheDataStore#postExpires()
	 */
	@Override
	@Scheduled(fixedRate=600000)	// 10 * 60 * 1000 -> Ten Minutes.
	public void postExpires() {
		log.info("Posting " +  expiresToBeWritten.size() + " expire" + (expiresToBeWritten.size() == 1 ? "" : "s"));
		synchronized (expiresToBeWritten) {
			Collection<String> expireKeysToRemove = new HashSet<String>(expiresToBeWritten.size(), (float)1.0);
			Iterator<Map.Entry<String, PendingExpire>> itrExpiresEntrySet = expiresToBeWritten.entrySet().iterator();
			while (itrExpiresEntrySet.hasNext()) {
				Map.Entry<String, PendingExpire> nextEntry = itrExpiresEntrySet.next();
				String nextKey = nextEntry.getKey();
				PendingExpire nextPendingExpire = nextEntry.getValue();
				expire(nextPendingExpire.key, nextPendingExpire.timeout, nextPendingExpire.timeUnit, true);
				expireKeysToRemove.add(nextKey);
			}
//			Iterator<String> itrExpires = expiresToBeWritten.keySet().iterator();
//			while (itrExpires.hasNext()) {
//				String nextKey = itrExpires.next();
//				PendingExpire nextPendingExpire = expiresToBeWritten.get(nextKey);
//				expire(nextPendingExpire.key, nextPendingExpire.timeout, nextPendingExpire.timeUnit, true);
//				expireKeysToRemove.add(nextKey);
//			}
			
			Iterator<String> itrExpireKeysToRemove = expireKeysToRemove.iterator();
			while (itrExpireKeysToRemove.hasNext()) {
				expiresToBeWritten.remove(itrExpireKeysToRemove.next());
			}
		}
	}
	
	private class PendingExpire {
		String key;
		long timeout;
		TimeUnit timeUnit;
		
		public PendingExpire(String key, long timeout, TimeUnit timeUnit) {
			this.key = key;
			this.timeout = timeout;
			this.timeUnit = timeUnit;
		}
	}

	
	/* (non-Javadoc)
	 * @see com.percero.agents.sync.datastore.IRedisCacheDataStore#getValue(java.lang.String)
	 */
	@Override
	public Object getValue( final String key ) {
		return template.opsForValue().get( key );
	}
	 
	/* (non-Javadoc)
	 * @see com.percero.agents.sync.datastore.IRedisCacheDataStore#getValues(java.util.Collection)
	 */
	@Override
	public List<Object> getValues( final Collection<String> keys ) {
		return template.opsForValue().multiGet(keys);
	}
	
	/* (non-Javadoc)
	 * @see com.percero.agents.sync.datastore.IRedisCacheDataStore#setValue(java.lang.String, java.lang.Object)
	 */
	@Override
	public void setValue( final String key, final Object value ) {
	    template.opsForValue().set( key, value );
	}
	
//	public void setValues( final KeyValuePair[] pairs ) {
	/* (non-Javadoc)
	 * @see com.percero.agents.sync.datastore.IRedisCacheDataStore#setValues(java.util.Map)
	 */
	@Override
	public void setValues( final Map<String, ? extends Object> keysAndValuesMap ) {
		template.opsForValue().multiSet(keysAndValuesMap);
//		if (pairs != null) {
//			for (KeyValuePair nextPair : pairs) {
//				template.opsForValue().set( nextPair.key, nextPair.value );
//			}
//		}
	}
	
	/* (non-Javadoc)
	 * @see com.percero.agents.sync.datastore.IRedisCacheDataStore#addSetValue(java.lang.String, java.lang.Object)
	 */
	@Override
	@Transactional
	public Long addSetValue( final String key, final Object value ) {
		return template.opsForSet().add(key, value);// ? Long.valueOf(1) : Long.valueOf(0);
	}
	
	/* (non-Javadoc)
	 * @see com.percero.agents.sync.datastore.IRedisCacheDataStore#lpushListValue(java.lang.String, java.lang.Object)
	 */
	@Override
	@Transactional
	public Long lpushListValue( final String key, final Object value ) {
		return template.opsForList().leftPush(key, value);
	}
	
	/* (non-Javadoc)
	 * @see com.percero.agents.sync.datastore.IRedisCacheDataStore#listAll(java.lang.String)
	 */
	@Override
	@Transactional
	public List<Object> listAll( final String key) {
		return listRange(key, Long.valueOf(0), Long.valueOf(-1));
	}
	
	/* (non-Javadoc)
	 * @see com.percero.agents.sync.datastore.IRedisCacheDataStore#listRange(java.lang.String, java.lang.Long, java.lang.Long)
	 */
	@Override
	@Transactional
	public List<Object> listRange( final String key, final Long start, final Long end ) {
		return template.opsForList().range(key, start, end);
	}
	
	/* (non-Javadoc)
	 * @see com.percero.agents.sync.datastore.IRedisCacheDataStore#listIndex(java.lang.String, java.lang.Long)
	 */
	@Override
	@Transactional
	public Object listIndex( final String key, final Long index ) {
		return template.opsForList().index(key, index);
	}
	
	/* (non-Javadoc)
	 * @see com.percero.agents.sync.datastore.IRedisCacheDataStore#listIndex(java.lang.String)
	 */
	@Override
	@Transactional
	public Object listIndex( final String key ) {
		return template.opsForList().index(key, Long.valueOf(0));
	}
	
	/* (non-Javadoc)
	 * @see com.percero.agents.sync.datastore.IRedisCacheDataStore#getHashValue(java.lang.String, java.lang.String)
	 */
	@Override
	@Transactional
	public Object getHashValue( final String key, final String hashKey ) {
		return template.opsForHash().get(key, hashKey);
	}
	
	/* (non-Javadoc)
	 * @see com.percero.agents.sync.datastore.IRedisCacheDataStore#getHashKeys(java.lang.String)
	 */
	@Override
	@Transactional
	public Set<Object> getHashKeys( final String key ) {
		return template.opsForHash().keys(key);
	}
	
	/* (non-Javadoc)
	 * @see com.percero.agents.sync.datastore.IRedisCacheDataStore#getHashEntries(java.lang.String)
	 */
	@Override
	@Transactional
	public Map<Object, Object> getHashEntries( final String key ) {
		return template.opsForHash().entries(key);
	}
	
	/* (non-Javadoc)
	 * @see com.percero.agents.sync.datastore.IRedisCacheDataStore#getHashSize(java.lang.String)
	 */
	@Override
	@Transactional
	public Long getHashSize( final String key ) {
		return template.opsForHash().size(key);
	}
	
	/* (non-Javadoc)
	 * @see com.percero.agents.sync.datastore.IRedisCacheDataStore#deleteHashKey(java.lang.String, java.lang.Object)
	 */
	@Override
	@Transactional
	public void deleteHashKey( final String key, final Object hashKey ) {
		template.opsForHash().delete(key, hashKey);
	}
	
	/* (non-Javadoc)
	 * @see com.percero.agents.sync.datastore.IRedisCacheDataStore#setHashValue(java.lang.String, java.lang.String, java.lang.Object)
	 */
	@Override
	@Transactional
	public void setHashValue( final String key, final String hashKey, final Object value ) {
		template.opsForHash().put(key, hashKey, value);
	}
	
	/* (non-Javadoc)
	 * @see com.percero.agents.sync.datastore.IRedisCacheDataStore#setAllHashValues(java.lang.String, java.util.Map)
	 */
	@Override
	@Transactional
	public void setAllHashValues( final String key, final Map<? extends Object, ? extends Object> hashMap ) {
		template.opsForHash().putAll(key, hashMap);
	}
	
	/* (non-Javadoc)
	 * @see com.percero.agents.sync.datastore.IRedisCacheDataStore#removeSetValue(java.lang.String, java.lang.Object)
	 */
	@Override
	@Transactional
	public Long removeSetValue( final String key, final Object value ) {
		return template.opsForSet().remove(key, value);// ? Long.valueOf(1) : Long.valueOf(0);
	}
	
	/* (non-Javadoc)
	 * @see com.percero.agents.sync.datastore.IRedisCacheDataStore#getSetIsEmpty(java.lang.String)
	 */
	@Override
	@Transactional
	public Boolean getSetIsEmpty( final String key ) {
		Long result = template.opsForSet().size(key);// ? Long.valueOf(1) : Long.valueOf(0);
		return (result <= 0);
	}
	
	/* (non-Javadoc)
	 * @see com.percero.agents.sync.datastore.IRedisCacheDataStore#removeSetValueAndGetSize(java.lang.String, java.lang.Object)
	 */
	@Override
	@Transactional
	public Long removeSetValueAndGetSize( final String key, final Object value ) {
		SetOperations<String, Object> setOps = template.opsForSet();
		setOps.remove(key, value);
		return setOps.size(key);
	}
	
	/* (non-Javadoc)
	 * @see com.percero.agents.sync.datastore.IRedisCacheDataStore#removeSetsValue(java.util.Collection, java.lang.Object)
	 */
	@Override
	public void removeSetsValue( final Collection<? extends Object> keys, final Object value ) {
		SetOperations<String, Object> setOps = template.opsForSet();
		Iterator<? extends Object> itrKeys = keys.iterator();
		while(itrKeys.hasNext()) {
			Object nextKey = itrKeys.next();
			try {
				setOps.remove((String) nextKey, value);
			} catch(Exception e) {
				log.warn("Invalid Key", e);
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see com.percero.agents.sync.datastore.IRedisCacheDataStore#removeSetsValue(java.lang.String, java.util.Collection, java.lang.Object)
	 */
	@Override
	public void removeSetsValue( final String keysPrefix, final Collection<? extends Object> keys, final Object value ) {
		SetOperations<String, Object> setOps = template.opsForSet();
		Iterator<? extends Object> itrKeys = keys.iterator();
		while(itrKeys.hasNext()) {
			Object nextKey = itrKeys.next();
			try {
				setOps.remove(keysPrefix + (String) nextKey, value);
			} catch(Exception e) {
				log.warn("Invalid Key", e);
			}
		}
	}
	
//	private static long keysCount = 0;
//	public Set<String> keys(String pattern) {
//		Set<String> result = template.keys(pattern);
//		log.info("Keys: " + ++keysCount);
//
//		return result;
//	}
	
	/* (non-Javadoc)
	 * @see com.percero.agents.sync.datastore.IRedisCacheDataStore#hasKey(java.lang.String)
	 */
	@Override
	public Boolean hasKey(String key) {
		Boolean result = template.hasKey(key);
		return result;
	}
	
	/* (non-Javadoc)
	 * @see com.percero.agents.sync.datastore.IRedisCacheDataStore#hasHashKey(java.lang.String, java.lang.String)
	 */
	@Override
	public Boolean hasHashKey(String key, String hashKey) {
		Boolean result = template.opsForHash().hasKey(key, hashKey);
		return result;
	}
	
	/* (non-Javadoc)
	 * @see com.percero.agents.sync.datastore.IRedisCacheDataStore#swapHashKey(java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	@Transactional
	public void swapHashKey( final String key, final String oldHashKey, final String newHashValue ) {
		HashOperations<String, Object, Object> hashOps = template.opsForHash();
		if (hashOps.hasKey(key, oldHashKey)) {
			hashOps.put(key, newHashValue, hashOps.get(key, oldHashKey));
			hashOps.delete(key, oldHashKey);
		}
	}
	
	/* (non-Javadoc)
	 * @see com.percero.agents.sync.datastore.IRedisCacheDataStore#renameIfAbsent(java.lang.String, java.lang.String)
	 */
	@Override
	public Boolean renameIfAbsent(String oldKey, String newKey) {
		return template.renameIfAbsent(oldKey, newKey);
	}
	
	/* (non-Javadoc)
	 * @see com.percero.agents.sync.datastore.IRedisCacheDataStore#rename(java.lang.String, java.lang.String)
	 */
	@Override
	public void rename(String oldKey, String newKey) {
		template.rename(oldKey, newKey);
	}
	
	/* (non-Javadoc)
	 * @see com.percero.agents.sync.datastore.IRedisCacheDataStore#setUnionAndStore(java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public Long setUnionAndStore(String key, String otherKey, String destKey) {
		return template.opsForSet().unionAndStore(key, otherKey, destKey);
	}
	
	/* (non-Javadoc)
	 * @see com.percero.agents.sync.datastore.IRedisCacheDataStore#setUnionAndStore(java.lang.String, java.util.Collection, java.lang.String)
	 */
	@Override
	public Long setUnionAndStore(String key, Collection<String> otherKeys, String destKey) {
		return template.opsForSet().unionAndStore(key, otherKeys, destKey);
	}
	
	/* (non-Javadoc)
	 * @see com.percero.agents.sync.datastore.IRedisDataStore#removeSetValues(java.lang.String, java.util.Collection)
	 */
	/* (non-Javadoc)
	 * @see com.percero.agents.sync.datastore.IRedisCacheDataStore#removeSetValues(java.lang.String, java.util.Collection)
	 */
	@Override
	@Transactional
	public void removeSetValues( final String key, final Collection<? extends Object> values ) {
		 SetOperations<String, Object> setOps = template.opsForSet();
		 
		 Iterator<? extends Object> itrValues = values.iterator();
		 while(itrValues.hasNext()) {
			 try {
				 setOps.remove(key, itrValues.next());
			 } catch(Exception e) {
				 log.error("Unable to remove Set Value", e);
			 }
		 }
	}
	
	/* (non-Javadoc)
	 * @see com.percero.agents.sync.datastore.IRedisDataStore#getSetValue(java.lang.String)
	 */
	/* (non-Javadoc)
	 * @see com.percero.agents.sync.datastore.IRedisCacheDataStore#getSetValue(java.lang.String)
	 */
	@Override
	public Set<? extends Object> getSetValue( final String key ) {
		return template.opsForSet().members(key);
	}
	
	/* (non-Javadoc)
	 * @see com.percero.agents.sync.datastore.IRedisCacheDataStore#replaceSet(java.lang.String, java.util.Collection)
	 */
	@Override
	@Transactional
	public Long replaceSet( final String key, Collection<? extends Object> values ) {
		template.delete(key);
		if (values != null) {
			return template.opsForSet().add(key, values.toArray());
		}
		else {
			return Long.valueOf(0);
		}
	}
	
	/* (non-Javadoc)
	 * @see com.percero.agents.sync.datastore.IRedisDataStore#swapSetValue(java.lang.String, java.lang.String, java.lang.String)
	 */
	/* (non-Javadoc)
	 * @see com.percero.agents.sync.datastore.IRedisCacheDataStore#swapSetValue(java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	@Transactional
	public void swapSetValue( final String key, final String oldValue, final String newValue ) {
		SetOperations<String, Object> setOps = template.opsForSet();
		if (setOps.isMember(key, oldValue)) {
			setOps.remove(key, oldValue);
			setOps.add(key, newValue);
		}
	}
	
	/* (non-Javadoc)
	 * @see com.percero.agents.sync.datastore.IRedisCacheDataStore#getSetIsMember(java.lang.String, java.lang.Object)
	 */
	@Override
	public Boolean getSetIsMember( final String key, final Object object ) {
		return template.opsForSet().isMember(key, object);
	}
	
	/* (non-Javadoc)
	 * @see com.percero.agents.sync.datastore.IRedisCacheDataStore#getSetsContainsMembers(java.util.Collection, java.lang.Object[])
	 */
	@Override
	@Transactional
	public Set<? extends Object> getSetsContainsMembers( final Collection<String> keys, final Object[] membersToCheck ) {
		SetOperations<String, Object> setOps = template.opsForSet();
		String randomKey = "UserTokenCheck:" + System.currentTimeMillis();
		setOps.add(randomKey, membersToCheck);
		
		Set<Object> intersectingMembers = new HashSet<Object>();
		Iterator<String> itrKeys = keys.iterator();
		while (itrKeys.hasNext()) {
			String nextKey = itrKeys.next();
			intersectingMembers.addAll(setOps.intersect(randomKey, nextKey));
		}
		template.delete(randomKey);
		return intersectingMembers;
	}
	
	/* (non-Javadoc)
	 * @see com.percero.agents.sync.datastore.IRedisCacheDataStore#getSetIntersect(java.lang.String, java.util.Collection)
	 */
	@Override
	public Set<Object> getSetIntersect( final String key, Collection<String> otherKeys ) {
		return template.opsForSet().intersect(key, otherKeys);
	}
	
	/* (non-Javadoc)
	 * @see com.percero.agents.sync.datastore.IRedisCacheDataStore#getSetIntersect(java.lang.String, java.lang.String)
	 */
	@Override
	public Set<Object> getSetIntersect( final String key, String otherKey ) {
		return template.opsForSet().intersect(key, otherKey);
	}
	
	/* (non-Javadoc)
	 * @see com.percero.agents.sync.datastore.IRedisCacheDataStore#getSetUnion(java.lang.String, java.util.Collection)
	 */
	@Override
	public Set<? extends Object> getSetUnion( final String key, final Collection<String> otherKeys ) {
		return template.opsForSet().union(key, otherKeys);
	}
	
	/* (non-Javadoc)
	 * @see com.percero.agents.sync.datastore.IRedisCacheDataStore#getSetUnion(java.lang.String, java.lang.String)
	 */
	@Override
	public Set<? extends Object> getSetUnion( final String key, final String otherKey ) {
		return template.opsForSet().union(key, otherKey);
	}
	
	/* (non-Javadoc)
	 * @see com.percero.agents.sync.datastore.IRedisCacheDataStore#deleteKey(java.lang.String)
	 */
	@Override
	public void deleteKey( final String key ) {
		template.delete(key);
	}
	
	/* (non-Javadoc)
	 * @see com.percero.agents.sync.datastore.IRedisCacheDataStore#deleteKeys(java.util.Collection)
	 */
	@Override
	public void deleteKeys( final Collection<String> keys ) {
		if (keys != null && !keys.isEmpty()) {
			template.delete(keys);
		}
	}

	/* (non-Javadoc)
	 * @see com.percero.agents.sync.datastore.IRedisCacheDataStore#setSetValue(java.lang.String, java.lang.Object)
	 */
	@Override
	@Transactional
	public Long setSetValue(String key, Object value) {
		return template.opsForSet().add(key, value);// ? Long.valueOf(1) : Long.valueOf(0);
	}

	/* (non-Javadoc)
	 * @see com.percero.agents.sync.datastore.IRedisCacheDataStore#getSet(java.lang.String)
	 */
	@Override
	public SetOperations<String, Object> getSet(String key) {
		return template.opsForSet();
	}

	/* (non-Javadoc)
	 * @see com.percero.agents.sync.datastore.IRedisCacheDataStore#getTemplate()
	 */
	@Override
	public RedisTemplate<String, Object> getTemplate() {
		return template;
	}

	/* (non-Javadoc)
	 * @see com.percero.agents.sync.datastore.IRedisCacheDataStore#keys(java.lang.String)
	 */
	@Override
	public Collection<String> keys(String pattern) {
		return template.keys(pattern);
	}

	/* (non-Javadoc)
	 * @see com.percero.agents.sync.datastore.IRedisCacheDataStore#removeKeysValue(java.lang.String, java.lang.Object)
	 */
	@Override
	@Transactional
	public void removeKeysValue(String pattern, Object value) {
		Set<String> keys = template.keys(pattern);
		Iterator<String> itrKeys = keys.iterator();
		
		while(itrKeys.hasNext()) {
			try {
				template.opsForSet().remove(itrKeys.next(), value);
			} catch(Exception e) {
				log.error("Error removing KeysValue", e);
			}
		}
	}
	
}
