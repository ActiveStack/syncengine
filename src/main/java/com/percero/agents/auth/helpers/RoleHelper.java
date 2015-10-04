package com.percero.agents.auth.helpers;

import com.percero.agents.auth.vo.IUserAnchor;
import com.percero.agents.auth.vo.IUserRole;
import com.percero.agents.sync.dao.DAORegistry;
import com.percero.agents.sync.dao.IDataAccessObject;
import com.percero.agents.sync.metadata.EntityImplementation;
import com.percero.agents.sync.metadata.MappedClass;
import com.percero.agents.sync.metadata.RelationshipImplementation;
import com.percero.framework.bl.ManifestHelper;
import com.percero.framework.vo.IPerceroObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jonnysamps on 10/2/15.
 */
public class RoleHelper {

    /**
     * Gets a list of role names for a user
     * @param userAnchor
     * @return
     */
    public static List<String> getUserRoleNames(IUserAnchor userAnchor){
        List<String> result = new ArrayList<>();
        List<IUserRole> roles = getUserRoles(userAnchor);

        for(IUserRole role : roles)
            result.add(role.getRoleName());

        return result;
    }

    public static List<IUserRole> getUserRoles(IUserAnchor userAnchor){
        List<IUserRole> result = new ArrayList<>();
        Class userRoleClass = ManifestHelper.findImplementingClass(IUserRole.class);
        RelationshipImplementation userAnchorRelImpl = getUserAnchorRI();

        try {
            // Now find the roles
            IDataAccessObject<IPerceroObject> userRoleDao =
                    (IDataAccessObject<IPerceroObject>) DAORegistry.getInstance().getDataAccessObject(userRoleClass.getName());
            IUserRole exampleRole = (IUserRole) userRoleClass.newInstance();

            userAnchorRelImpl.sourceMappedField.getSetter().invoke(exampleRole, userAnchor);

            List<IPerceroObject> roleList = userRoleDao.findByExample(exampleRole, null, null, false);
            for (IPerceroObject role : roleList) {
                result.add((IUserRole) role);
            }
        }catch(Exception e){}

        return result;
    }

    /**
     * Finds the UserRole implementation
     * @return
     */
    public static EntityImplementation getUserRoleEntityImplementation(){
        EntityImplementation userRoleEI = null;
        List<EntityImplementation> userRoleMappedClasses = MappedClass.findEntityImplementation(IUserRole.class);
        if (userRoleMappedClasses.size() > 0)
            userRoleEI = userRoleMappedClasses.get(0);

        return userRoleEI;
    }

    public static RelationshipImplementation getUserAnchorRI(){
        RelationshipImplementation userAnchorRelImpl =
        getUserRoleEntityImplementation().findRelationshipImplementationBySourceVarName(IUserRole.USER_ANCHOR_FIELD_NAME);
        return userAnchorRelImpl;
    }
}
