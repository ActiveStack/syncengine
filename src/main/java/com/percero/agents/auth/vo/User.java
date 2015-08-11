
/**
 * This is the persistence class for the User entity.
 *
 * This is a sub-class of _Super_User.java
 * and is intended for behavior customization.
 *
 * This class is only generated when there is no file already present
 * at its target location.  Thus custom behavior that you add here will
 * survive regeneration of the super-class.
 *
 * In order to prevent the generation of entity classes when a model
 * is deployed, the following annotation must be included at the top
 * level of your data model:
 * <model>
 *   <annotation name="DMS">
 *       <item name="ServerGeneratedEntities">false</item>
 *   </annotation>
 *
 * The server will then expect to find a JPA annotated JavaBean named
 * com.com.percero.agents.auth.vo.User in the classpath of the LCDS web application.
 *
 * Before deploying a model with the ServerGeneratedEntities flag set to false,
 * you will need to compile this class (and its associated classes) and place
 * them in the WEB-INF/classes or WEB-INF/lib directory of the J2EE application.
 *
 */
 
package com.percero.agents.auth.vo;

import javax.persistence.*;

@Entity
public class User extends _Super_User
{

	/**
	 * 
	 */
	private static final long serialVersionUID = 7087158786648386663L;

}