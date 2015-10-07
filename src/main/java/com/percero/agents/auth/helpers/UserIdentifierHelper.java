package com.percero.agents.auth.helpers;

import com.percero.agents.auth.vo.IUserAnchor;
import com.percero.agents.auth.vo.IUserIdentifier;
import com.percero.agents.sync.dao.DAORegistry;
import com.percero.agents.sync.dao.IDataAccessObject;
import com.percero.agents.sync.metadata.EntityImplementation;
import com.percero.agents.sync.metadata.PropertyImplementation;
import com.percero.agents.sync.metadata.RelationshipImplementation;
import com.percero.framework.vo.IPerceroObject;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by jonnysamps on 10/7/15.
 */
public class UserIdentifierHelper {
    public static List<IUserIdentifier> getUserIdentifiersForUser(IUserAnchor user){
        List<IUserIdentifier> result = new ArrayList<>();

        return result;
    }

    public static IUserIdentifier getUserIdentifierForUserAndValue(EntityImplementation ei, IUserAnchor userAnchor, String value){

        IUserIdentifier result = null;
        IDataAccessObject<IPerceroObject> dao =
                (IDataAccessObject<IPerceroObject>) DAORegistry.getInstance().getDataAccessObject(ei.mappedClass.className);
        PropertyImplementation userIdentifierPropImpl = ei.findPropertyImplementationByName(IUserIdentifier.USER_IDENTIFIER_FIELD_NAME);
        RelationshipImplementation userAnchorRelImpl = ei.findRelationshipImplementationBySourceVarName(IUserIdentifier.USER_ANCHOR_FIELD_NAME);

        try {
            IUserIdentifier example = (IUserIdentifier) ei.mappedClass.clazz.newInstance();
            Method valueSetter = userIdentifierPropImpl.mappedField.getSetter();
            valueSetter.invoke(example, value);
            Method userSetter = userAnchorRelImpl.sourceMappedField.getSetter();
            userSetter.invoke(example, userAnchor);
            List <IPerceroObject> list = dao.findByExample((IPerceroObject) example, null, null, false);
            if(list.size() > 0)
                result = (IUserIdentifier) list.get(0);

        }catch(Exception e){}

        return result;
    }


}

