package com.percero.agents.sync.cw;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.mchange.v2.lang.ObjectUtils;
import com.percero.agents.sync.access.IAccessManager;
import com.percero.agents.sync.vo.BaseDataObject;
import com.percero.agents.sync.vo.ClassIDPair;
import com.percero.framework.vo.IPerceroObject;

/**
 * ChangeWatcherHelper allows code to be executed upon the change of any model
 * data. The ChangeWatcherHelper is used as the mechanism to execute that code.
 * A ChangeWatcherHelper must be registered with the ChangeWatcherHelperFactory.
 * Ex: ChangeWatcherHelperFactory.getInstance().registerChangeWatcherHelper(
 * "category", this );
 * 
 * @author Collin Brown
 * 
 */
@Component
public class ChangeWatcherHelper implements IChangeWatcherHelper {

	private static final Logger log = Logger.getLogger(ChangeWatcherHelper.class);


	@Autowired
	protected IAccessManager accessManager;
	public void setAccessManager(IAccessManager value) {
		accessManager = value;
	}

	public Object process(String category, String subCategory, String fieldName, IPerceroObject oldValue) {
		// This is really an error.
		StringBuilder strBuilder = new StringBuilder("No value calculate method found for: ").append(category).append(":").append(subCategory).append(":").append(fieldName);
		log.error(strBuilder.toString());
		
		return null;
	}
	
	public Object process(String category, String subCategory, String fieldName, String[] params, IPerceroObject oldValue) {
		// This is really an error.
		StringBuilder strBuilder = new StringBuilder("No value process method found for: ").append(category).append(":").append(subCategory).append(":").append(fieldName);
		for(String nextString : params) {
			strBuilder.append(".").append(nextString);
		}
		log.error(strBuilder.toString());
		
		return null;
	}
	
	
	public Object reprocess(String category, String subCategory, String fieldName, Collection<String> clientIds, String[] params, Long requestTimestamp, IPerceroObject oldValue) {
		ChangeWatcherReporting.reprocessCounter++;
		log.debug(ChangeWatcherReporting.stringResults());
		return process(category, subCategory, fieldName, params, oldValue);
	}
	
	
	@SuppressWarnings("unchecked")
	public static boolean eqOrBothNull(Object resultA, Object resultB) {
		if (resultA == null && resultB == null) {
			return true;
		}
		else if (resultA == null || resultB == null) {
			return false;
		}
		else {
			Class<? extends Object> classA = resultA.getClass();
			Class<? extends Object> classB = resultB.getClass();
			
			if (classA != classB) {
				return false;
			}
			else if (List.class.isAssignableFrom(classA)) {
				// This is a List of objects.
				List<? extends Object> listA = (List<? extends Object>) resultA;
				List<? extends Object> listB = (List<? extends Object>) resultB;
				
				// If the Lists are different sizes, then they are different.
				if (listA.size() != listB.size()) {
					return false;
				}
				// Else if the Lists have 0 size, then they are the same.
				else if (listA.size() == 0) {
					return true;
				}

				// Lists are ordered, so if elements are out of order then they are different.
				for(int i=0; i< listA.size(); i++) {
					Object nextA = listA.get(i);
					Object nextB = listB.get(i);

					Class<? extends Object> nextClassA = nextA.getClass();
					
					Class<? extends Object> nextClassB = nextB.getClass();
					if (nextClassA != nextClassB) {
						return false;
					}
					else if (ClassIDPair.class.isAssignableFrom(nextClassA)) {
						// ClassIDPair objects.
						ClassIDPair pairA = (ClassIDPair) nextA;
						ClassIDPair pairB = (ClassIDPair) nextB;
						
						if (!pairA.equals(pairB)) {
							return false;
						}
					}
					else if (IPerceroObject.class.isAssignableFrom(nextClassA)) {
						// Is some sort of Percero Object.
						IPerceroObject poA = (IPerceroObject) nextA;
						IPerceroObject poB = (IPerceroObject) nextB;
						
						if (poA.getID() == null && poB.getID() == null) {
							continue;
						}
						else if (poA.getID() == null || poB.getID() == null) {
							return false;
						}
						else {
							if (!poA.getID().equals(poB.getID())) {
								return false;
							}
						}
					}
					else {
						// Some sort of other base/primitive type.
						if (!ObjectUtils.eqOrBothNull(nextA, nextB)) {
							return false;
						}
					}
				}
				
				// At this point, the sizes of the List are the same 
				//	and every item in A exists in B, therefore every item 
				//	in B exists in A.  Also, the order is the same.
				return true;
			}
			else if (Collection.class.isAssignableFrom(classA)) {
				// This is a collection of objects.
				Collection<? extends Object> collA = (Collection<? extends Object>) resultA;
				Collection<? extends Object> collB = (Collection<? extends Object>) resultB;
				
				// If the collections are different sizes, then they are different.
				if (collA.size() != collB.size()) {
					return false;
				}
				// Else if the collections have 0 size, then they are the same.
				else if (collA.size() == 0) {
					return true;
				}
				
				Iterator<? extends Object> itrA = collA.iterator();
				Iterator<? extends Object> itrB = null;
				while (itrA.hasNext()) {
					Object nextA = itrA.next();
					Class<? extends Object> nextClassA = nextA.getClass();
					boolean nextAExistsInB = false;
					
					itrB = collB.iterator();
					while (itrB.hasNext()) {
						Object nextB = itrB.next();
						
						Class<? extends Object> nextClassB = nextB.getClass();
						if (nextClassA != nextClassB) {
							continue;
						}
						else if (ClassIDPair.class.isAssignableFrom(nextClassA)) {
							// ClassIDPair objects.
							ClassIDPair pairA = (ClassIDPair) nextA;
							ClassIDPair pairB = (ClassIDPair) nextB;
							
							if (pairA.equals(pairB)) {
								nextAExistsInB = true;
								break;
							}
						}
						else if (IPerceroObject.class.isAssignableFrom(nextClassA)) {
							// Is some sort of Percero Object.
							IPerceroObject poA = (IPerceroObject) nextA;
							IPerceroObject poB = (IPerceroObject) nextB;
							
							if (poA.getID() == null && poB.getID() == null) {
								nextAExistsInB = true;
								break;
							}
							else if (poA.getID() == null || poB.getID() == null) {
								continue;
							}
							else {
								if (poA.getID().equals(poB.getID())) {
									nextAExistsInB = true;
									break;
								}
							}
						}
						else {
							// Some sort of other base/primitive type.
							if (ObjectUtils.eqOrBothNull(nextA, nextB)) {
								nextAExistsInB = true;
								break;
							}
						}
					}
					
					if (!nextAExistsInB) {
						// nextA does not exist in B, so these are different.
						return false;
					}
				}
				
				// At this point, the sizes of the collections are the same 
				//	and every item in A exists in B, therefore every item 
				//	in B exists in A.
				return true;
			}
			else if (ClassIDPair.class.isAssignableFrom(classA)) {
				// ClassIDPair objects.
				ClassIDPair pairA = (ClassIDPair) resultA;
				ClassIDPair pairB = (ClassIDPair) resultB;
				
				return pairA.equals(pairB);
			}
			else if (IPerceroObject.class.isAssignableFrom(classA)) {
				// Is some sort of Percero Object.
				IPerceroObject poA = (IPerceroObject) resultA;
				IPerceroObject poB = (IPerceroObject) resultB;
				
				if (poA.getID() == null && poB.getID() == null) {
					return true;
				}
				else if (poA.getID() == null || poB.getID() == null) {
					return false;
				}
				else {
					return poA.getID().equals(poB.getID());
				}
			}
			else {
				// Some sort of other base/primitive type.
				return ObjectUtils.eqOrBothNull(resultA, resultB);
			}
		}
	}
	
	/**
	 * @param theObject
	 * @param results
	 * @return	TRUE if theObject was added to results. FALSE if results already contains theObject (by ID).
	 */
	public static Boolean addResultIfNotExists(BaseDataObject theObject, List<IPerceroObject> results) {
		Boolean objExists = false;
		Iterator<? extends IPerceroObject> itrResults = results.iterator();
		while (itrResults.hasNext()) {
			IPerceroObject nextResult = itrResults.next();
			if (nextResult.getID().equalsIgnoreCase(theObject.getID())) {
				objExists = true;
				break;
			}
		}
		
		if (!objExists) {
			results.add(theObject);
			return true;
		}
		else {
			return false;
		}
	}
}
