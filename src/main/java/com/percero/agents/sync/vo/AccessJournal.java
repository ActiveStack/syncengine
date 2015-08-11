
/**
 * This is the persistence class for the Email entity.
 *
 * This is a sub-class of _Super_Email.java
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
 * com.com.percero.agents.auth.vo.Email in the classpath of the LCDS web application.
 *
 * Before deploying a model with the ServerGeneratedEntities flag set to false,
 * you will need to compile this class (and its associated classes) and place
 * them in the WEB-INF/classes or WEB-INF/lib directory of the J2EE application.
 *
 */
 
package com.percero.agents.sync.vo;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.hibernate.annotations.TypeDef;

import com.percero.hibernate.types.DateBigIntType;

@Entity
@TypeDef(name="bigIntDate", typeClass=DateBigIntType.class)
@Table(
		name="AccessJournal",
		uniqueConstraints = {
				@UniqueConstraint(columnNames = {"userID", "className", "classID"}),
				@UniqueConstraint(columnNames = {"className", "classID", "userID"})
			}
)
public class AccessJournal extends _Super_AccessJournal
{

}