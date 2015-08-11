package com.percero.agents.sync.helpers;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.percero.agents.sync.access.IAccessManager;
import com.percero.agents.sync.vo.ClassIDPair;
import com.percero.framework.vo.IPerceroObject;

@Component
public class PostGetHelper {

	//private static final Logger log = Logger.getLogger(PostGetHelper.class);

	@Autowired
	IAccessManager accessManager;
	public void setAccessManager(IAccessManager value) {
		accessManager = value;
	}

	public void postGetObject(IPerceroObject perceroObject, String userId, String clientId) throws Exception {
		ClassIDPair pair = new ClassIDPair(perceroObject.getID(), perceroObject.getClass().getCanonicalName());
		accessManager.saveAccessJournal(pair, userId, clientId);
	}
	
	public void postGetObject(List<IPerceroObject> perceroObjects, String userId, String clientId) throws Exception {
		List<ClassIDPair> classIdPairs = new ArrayList<ClassIDPair>();
		Iterator<IPerceroObject> itrPerceroObjects = perceroObjects.iterator();
		while (itrPerceroObjects.hasNext()) {
			IPerceroObject nextPerceroObject = itrPerceroObjects.next();
			classIdPairs.add(new ClassIDPair(nextPerceroObject.getID(), nextPerceroObject.getClass().getCanonicalName()));
		}
		accessManager.saveAccessJournal(classIdPairs, userId, clientId);
	}
}
