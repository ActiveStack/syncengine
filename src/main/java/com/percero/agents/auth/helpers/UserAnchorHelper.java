package com.percero.agents.auth.helpers;

import com.percero.agents.auth.vo.IUserAnchor;
import com.percero.agents.sync.dao.DAORegistry;
import com.percero.agents.sync.dao.IDataAccessObject;
import com.percero.agents.sync.metadata.EntityImplementation;
import com.percero.agents.sync.metadata.MappedClass;
import com.percero.framework.bl.ManifestHelper;
import com.percero.framework.vo.IPerceroObject;

import java.util.List;

/**
 * Created by jonnysamps on 10/2/15.
 */
public class UserAnchorHelper {
    public static IUserAnchor getUserAnchor(String userId){
        IUserAnchor result = null;
        Class userAnchorClass = ManifestHelper.findImplementingClass(IUserAnchor.class);

        IDataAccessObject<IPerceroObject> userAnchorDao =
                (IDataAccessObject<IPerceroObject>) DAORegistry.getInstance().getDataAccessObject(userAnchorClass.getName());

        try {
            IUserAnchor example = (IUserAnchor) userAnchorClass.newInstance();
            example.setUserId(userId);
            List<IPerceroObject> list = userAnchorDao.findByExample((IPerceroObject)example, null, null, false);
            if(list.size() > 0)
                result = (IUserAnchor)list.get(0);
        }catch(Exception e){}

        return result;
    }

    public static EntityImplementation getUserAnchorEntityImplementation(){
        EntityImplementation userAnchorEI = null;
        List<EntityImplementation> userAnchorMappedClasses = MappedClass.findEntityImplementation(IUserAnchor.class);
        if (userAnchorMappedClasses.size() > 0) {
            userAnchorEI = userAnchorMappedClasses.get(0);
        }
        return userAnchorEI;
    }
}
