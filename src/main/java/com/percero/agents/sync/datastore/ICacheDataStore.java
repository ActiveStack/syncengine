package com.percero.agents.sync.datastore;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

public interface ICacheDataStore {

	public abstract Boolean expire(String key, long timeout, TimeUnit timeUnit);

	public abstract Boolean expire(Collection<String> keys, long timeout,
			TimeUnit timeUnit);

	public abstract Boolean expire(String key, long timeout, TimeUnit timeUnit,
			Boolean forceNow);

	public abstract Boolean expire(Collection<String> keys, long timeout,
			TimeUnit timeUnit, Boolean forceNow);

	@Scheduled(fixedRate = 600000)
	// 10 * 60 * 1000 -> Ten Minutes.
	public abstract void postExpires();

	public abstract Object getValue(String key);

	public abstract List<Object> getValues(Collection<String> keys);

	public abstract void setValue(String key, Object value);

	//	public void setValues( final KeyValuePair[] pairs ) {
	public abstract void setValues(
			Map<String, ? extends Object> keysAndValuesMap);

	@Transactional
	public abstract Long addSetValue(String key, Object value);

	@Transactional
	public abstract Long lpushListValue(String key, Object value);

	@Transactional
	public abstract List<? extends Object> listAll(String key);

	@Transactional
	public abstract List<? extends Object> listRange(String key, Long start, Long end);

	@Transactional
	public abstract Object listIndex(String key, Long index);

	@Transactional
	public abstract Object listIndex(String key);

	@Transactional
	public abstract Object getHashValue(String key, String hashKey);

	@Transactional
	public abstract Set<String> getHashKeys(String key);

	@Transactional
	public abstract Map<String, Object> getHashEntries(String key);

	@Transactional
	public abstract Long getHashSize(String key);

	@Transactional
	public abstract void deleteHashKey(String key, String hashKey);

	@Transactional
	public abstract void setHashValue(String key, String hashKey, Object value);

	@Transactional
	public abstract void setAllHashValues(String key,
			Map<? extends Object, ? extends Object> hashMap);

	@Transactional
	public abstract Long removeSetValue(String key, Object value);

	@Transactional
	public abstract Boolean getSetIsEmpty(String key);

	@Transactional
	public Long getSetSize( final String key );

	@Transactional
	public abstract Long removeSetValueAndGetSize(String key, Object value);

	public abstract void removeSetsValue(Collection<? extends Object> keys,
			Object value);

	public abstract void removeSetsValue(String keysPrefix,
			Collection<? extends Object> keys, Object value);

	public abstract Boolean hasKey(String key);

	public abstract Boolean hasHashKey(String key, String hashKey);

	@Transactional
	public abstract void swapHashKey(String key, String oldHashKey,
			String newHashValue);

	public abstract Boolean renameIfAbsent(String oldKey, String newKey);

	public abstract void rename(String oldKey, String newKey);

	public abstract Long setUnionAndStore(String key, String otherKey,
			String destKey);

	public abstract Long setUnionAndStore(String key,
			Collection<String> otherKeys, String destKey);

	/* (non-Javadoc)
	 * @see com.percero.agents.sync.datastore.IRedisDataStore#removeSetValues(java.lang.String, java.util.Collection)
	 */
	@Transactional
	public abstract void removeSetValues(String key,
			Collection<? extends Object> values);

	/* (non-Javadoc)
	 * @see com.percero.agents.sync.datastore.IRedisDataStore#getSetValue(java.lang.String)
	 */
	public abstract Set<? extends Object> getSetValue(String key);

	@Transactional
	public abstract Long replaceSet(String key,
			Collection<? extends Object> values);

	/* (non-Javadoc)
	 * @see com.percero.agents.sync.datastore.IRedisDataStore#swapSetValue(java.lang.String, java.lang.String, java.lang.String)
	 */
	@Transactional
	public abstract void swapSetValue(String key, String oldValue,
			String newValue);

	public abstract Boolean getSetIsMember(String key, Object object);

	@Transactional
	public abstract Set<? extends Object> getSetsContainsMembers(
			Collection<String> keys, Object[] membersToCheck);

	public abstract Set<Object> getSetIntersect(String key,
			Collection<String> otherKeys);

	public abstract Set<Object> getSetIntersect(String key, String otherKey);

	public abstract Set<? extends Object> getSetUnion(String key,
			Collection<String> otherKeys);

	public abstract Set<? extends Object> getSetUnion(String key,
			String otherKey);

	public abstract void deleteKey(String key);

	public abstract void deleteKeys(Collection<String> keys);

	@Transactional
	public abstract Long setSetValue(String key, Object value);

	public abstract SetOperations<String, Object> getSet(String key);

	public abstract RedisTemplate<String, Object> getTemplate();

	public abstract Collection<String> keys(String pattern);

	@Transactional
	public abstract void removeKeysValue(String pattern, Object value);

}