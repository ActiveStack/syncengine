package com.percero.agents.sync.datastore;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.percero.agents.sync.services.PerceroRedisTemplate;

@Component
public class RedisClusterDataStore {

	private static Logger log = Logger.getLogger(RedisClusterDataStore.class);

	@Autowired
	private PerceroRedisTemplate template;
	
	public Boolean expire( final String key, final long timeout, final TimeUnit timeUnit ) {
		return template.expire(key, timeout, timeUnit);
	}
	 
	public Object getValue( final String key ) {
	    return template.opsForValue().get( key );
	}
	 
	public void setValue( final String key, final Object value ) {
	    template.opsForValue().set( key, value );
	}
	
	@Transactional
	public Long setSetValue( final String key, final Object value ) {
		return template.opsForSet().add(key, value);// ? Long.valueOf(1) : Long.valueOf(0);
	}
	
	@Transactional
	public Long lpushListValue( final String key, final Object value ) {
		return template.opsForList().leftPush(key, value);
	}
	
	@Transactional
	public List<Object> listAll( final String key) {
		return listRange(key, Long.valueOf(0), Long.valueOf(-1));
	}
	
	@Transactional
	public List<Object> listRange( final String key, final Long start, final Long end ) {
		return template.opsForList().range(key, start, end);
	}
	
	@Transactional
	public Object listIndex( final String key, final Long index ) {
		return template.opsForList().index(key, index);
	}
	
	@Transactional
	public Object listIndex( final String key ) {
		return template.opsForList().index(key, Long.valueOf(0));
	}
	
	@Transactional
	public void setHashValue( final String key, final String hashKey, final Object value ) {
		template.opsForHash().put(key, hashKey, value);
	}
	
	@Transactional
	public Long removeSetValue( final String key, final Object value ) {
		return template.opsForSet().remove(key, value);// ? Long.valueOf(1) : Long.valueOf(0);
	}
	
	public void removeSetsValue( final Collection<String> keys, final Object value ) {
		SetOperations<String, Object> setOps = template.opsForSet();
		Iterator<String> itrKeys = keys.iterator();
		while(itrKeys.hasNext()) {
			String nextKey = itrKeys.next();
			setOps.remove(nextKey, value);
		}
	}
	
	public SetOperations<String, Object> getSet( final String key ) {
		return template.opsForSet();
	}
	
	public RedisTemplate<String, Object> getTemplate() {
		return template;
	}
	
	public Collection<String> keys(String pattern) {
		return template.keys(pattern);
	}
	
	public Boolean renameIfAbsent(String oldKey, String newKey) {
		return template.renameIfAbsent(oldKey, newKey);
	}
	
	public void rename(String oldKey, String newKey) {
		template.rename(oldKey, newKey);
	}
	
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
	
	@Transactional
	public void removeKeysValue( final String pattern, final Object value ) {
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
	
	@Transactional
	public void removeSetValues( final String key, final Collection<Object> values ) {
		 SetOperations<String, Object> setOps = template.opsForSet();
		 
		 Iterator<Object> itrValues = values.iterator();
		 while(itrValues.hasNext()) {
			 try {
				 setOps.remove(key, itrValues.next());
			 } catch(Exception e) {
				 log.error("Unable to remove Set Value", e);
			 }
		 }
	}
	
	public Set<Object> getSetValue( final String key ) {
		template.opsForSet().members(key);
		return template.opsForSet().members(key);
	}
	
	@Transactional
	public void swapSetValue( final String key, final String oldValue, final String newValue ) {
		SetOperations<String, Object> setOps = template.opsForSet();
		if (setOps.isMember(key, oldValue)) {
			setOps.remove(key, oldValue);
			setOps.add(key, newValue);
		}
	}
	
	public Boolean getSetIsMember( final String key, final Object object ) {
		
		return template.opsForSet().isMember(key, object);
	}
	
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
	
	public Set<Object> getSetUnion( final String key, final Collection<String> otherKeys ) {
		SetOperations<String, Object> setOps = template.opsForSet();
		
		Set<Object> theSet = setOps.members(key);
		Iterator<String> itr = otherKeys.iterator();
		while(itr.hasNext()) {
			theSet.addAll(setOps.members(itr.next()));
		}
		return theSet;
	}
	
	public Set<Object> getSetUnion( final String key, final String otherKey ) {
		SetOperations<String, Object> setOps = template.opsForSet();
		
		Set<Object> theSet = setOps.members(key);
		theSet.addAll(setOps.members(otherKey));
		return theSet;
	}
	
	public void deleteKey( final String key ) {
		template.delete(key);
	}
	
	public void deleteKeys( final Collection<String> keys ) {
		if (keys.size() > 0)
			template.delete(keys);
	}
}
