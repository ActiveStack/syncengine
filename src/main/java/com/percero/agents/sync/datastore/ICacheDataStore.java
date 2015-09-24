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

	Boolean expire(String key, long timeout, TimeUnit timeUnit);

	Boolean expire(Collection<String> keys, long timeout,
			TimeUnit timeUnit);

	Boolean expire(String key, long timeout, TimeUnit timeUnit,
			Boolean forceNow);

	Boolean expire(Collection<String> keys, long timeout,
			TimeUnit timeUnit, Boolean forceNow);

	@Scheduled(fixedRate = 600000)
	// 10 * 60 * 1000 -> Ten Minutes.
	void postExpires();

	Object getValue(String key);

	List<Object> getValues(Collection<String> keys);

	void setValue(String key, Object value);

	//	public void setValues( final KeyValuePair[] pairs ) {
	void setValues(
			Map<String, ? extends Object> keysAndValuesMap);

	@Transactional
	Long addSetValue(String key, Object value);

	@Transactional
	Long lpushListValue(String key, Object value);

	@Transactional
	List<? extends Object> listAll(String key);

	@Transactional
	List<? extends Object> listRange(String key, Long start, Long end);

	@Transactional
	Object listIndex(String key, Long index);

	@Transactional
	Object listIndex(String key);

	@Transactional
	Object getHashValue(String key, String hashKey);

	@Transactional
	Set<String> getHashKeys(String key);

	@Transactional
	Map<String, Object> getHashEntries(String key);

	@Transactional
	Long getHashSize(String key);

	@Transactional
	void deleteHashKey(String key, String hashKey);

	@Transactional
	void setHashValue(String key, String hashKey, Object value);

	@Transactional
	void setAllHashValues(String key,
			Map<? extends Object, ? extends Object> hashMap);

	@Transactional
	Long removeSetValue(String key, Object value);

	@Transactional
	Boolean getSetIsEmpty(String key);

	@Transactional
	public Long getSetSize( final String key );

	@Transactional
	Long removeSetValueAndGetSize(String key, Object value);

	void removeSetsValue(Collection<? extends Object> keys,
			Object value);

	void removeSetsValue(String keysPrefix,
			Collection<? extends Object> keys, Object value);

	Boolean hasKey(String key);

	Boolean hasHashKey(String key, String hashKey);

	@Transactional
	void swapHashKey(String key, String oldHashKey,
			String newHashValue);

	Boolean renameIfAbsent(String oldKey, String newKey);

	void rename(String oldKey, String newKey);

	Long setUnionAndStore(String key, String otherKey,
			String destKey);

	Long setUnionAndStore(String key,
			Collection<String> otherKeys, String destKey);

	/* (non-Javadoc)
	 * @see com.percero.agents.sync.datastore.IRedisDataStore#removeSetValues(java.lang.String, java.util.Collection)
	 */
	@Transactional
	void removeSetValues(String key,
			Collection<? extends Object> values);

	/* (non-Javadoc)
	 * @see com.percero.agents.sync.datastore.IRedisDataStore#getSetValue(java.lang.String)
	 */
	Set<? extends Object> getSetValue(String key);

	@Transactional
	Long replaceSet(String key,
			Collection<? extends Object> values);

	/* (non-Javadoc)
	 * @see com.percero.agents.sync.datastore.IRedisDataStore#swapSetValue(java.lang.String, java.lang.String, java.lang.String)
	 */
	@Transactional
	void swapSetValue(String key, String oldValue,
			String newValue);

	Boolean getSetIsMember(String key, Object object);

	@Transactional
	Set<? extends Object> getSetsContainsMembers(
			Collection<String> keys, Object[] membersToCheck);

	Set<Object> getSetIntersect(String key,
			Collection<String> otherKeys);

	Set<Object> getSetIntersect(String key, String otherKey);

	Set<? extends Object> getSetUnion(String key,
			Collection<String> otherKeys);

	Set<? extends Object> getSetUnion(String key,
			String otherKey);

	void deleteKey(String key);

	void deleteKeys(Collection<String> keys);

	@Transactional
	Long setSetValue(String key, Object value);

	SetOperations<String, Object> getSet(String key);

	RedisTemplate<String, Object> getTemplate();

	Collection<String> keys(String pattern);

	@Transactional
	void removeKeysValue(String pattern, Object value);

}