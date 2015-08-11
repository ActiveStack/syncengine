package com.percero.agents.sync.metadata;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

import org.apache.log4j.Logger;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import com.percero.agents.sync.annotations.PerceroNamedNativeQueries;
import com.percero.agents.sync.annotations.PerceroNamedNativeQuery;
import com.percero.agents.sync.hibernate.SyncHibernateUtils;
import com.percero.agents.sync.metadata.annotations.AccessRight;
import com.percero.agents.sync.metadata.annotations.AccessRights;
import com.percero.agents.sync.metadata.annotations.DataProvider;
import com.percero.agents.sync.metadata.annotations.EntityInterface;
import com.percero.agents.sync.metadata.annotations.EntityInterfaces;
import com.percero.agents.sync.metadata.annotations.Externalize;
import com.percero.agents.sync.metadata.annotations.PropertyInterface;
import com.percero.agents.sync.metadata.annotations.PropertyInterfaceParam;
import com.percero.agents.sync.metadata.annotations.PropertyInterfaces;
import com.percero.agents.sync.metadata.annotations.RelationshipInterface;
import com.percero.agents.sync.metadata.annotations.RelationshipInterfaces;
import com.percero.framework.bl.IManifest;
import com.percero.framework.bl.ManifestHelper;
import com.percero.framework.metadata.IMappedClass;
import com.percero.framework.metadata.IMappedQuery;
import com.percero.framework.vo.IPerceroObject;

@SuppressWarnings("unchecked")
public class MappedClass implements IMappedClass {
	
	private static Logger logger = Logger.getLogger(MappedClass.class);
	
	public static Map<Class<?>,List<EntityImplementation>> entityInterfacesMappedClasses = Collections.synchronizedMap(new HashMap<Class<?>, List<EntityImplementation>>());
	
	public static Boolean allMappedClassesInitialized = false;

	@SuppressWarnings("rawtypes")
	public static void processManifest(IManifest manifest) {
		if (!allMappedClassesInitialized) {
			try {
				IMappedClassManager mcm = MappedClassManagerFactory.getMappedClassManager();
				Set<MappedClass> mappedClasses = new HashSet<MappedClass>();
				ManifestHelper.setManifest(manifest);
				Iterator<String> itrUuidMap = manifest.getUuidMap().keySet().iterator();
				while(itrUuidMap.hasNext()) {
					String nextUuid = itrUuidMap.next();
					Class nextClass = manifest.getUuidMap().get(nextUuid);
					
					MappedClass mc = mcm.getMappedClassByClassName(nextClass.getCanonicalName());
					mappedClasses.add(mc);
				}
	
				// Initialize the Fields
				Iterator<MappedClass> itrMappedClasses = mappedClasses.iterator();
				while (itrMappedClasses.hasNext()) {
					MappedClass mc = itrMappedClasses.next();
					mc.initializeFields();
				}
				// Initialize the Queries
				itrMappedClasses = mappedClasses.iterator();
				while (itrMappedClasses.hasNext()) {
					MappedClass mc = itrMappedClasses.next();
					mc.initializeQueries();
				}
				// Initialize the Relationships
				itrMappedClasses = mappedClasses.iterator();
				while (itrMappedClasses.hasNext()) {
					MappedClass mc = itrMappedClasses.next();
					mc.initializeRelationships();
				}
				
				// Now set the childMappedClasses.
				itrMappedClasses = mappedClasses.iterator();
				while (itrMappedClasses.hasNext()) {
					MappedClass mc = itrMappedClasses.next();
					Class clazz = mc.clazz.getSuperclass();
					int classLevel = 0;
					while (clazz != null) {
						if (manifest.getClassList().contains(clazz)) {
							MappedClass nextMc = mcm.getMappedClassByClassName(clazz.getCanonicalName());
							nextMc.childMappedClasses.add(mc);
							if (classLevel == 0) {
								mc.parentMappedClass = nextMc;
							}
							classLevel++;
						}
						clazz = clazz.getSuperclass();
					}
				}
				
				
				MappedClass.allMappedClassesInitialized = true;
	
			} catch(Exception e) {
				logger.error("Unable to process manifest.", e);
			}
		}
	}
	
	@SuppressWarnings("rawtypes")
	public static List<EntityImplementation> findEntityImplementation(Class interfaceClazz) {
		return entityInterfacesMappedClasses.get(interfaceClazz);
	}

	private static Comparator<MappedField> fieldComparator;
	
	static {
		fieldComparator = new Comparator<MappedField>() {
			public int compare(MappedField o1, MappedField o2) {
				return o1.getField().getName().compareToIgnoreCase(o2.getField().getName());
			}
		};
	}
	
	public int ID = 0;
	public Boolean needsReadCleaning = false;
	public Boolean getNeedsReadCleaning () {
		if (getReadAccessRightsFieldReferences() != null && !getReadAccessRightsFieldReferences().isEmpty()) {
			return true;
		}
		else if (getReadQuery() != null) {
			return true;
		}
		else if (getReadAllQuery() != null) {
			return true;
		}
		else {
			return false;
		}
	}
	
	public Boolean fieldsInitialized = false;
	public Boolean queriesInitialized = false;
	public Boolean relationshipsInitialized = false;
//	private Boolean isInitializing = false;
	public String dataProviderName = "";
	public String className = "";
	public String tableName = "";
	public List<MappedField> requiredFields = Collections.synchronizedList(new ArrayList<MappedField>());
	public Set<MappedField> toManyFields = Collections.synchronizedSet(new HashSet<MappedField>());
	public Set<MappedField> toOneFields = Collections.synchronizedSet(new HashSet<MappedField>());
	public Set<MappedField> propertyFields = Collections.synchronizedSet(new HashSet<MappedField>());
	public List<Field> entityFields = Collections.synchronizedList(new ArrayList<Field>());
	public List<Field> mapFields = Collections.synchronizedList(new ArrayList<Field>());
	public List<Field> listFields = Collections.synchronizedList(new ArrayList<Field>());
	public List<MappedField> nonLazyLoadingFields = Collections.synchronizedList(new ArrayList<MappedField>());
	public Set<MappedFieldPerceroObject> externalizablePerceroObjectFields = Collections.synchronizedSet(new HashSet<MappedFieldPerceroObject>());
	public Set<MappedField> externalizableFields = Collections.synchronizedSet(new TreeSet<MappedField>(fieldComparator));
	public Map<MappedField, MappedField> cascadeRemoveFieldReferences = Collections.synchronizedMap(new HashMap<MappedField, MappedField>());
	public Map<MappedField, MappedField> nulledOnRemoveFieldReferences = Collections.synchronizedMap(new HashMap<MappedField, MappedField>());
	@SuppressWarnings("rawtypes")
	public Map<Class, EntityImplementation> entityImplementations = Collections.synchronizedMap(new HashMap<Class, EntityImplementation>());
	public MappedClass parentMappedClass = null;
	public List<MappedClass> childMappedClasses = Collections.synchronizedList(new ArrayList<MappedClass>());
	public List<MappedClass> getChildMappedClasses() {
		return this.childMappedClasses;
	}
	
	
	/**
	 * readAccessRightsFieldReferences holds all MappedFields that have some sort of
	 * readAccessRights associated with that field.  This means that this field needs
	 * to be recalculated for each User.
	 * If a MappedClass has NO fields in this list AND NO readQuery, then all objects
	 * of that type do NOT need to be recalculated for each User.
	 */
	public Set<MappedField> readAccessRightsFieldReferences = new HashSet<MappedField>();
	public Set<MappedField> getReadAccessRightsFieldReferences() {
		return readAccessRightsFieldReferences;
	}
	public List<Object> uniqueConstraints = new ArrayList<Object>();
	public List<IMappedQuery> queries = new ArrayList<IMappedQuery>();
	public Boolean hasGeneratedId = false;
	public Boolean hasNonLazyLoadProperties = false;
	
	@SuppressWarnings("rawtypes")
	public Class clazz = null;
	
	public MappedField getExternalizeFieldByName(String fieldName) {
		for (MappedField mappedField : externalizableFields) {
			if (mappedField.getField().getName().equals(fieldName))
				return mappedField;
		}
		
		return null;
	}
	
	private IMappedQuery readAllQuery = null;
	public IMappedQuery getReadAllQuery() {
		return readAllQuery;
	}

	public void setReadAllQuery(IMappedQuery query) {
		this.readAllQuery = query;
	}
	
	private IMappedQuery createQuery = null;
	public IMappedQuery getCreateQuery() {
		return createQuery;
	}

	public void setCreateQuery(IMappedQuery createQuery) {
		this.createQuery = createQuery;
	}

	private IMappedQuery updateQuery = null;
	public IMappedQuery getUpdateQuery() {
		return updateQuery;
	}

	public void setUpdateQuery(IMappedQuery updateQuery) {
		this.updateQuery = updateQuery;
	}

	private IMappedQuery readQuery = null;
	public IMappedQuery getReadQuery() {
		return readQuery;
	}
	
	public void setReadQuery(IMappedQuery readQuery) {
		this.readQuery = readQuery;
	}
	
	private IMappedQuery deleteQuery = null;
	public IMappedQuery getDeleteQuery() {
		return deleteQuery;
	}

	public void setDeleteQuery(IMappedQuery deleteQuery) {
		this.deleteQuery = deleteQuery;
	}


	@SuppressWarnings("rawtypes")
	public MappedClass(int theId, String aClassName) {
		ID = theId;
		className = aClassName;
		try {
			clazz = MappedClass.forName(className);
		} catch(Exception e) {
			logger.error("Unable to instantiate class " + className, e);
		}
		
		MappedClassManagerFactory.getMappedClassManager();
		
		// Look for EntityInterfaces
		Class nextClazz = clazz;
		while (nextClazz != null) {
			EntityInterface entityInterface = (EntityInterface) nextClazz.getAnnotation(EntityInterface.class);
			if (entityInterface != null) {
				if (entityInterface.interfaceClass() != null) {
					List<EntityImplementation> allEntityImplementations = MappedClass.entityInterfacesMappedClasses.get(entityInterface.interfaceClass());
					if (allEntityImplementations == null) {
						allEntityImplementations = new ArrayList<EntityImplementation>();
						MappedClass.entityInterfacesMappedClasses.put(entityInterface.interfaceClass(), allEntityImplementations);
					}
					
					EntityImplementation entityImpl = processEntityInterface(entityInterface);
					allEntityImplementations.add(entityImpl);
				}
			}
	
			EntityInterfaces entityInterfaces = (EntityInterfaces) nextClazz.getAnnotation(EntityInterfaces.class);
			if (entityInterfaces != null) {
				for(EntityInterface nextEntityInterface : entityInterfaces.entityInterfaces()) {
					if (nextEntityInterface.interfaceClass() != null) {
						List<EntityImplementation> allEntityImplementations = MappedClass.entityInterfacesMappedClasses.get(nextEntityInterface.interfaceClass());
						if (allEntityImplementations == null) {
							allEntityImplementations = new ArrayList<EntityImplementation>();
							MappedClass.entityInterfacesMappedClasses.put(nextEntityInterface.interfaceClass(), allEntityImplementations);
						}
						
						EntityImplementation entityImpl = processEntityInterface(entityInterface);
						if (!allEntityImplementations.contains(entityImpl)) {
							allEntityImplementations.add(entityImpl);
						}
					}
	
				}
			}
			nextClazz = nextClazz.getSuperclass();
		}
	}
	
	protected EntityImplementation processEntityInterface(EntityInterface entityInterface) {
		if (entityInterface == null || entityInterface.interfaceClass() == null) {
			logger.warn("Invalid EntityInterface on class " + className);
			return null;
		}

		// Check to see if interface has already been processed.
		EntityImplementation entityImpl = entityImplementations.get(entityInterface.interfaceClass());
		if (entityImpl != null) {
			// This EntityInterface has already been processed.
			return entityImpl;
		}
		
		entityImpl = new EntityImplementation();
		entityImpl.mappedClass = this;
		entityImpl.entityInterfaceClass = entityInterface.interfaceClass();
		
		entityImplementations.put(entityInterface.interfaceClass(), entityImpl);
		return entityImpl;
	}
	
	@SuppressWarnings("rawtypes")
	public void initializeFields() {
		if (fieldsInitialized) {
			return;
		}
		
		try {
			Class clazz = MappedClass.forName(className);
			
			List<Field> fields = SyncHibernateUtils.getClassFields(clazz);
			for(Field nextField : fields) {
				// Ignore this field if marked as Transient.
				Transient transientAnno = (Transient) nextField.getAnnotation(Transient.class);
				if (transientAnno != null)
					continue;
				
				Externalize externalize = (Externalize) nextField.getAnnotation(Externalize.class);
				if (externalize == null) {
					// Only process Externalizeable fields.
					continue;
				}
				
				// Check to see if this Field has already been processed.
				Iterator<MappedField> itrExternalizeFields = externalizableFields.iterator();
				Boolean fieldAlreadyMapped = false;
				while (itrExternalizeFields.hasNext()) {
					MappedField nextExistingMappedField = itrExternalizeFields.next();
					if (nextExistingMappedField.getField().getName().equals(nextField.getName())) {
						// This field has already been mapped.
						fieldAlreadyMapped = true;
						break;
					}
				}
				
				if (fieldAlreadyMapped) {
					continue;
				}
				
				Method theGetter = SyncHibernateUtils.getFieldGetters(clazz, nextField);
				transientAnno = (Transient) theGetter.getAnnotation(Transient.class);
				if (transientAnno != null)
					continue;
				Method theSetter = SyncHibernateUtils.getFieldSetters(clazz, nextField);
				
				MappedField nextMappedField = null;
				Class nextFieldClass = nextField.getType();
				if (nextFieldClass == int.class)
					nextMappedField = new MappedFieldInt();
				else if (nextFieldClass == Integer.class)
					nextMappedField = new MappedFieldInteger();
				else if (nextFieldClass == float.class)
					nextMappedField = new MappedFieldFloat();
				else if (nextFieldClass == Float.class)
					nextMappedField = new MappedFieldFloat();
				else if (nextFieldClass == double.class)
					nextMappedField = new MappedFieldDouble();
				else if (nextFieldClass == Double.class)
					nextMappedField = new MappedFieldDouble();
				else if (nextFieldClass == boolean.class)
					nextMappedField = new MappedFieldBool();
				else if (nextFieldClass == Boolean.class)
					nextMappedField = new MappedFieldBoolean();
				else if (nextFieldClass == String.class)
					nextMappedField = new MappedFieldString();
				else if (nextFieldClass == Date.class)
					nextMappedField = new MappedFieldDate();
/**				else if (nextFieldClass == Map.class)
					nextMappedField = new MappedFieldMap();
				else if (nextFieldClass == List.class)
					nextMappedField = new MappedFieldList();**/
				else if (implementsInterface(nextFieldClass, IPerceroObject.class))
					nextMappedField = new MappedFieldPerceroObject();
				else if (implementsInterface(nextFieldClass, Map.class))
					nextMappedField = new MappedFieldMap();
				else if (implementsInterface(nextFieldClass, List.class))
					nextMappedField = new MappedFieldList();

				else
					nextMappedField = new MappedField();

				nextMappedField.setMappedClass(this);
				nextMappedField.setField(nextField);
				nextMappedField.setGetter(theGetter);
				nextMappedField.setSetter(theSetter);
				
				nextMappedField.setUseLazyLoading(externalize.useLazyLoading());
				if (!nextMappedField.getUseLazyLoading())
					this.hasNonLazyLoadProperties = true;
				
				if (!externalize.useLazyLoading())
					nonLazyLoadingFields.add(nextMappedField);
				externalizableFields.add(nextMappedField);
				if (nextMappedField instanceof MappedFieldPerceroObject)
					externalizablePerceroObjectFields.add((MappedFieldPerceroObject) nextMappedField);
				
				OneToMany oneToMany = (OneToMany) theGetter.getAnnotation(OneToMany.class);
				if (oneToMany == null)
					oneToMany = (OneToMany) nextField.getAnnotation(OneToMany.class);
				
				if (oneToMany != null) {
					toManyFields.add(nextMappedField);
				}

				Boolean isPropertyField = true;
				Entity nextEntity = (Entity) nextField.getType().getAnnotation(Entity.class);
				if (nextEntity != null)
				{
					entityFields.add(nextField);
					toOneFields.add(nextMappedField);
					isPropertyField = false;
				}
				
				if (inheritsFrom(nextField.getType(), Map.class))
				{
					mapFields.add(nextField);
					toManyFields.add(nextMappedField);
					isPropertyField = false;
				}
				
				if (inheritsFrom(nextField.getType(), List.class))
				{
					listFields.add(nextField);
					toManyFields.add(nextMappedField);
					isPropertyField = false;
				}
				
				if (isPropertyField) {
					propertyFields.add(nextMappedField);
				}
				
				Id id = (Id) theGetter.getAnnotation(Id.class);
				if (id == null)
					id = (Id) nextField.getAnnotation(Id.class);
				if (id != null) {
					uniqueConstraints.add(nextMappedField);
					
					// Check to see if this class has a Generated ID.
					GeneratedValue generatedValue = (GeneratedValue) nextField.getAnnotation(GeneratedValue.class);
					hasGeneratedId = (generatedValue != null);
				}
				
				Column column = (Column) theGetter.getAnnotation(Column.class);
				if (column == null)
					column = (Column) nextField.getAnnotation(Column.class);
				if (column != null) {
					if (column.unique())
						uniqueConstraints.add(nextMappedField);
					
					if (column.name() != null && column.name().trim().length() > 0)
						nextMappedField.setColumnName(column.name());
					else
						nextMappedField.setColumnName(nextField.getName());
				}

				// Get NamedQueries for handling Access Rights.
				AccessRights accessRights = (AccessRights) nextField.getAnnotation(AccessRights.class);
				if (accessRights == null)
					accessRights = (AccessRights) nextMappedField.getGetter().getAnnotation(AccessRights.class);
				if (accessRights != null) {
					for(AccessRight nextAccessRight : accessRights.value()) {
						/*if (nextAccessRight.type().equalsIgnoreCase("createQuery")) {
							if (nextAccessRight.query().indexOf("jpql:") >= 0)
								nextMappedField.createQuery = new JpqlQuery();
							else
								nextMappedField.createQuery = new MappedQuery();
							nextMappedField.createQuery.setQuery(nextAccessRight.query());
						} else if (nextAccessRight.type().equalsIgnoreCase("updateQuery")) {
							if (nextAccessRight.query().indexOf("jpql:") >= 0)
								nextMappedField.updateQuery = new JpqlQuery();
							else
								nextMappedField.updateQuery = new MappedQuery();
							nextMappedField.updateQuery.setQuery(nextAccessRight.query());
						} else*/ 
						if (nextAccessRight.type().equalsIgnoreCase("readQuery")) {
							if (nextAccessRight.query().indexOf("jpql:") >= 0)
								nextMappedField.setReadQuery(new JpqlQuery());
							else
								nextMappedField.setReadQuery(new MappedQuery());
							nextMappedField.getReadQuery().setQuery(nextAccessRight.query());
							nextMappedField.setHasReadAccessRights(true);
							readAccessRightsFieldReferences.add(nextMappedField);
						} /*else if (nextAccessRight.type().equalsIgnoreCase("deleteQuery")) {
							if (nextAccessRight.query().indexOf("jpql:") >= 0)
								nextMappedField.deleteQuery = new JpqlQuery();
							else
								nextMappedField.deleteQuery = new MappedQuery();
							nextMappedField.deleteQuery.setQuery(nextAccessRight.query());
						}*/

						// Add to queries list.
						IMappedQuery nextQuery = null;
						if (nextAccessRight.query().indexOf("jpql:") >= 0)
							nextQuery = new JpqlQuery();
						else
							nextQuery = new MappedQuery();
						nextQuery.setQuery(nextAccessRight.query());
						nextQuery.setQueryName(nextAccessRight.type());
						
						nextMappedField.queries.add(nextQuery);
					}
				}
				
				// Check to see if this has any PropertyInterfaces that need to be addresed.
				PropertyInterface propInterface = (PropertyInterface) nextField.getAnnotation(PropertyInterface.class);
				if (propInterface != null) {
					processMappedFieldPropertyInterface(propInterface, nextMappedField);
				}
				PropertyInterfaces propertyInterfaces = (PropertyInterfaces) nextField.getAnnotation(PropertyInterfaces.class);
				if (propertyInterfaces != null) {
					for(PropertyInterface nextPropInterface : propertyInterfaces.propertyInterfaces()) {
						processMappedFieldPropertyInterface(nextPropInterface, nextMappedField);
					}
				}
			}
		} catch(Exception e) {
			logger.error("Error parsing MappedClass " + this.className, e);
		}
		fieldsInitialized = true;
	}
	
	private void processMappedFieldPropertyInterface(PropertyInterface propInterface, MappedField mappedField) {
		if (propInterface == null || !StringUtils.hasText(propInterface.propertyName())) {
			logger.warn("Invalid PropertyInterface for " + className + "." + mappedField.getField().getName());
			return;
		}
		
		EntityImplementation entityImpl = entityImplementations.get(propInterface.entityInterfaceClass());
		if (entityImpl != null) {
			Iterator<PropertyImplementation> itrPropImpls = entityImpl.propertyImplementations.iterator();
			while (itrPropImpls.hasNext()) {
				PropertyImplementation nextPropImpl = itrPropImpls.next();
				if (nextPropImpl.propertyName.equals(propInterface.propertyName())) {
					// Found property implementation.
					nextPropImpl.mappedField = mappedField;
					return;
				}
			}
			
			// No valid PropertyIntrerface found so create one.
			PropertyImplementation propImpl = new PropertyImplementation();
			propImpl.entityImplementation = entityImpl;
			propImpl.propertyName = propInterface.propertyName();
			propImpl.mappedField = mappedField;
			entityImpl.propertyImplementations.add(propImpl);
						
			for(PropertyInterfaceParam nextPropInterfaceParam : propInterface.params()) {
				PropertyImplementationParam param = new PropertyImplementationParam();
				param.propertyImplementation = propImpl;
				param.name = nextPropInterfaceParam.name();
				param.value = nextPropInterfaceParam.value();
				propImpl.params.add(param);
			}
		}
		else {
			// Need to crearte EntityImplementation as well.
			entityImpl = new EntityImplementation();
			entityImpl.entityInterfaceClass = propInterface.entityInterfaceClass();
			entityImpl.mappedClass = this;
			
			PropertyImplementation propImpl = new PropertyImplementation();
			propImpl.entityImplementation = entityImpl;
			propImpl.propertyName = propInterface.propertyName();
			propImpl.mappedField = mappedField;
			entityImpl.propertyImplementations.add(propImpl);
						
			for(PropertyInterfaceParam nextPropInterfaceParam : propInterface.params()) {
				PropertyImplementationParam param = new PropertyImplementationParam();
				param.propertyImplementation = propImpl;
				param.name = nextPropInterfaceParam.name();
				param.value = nextPropInterfaceParam.value();
				propImpl.params.add(param);
			}
		}
	}
		
	@SuppressWarnings("rawtypes")
	public void initializeQueries() {
		if (queriesInitialized) {
			return;
		}
		IMappedClassManager mcm = MappedClassManagerFactory.getMappedClassManager();
		try {
			Class clazz = MappedClass.forName(className);
			Class tempClazz = clazz;
			while(!tempClazz.equals(Object.class)) {
				// Get Entity name, if exists.
				if (!StringUtils.hasText(tableName)) {
					Entity entity = (Entity) tempClazz.getAnnotation(Entity.class);
					if (entity != null) {
						tableName = entity.name();
					}
				}
				
				if (!StringUtils.hasText(dataProviderName)) {
					DataProvider dataProvider = (DataProvider) tempClazz.getAnnotation(DataProvider.class);
					if (dataProvider != null) {
						dataProviderName = dataProvider.name();
					}
				}
				
				// Get native queries for handling AccessRights
				/**
				 * readAllQuery
				 * Added by Jonathan Samples because initial download for certain entities was disasterously
				 * slow using the old style
				 */
				PerceroNamedNativeQueries nativeQueries = (PerceroNamedNativeQueries) tempClazz.getAnnotation(PerceroNamedNativeQueries.class);
				if(nativeQueries != null){
					for(PerceroNamedNativeQuery q : nativeQueries.value()){
						if (q.name().equalsIgnoreCase("readAllQuery")) {
							logger.debug("Adding readAllQuery to mappedClass: "+className);
							readAllQuery = new SqlQuery(q.query());
						}
					}
				}
				
				// Get NamedQueries for handling Access Rights.
				NamedQueries namedQueries = (NamedQueries) tempClazz.getAnnotation(NamedQueries.class);
				if (namedQueries != null) {
					for(NamedQuery nextNamedQuery : namedQueries.value()) {
						
						if (nextNamedQuery.name().equalsIgnoreCase("createQuery")) {
							createQuery = QueryFactory.createQuery(nextNamedQuery.query());
						} else if (nextNamedQuery.name().equalsIgnoreCase("updateQuery")) {
							updateQuery = QueryFactory.createQuery(nextNamedQuery.query());
						} else if (nextNamedQuery.name().equalsIgnoreCase("readQuery")) {
							readQuery = QueryFactory.createQuery(nextNamedQuery.query());
							
							Iterator<MappedField> itrToOneFields = toOneFields.iterator();
							while(itrToOneFields.hasNext()) {
								MappedField nextMappedField = itrToOneFields.next();
								MappedClass referencedMappedClass = mcm.getMappedClassByClassName(nextMappedField.getField().getType().getCanonicalName());
								// Need to find the corresponding field.
								for(MappedField nextRefMappedField : referencedMappedClass.toManyFields) {
									if (nextRefMappedField instanceof MappedFieldList) {
										OneToMany refOneToMany = nextRefMappedField.getField().getAnnotation(OneToMany.class);
										if (refOneToMany == null)
											refOneToMany = nextRefMappedField.getGetter().getAnnotation(OneToMany.class);
										
										if (refOneToMany != null && refOneToMany.targetEntity().getCanonicalName().equals(this.className) && nextMappedField.getField().getName().equals(refOneToMany.mappedBy())) {
											// Found the referenced field.
											nextRefMappedField.setHasReadAccessRights(true);
											referencedMappedClass.readAccessRightsFieldReferences.add(nextRefMappedField);
											break;
										}
									}
									else if (nextRefMappedField instanceof MappedFieldPerceroObject) {
										OneToOne refOneToOne = nextRefMappedField.getField().getAnnotation(OneToOne.class);
										if (refOneToOne == null)
											refOneToOne = nextRefMappedField.getGetter().getAnnotation(OneToOne.class);
										
										if (refOneToOne != null && refOneToOne.targetEntity().getCanonicalName().equals(this.className) && nextMappedField.getField().getName().equals(refOneToOne.mappedBy())) {
											// Found the referenced field.
											nextRefMappedField.setHasReadAccessRights(true);
											referencedMappedClass.readAccessRightsFieldReferences.add(nextRefMappedField);
											break;
										}
									}
								}
							}
							
						} else if (nextNamedQuery.name().equalsIgnoreCase("deleteQuery")) {
							deleteQuery  = QueryFactory.createQuery(nextNamedQuery.query());
						}

						// Add to queries list.
						IMappedQuery nextQuery = QueryFactory.createQuery(nextNamedQuery.query());
						nextQuery.setQueryName(nextNamedQuery.name());
						
						queries.add(nextQuery);
					}
				}

				Table table = (Table) tempClazz.getAnnotation(Table.class);
				
				if (table != null) {
					for(UniqueConstraint nextUniqueConstraint : table.uniqueConstraints()) {
						// TODO: Add an Array of MappedFields instead of a UniqueConstraint.
						List<MappedField> listMappedFields = new ArrayList<MappedField>();
						for(String nextColumnName : nextUniqueConstraint.columnNames()) {
							Iterator<MappedField> itrAllMappedFields = externalizableFields.iterator();
							while(itrAllMappedFields.hasNext()) {
								MappedField nextMappedField = itrAllMappedFields.next();
								if (nextColumnName.equalsIgnoreCase(nextMappedField.getColumnName())) {
									listMappedFields.add(nextMappedField);
									break;
								}
							}
						}
						uniqueConstraints.add(listMappedFields);
					}
				}
				
				tempClazz = tempClazz.getSuperclass();
			}
			
			
			if (tableName == null || tableName.isEmpty()) {
				tableName = ClassUtils.getShortName(clazz);
			}
		} catch(Exception e) {
			logger.error("Error parsing MappedClass " + this.className, e);
		}
		
		queriesInitialized = true;
	}
	
	public void initializeRelationships() {
		if (relationshipsInitialized) {
			return;
		}
		IMappedClassManager mcm = MappedClassManagerFactory.getMappedClassManager();
		try {
			
			//	NOTE: These sections have to be done after the code above so that we know all MappedClasses in the toManyFields list have been initialized
			Iterator<MappedField> itrExternalizeFields = externalizableFields.iterator();
			while(itrExternalizeFields.hasNext()) {
				MappedField nextMappedField = itrExternalizeFields.next();

				OneToMany oneToMany = (OneToMany) nextMappedField.getGetter().getAnnotation(OneToMany.class);
				if (oneToMany == null)
					oneToMany = (OneToMany) nextMappedField.getField().getAnnotation(OneToMany.class);
				
				if (oneToMany != null) {
					//toManyFields.add(nextMappedField);
					ParameterizedType listType = (ParameterizedType) nextMappedField.getField().getGenericType();
					Class<?> listClass = (Class<?>) listType.getActualTypeArguments()[0];
					MappedClass referencedMappedClass = mcm.getMappedClassByClassName(listClass.getCanonicalName());
					if (!referencedMappedClass.fieldsInitialized) {
						referencedMappedClass.initializeFields();
					}
					if (referencedMappedClass.getReadQuery() != null && StringUtils.hasText(referencedMappedClass.getReadQuery().getQuery())) {
						nextMappedField.setHasReadAccessRights(true);
						readAccessRightsFieldReferences.add(nextMappedField);
					}
				}

				ManyToOne manyToOne = (ManyToOne) nextMappedField.getGetter().getAnnotation(ManyToOne.class);
				if (manyToOne == null)
					manyToOne = (ManyToOne) nextMappedField.getField().getAnnotation(ManyToOne.class);
				
				OneToOne oneToOne = (OneToOne) nextMappedField.getGetter().getAnnotation(OneToOne.class);
				if (oneToOne == null)
					oneToOne = (OneToOne) nextMappedField.getField().getAnnotation(OneToOne.class);
				
				if (manyToOne != null && !manyToOne.optional()
						||
						oneToOne != null && !oneToOne.optional()) {
					requiredFields.add(nextMappedField);
					
					MappedClass referencedMappedClass = mcm.getMappedClassByClassName(nextMappedField.getField().getType().getName());
					if (!referencedMappedClass.fieldsInitialized) {
						referencedMappedClass.initializeFields();
					}
					
					if (referencedMappedClass.getReadQuery() != null && StringUtils.hasText(referencedMappedClass.getReadQuery().getQuery())) {
						nextMappedField.setHasReadAccessRights(true);
						readAccessRightsFieldReferences.add(nextMappedField);
					}
					
					MappedField reverseMappedField = null;
					
					// Find the reverse field.
					for(MappedField nextRefMappedField : referencedMappedClass.toManyFields) {
						if (nextRefMappedField instanceof MappedFieldList) {
							OneToMany refOneToMany = nextRefMappedField.getField().getAnnotation(OneToMany.class);
							if (refOneToMany == null)
								refOneToMany = nextRefMappedField.getGetter().getAnnotation(OneToMany.class);
							
							if (refOneToMany != null) {
								
								Boolean inheritsFrom = inheritsFrom(this.clazz, refOneToMany.targetEntity());
								if (inheritsFrom && nextMappedField.getField().getName().equals(refOneToMany.mappedBy())) {
									// Found the referenced field.
									reverseMappedField = nextRefMappedField;
									break;
								}
							}
						}
						else if (nextRefMappedField instanceof MappedFieldPerceroObject) {
							OneToOne refOneToOne = nextRefMappedField.getField().getAnnotation(OneToOne.class);
							if (refOneToOne == null)
								refOneToOne = nextRefMappedField.getGetter().getAnnotation(OneToOne.class);
							
							if (refOneToOne != null) {
								Boolean inheritsFrom = inheritsFrom(this.clazz, refOneToOne.targetEntity());
								if (inheritsFrom && nextMappedField.getField().getName().equals(refOneToOne.mappedBy())) {
									// Found the referenced field.
									reverseMappedField = nextRefMappedField;
									break;
								}
							}
						}
					}
					
					if (reverseMappedField == null) {
						// Find the reverse field.
						for(MappedField nextRefMappedField : referencedMappedClass.toOneFields) {
							if (nextRefMappedField instanceof MappedFieldPerceroObject) {
								OneToOne refOneToOne = nextRefMappedField.getField().getAnnotation(OneToOne.class);
								if (refOneToOne == null)
									refOneToOne = nextRefMappedField.getGetter().getAnnotation(OneToOne.class);
								
								if (refOneToOne != null) {
									Boolean inheritsFrom = inheritsFrom(this.clazz, nextRefMappedField.getField().getType());
									if (inheritsFrom && nextMappedField.getField().getName().equals(refOneToOne.mappedBy())) {
										// Found the referenced field.
										reverseMappedField = nextRefMappedField;
										break;
									}
								}
							}
						}
					}
					
					if (!referencedMappedClass.cascadeRemoveFieldReferences.keySet().contains(nextMappedField)) {
						if (reverseMappedField == null) {
							if (manyToOne != null) {
								if (!manyToOne.targetEntity().getName().equalsIgnoreCase("void")) {
									System.out.println("IS THIS CORRECT?");
								}
							}
							if (oneToOne != null) {
								if (!oneToOne.targetEntity().getName().equalsIgnoreCase("void")) {
									System.out.println("IS THIS CORRECT?");
								}
							}
						}
						referencedMappedClass.cascadeRemoveFieldReferences.put(nextMappedField, reverseMappedField);
					}
					
					nextMappedField.setReverseMappedField(reverseMappedField);

				} else if (manyToOne != null && manyToOne.optional()
						||
						oneToOne != null && oneToOne.optional() && (oneToOne.mappedBy() == null || oneToOne.mappedBy().isEmpty())
						) {
					MappedClass referencedMappedClass = mcm.getMappedClassByClassName(nextMappedField.getField().getType().getName());
					
					if (referencedMappedClass.getReadQuery() != null && StringUtils.hasText(referencedMappedClass.getReadQuery().getQuery()))
							nextMappedField.setHasReadAccessRights(true);
					
					MappedField reverseMappedField = null;
					
					// Find the reverse field.
					for(MappedField nextRefMappedField : referencedMappedClass.toManyFields) {
						if (nextRefMappedField instanceof MappedFieldList) {
							OneToMany refOneToMany = nextRefMappedField.getField().getAnnotation(OneToMany.class);
							if (refOneToMany == null)
								refOneToMany = nextRefMappedField.getGetter().getAnnotation(OneToMany.class);
							
							if (refOneToMany != null) {
								Boolean inheritsFrom = inheritsFrom(this.clazz, refOneToMany.targetEntity());
								if (inheritsFrom && nextMappedField.getField().getName().equals(refOneToMany.mappedBy())) {
									// Found the referenced field.
									reverseMappedField = nextRefMappedField;
									break;
								}
							}
						}
						else if (nextRefMappedField instanceof MappedFieldPerceroObject) {
							OneToOne refOneToOne = nextRefMappedField.getField().getAnnotation(OneToOne.class);
							if (refOneToOne == null)
								refOneToOne = nextRefMappedField.getGetter().getAnnotation(OneToOne.class);
							
							if (refOneToOne != null) {
								Boolean inheritsFrom = inheritsFrom(this.clazz, refOneToOne.targetEntity());
								if (inheritsFrom && nextMappedField.getField().getName().equals(refOneToOne.mappedBy())) {
									// Found the referenced field.
									reverseMappedField = nextRefMappedField;
									break;
								}
							}
						}
					}
					
					if (reverseMappedField == null) {
						// Find the reverse field.
						for(MappedField nextRefMappedField : referencedMappedClass.toOneFields) {
							if (nextRefMappedField instanceof MappedFieldPerceroObject) {
								OneToOne refOneToOne = nextRefMappedField.getField().getAnnotation(OneToOne.class);
								if (refOneToOne == null)
									refOneToOne = nextRefMappedField.getGetter().getAnnotation(OneToOne.class);
								
								if (refOneToOne != null) {
									Boolean inheritsFrom = inheritsFrom(this.clazz, nextRefMappedField.getField().getType());
									if (inheritsFrom && nextMappedField.getField().getName().equals(refOneToOne.mappedBy())) {
										// Found the referenced field.
										reverseMappedField = nextRefMappedField;
										break;
									}
								}
							}
						}
					}
						
					if (!referencedMappedClass.nulledOnRemoveFieldReferences.keySet().contains(nextMappedField)) {
						if (reverseMappedField == null) {
							if (manyToOne != null) {
								if (!manyToOne.targetEntity().getName().equalsIgnoreCase("void")) {
									System.out.println("IS THIS CORRECT?");
								}
							}
							if (oneToOne != null) {
								if (!oneToOne.targetEntity().getName().equalsIgnoreCase("void")) {
									System.out.println("IS THIS CORRECT?");
								}
							}
						}
						referencedMappedClass.nulledOnRemoveFieldReferences.put(nextMappedField, reverseMappedField);
						readAccessRightsFieldReferences.add(nextMappedField);
					}

					nextMappedField.setReverseMappedField(reverseMappedField);
				}
				
				// Check to see if this has any RelationshipInterfaces that need to be addresed.
				RelationshipInterface propInterface = (RelationshipInterface) nextMappedField.getField().getAnnotation(RelationshipInterface.class);
				if (propInterface != null) {
					processMappedFieldRelationshipInterface(propInterface, nextMappedField);
				}
				RelationshipInterfaces relationshipInterfaces = (RelationshipInterfaces) nextMappedField.getField().getAnnotation(RelationshipInterfaces.class);
				if (relationshipInterfaces != null) {
					for(RelationshipInterface nextPropInterface : relationshipInterfaces.relationshipInterfaces()) {
						processMappedFieldRelationshipInterface(nextPropInterface, nextMappedField);
					}
				}
			}
			
			// Now check Read AccessRights on all toMany fields.
			Iterator<MappedField> itrToManyFields = toManyFields.iterator();
			while(itrToManyFields.hasNext()) {
				MappedField nextMappedField = itrToManyFields.next();
				ParameterizedType listType = (ParameterizedType) nextMappedField.getField().getGenericType();
				Class<?> listClass = (Class<?>) listType.getActualTypeArguments()[0];
				MappedClass referencedMappedClass = mcm.getMappedClassByClassName(listClass.getCanonicalName());
				if (referencedMappedClass.getReadQuery() != null && StringUtils.hasText(referencedMappedClass.getReadQuery().getQuery())) {
					nextMappedField.setHasReadAccessRights(true);
					readAccessRightsFieldReferences.add(nextMappedField);
				}
			}
		} catch(Exception e) {
			logger.error("Error parsing MappedClass " + this.className, e);
		}
		
		relationshipsInitialized = true;
	}
	
	private void processMappedFieldRelationshipInterface(RelationshipInterface relInterface, MappedField mappedField) {
		if (relInterface == null || !StringUtils.hasText(relInterface.sourceVarName())) {
			logger.warn("Invalid RelationshipInterface for " + className + "." + mappedField.getField().getName());
			return;
		}
		
		EntityImplementation entityImpl = entityImplementations.get(relInterface.entityInterfaceClass());
		if (entityImpl != null) {
			Iterator<RelationshipImplementation> itrRelImpls = entityImpl.relationshipImplementations.iterator();
			while (itrRelImpls.hasNext()) {
				RelationshipImplementation nextRelImpl = itrRelImpls.next();
				if (nextRelImpl.sourceVarName.equals(relInterface.sourceVarName())) {
					// Found relationship implementation.
					nextRelImpl.sourceMappedField = mappedField;
					return;
				}
			}
			
			// No valid RelationshipIntrerface found so create one.
			RelationshipImplementation relImpl = new RelationshipImplementation();
			relImpl.entityImplementation = entityImpl;
			relImpl.sourceVarName = relInterface.sourceVarName();
			relImpl.sourceMappedField = mappedField;
			entityImpl.relationshipImplementations.add(relImpl);
		}
		else {
			// Need to crearte EntityImplementation as well.
			entityImpl = new EntityImplementation();
			entityImpl.entityInterfaceClass = relInterface.entityInterfaceClass();
			entityImpl.mappedClass = this;
			
			RelationshipImplementation relImpl = new RelationshipImplementation();
			relImpl.entityImplementation = entityImpl;
			relImpl.sourceVarName = relInterface.sourceVarName();
			relImpl.sourceMappedField = mappedField;
			entityImpl.relationshipImplementations.add(relImpl);
		}

		/**
		// Find the existing RelationshipImplementation and set its mappedField.
		Iterator<Class> itrClasses = entityImplementations.keySet().iterator();
		while (itrClasses.hasNext()) {
			Class nextInterfaceClazz = itrClasses.next();
			Iterator<EntityImplementation> itrEntityImpls = entityImplementations.get(nextInterfaceClazz).iterator();
			while (itrEntityImpls.hasNext()) {
				EntityImplementation nextEntityImpl = itrEntityImpls.next();
				Iterator<RelationshipImplementation> itrRelImpls = nextEntityImpl.relationshipImplementations.iterator();
				while (itrRelImpls.hasNext()) {
					RelationshipImplementation nextRelImpl = itrRelImpls.next();
					if (nextRelImpl.sourceVarName.equals(relInterface.sourceVarName())) {
						// Found relationship implementation.
						nextRelImpl.sourceMappedField = mappedField;
						return;
					}
				}
			}
		}
		
		// No corresponding Relationship Implementation.
		logger.warn("No RelationshipImplementation found on " + className + "." + mappedField.getField().getName() + " of type " + relInterface.sourceVarName());*/
	}
	
	@SuppressWarnings("rawtypes")
	private boolean inheritsFrom(Class a, Class b) {
		if (a.equals(b))
			return true;
		
		Class s = a.getSuperclass();
		while(true) {
			if (s != null && s.equals(b))
				return true;
			else if (s == null || s.equals(Object.class))
				return false;
			else
				s = s.getSuperclass();
		}
	}
	
	@SuppressWarnings("rawtypes")
	private boolean implementsInterface(Class a, Class b) {
		if (a.equals(b))
			return true;
		
		Class[] interfaces = a.getInterfaces();
		
		for(Class nextInterface : interfaces) {
			if (nextInterface.equals(b))
				return true;
		}
		
		Class s = a.getSuperclass();
		if (s != null)
			return implementsInterface(s, b);
		
		return false;
	}
	
	@SuppressWarnings("rawtypes")
	public MappedClass getSuperMappedClass() {
		IMappedClassManager mcm = MappedClassManagerFactory.getMappedClassManager();
		
		try {
			Class clazz = MappedClass.forName(className);
			MappedClass superMappedClass = mcm.getMappedClassByClassName(clazz.getSuperclass().getName());
			return superMappedClass;
		} catch(Exception e) {
			return null;
		}
	}
	
	public MappedField getMappedFieldByName(String fieldName) {
		MappedField result = null;
		for(MappedField nextMappedField : externalizableFields) {
			if (nextMappedField.getField().getName().equals(fieldName)) {
				result = nextMappedField;
				break;
			}
		}
			
		return result;
	}
	

	public boolean isFieldRequired(String fieldName) {
		Iterator<MappedField> itr = requiredFields.iterator();
		while (itr.hasNext()) {
			MappedField nextReqField = itr.next();
			if (nextReqField.getField().getName().equals(fieldName))
				return true;
		}
		return false;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		else if (obj.getClass() != this.getClass())
			return false;
		else {
			MappedClass mcObj = (MappedClass) obj;
			if ((this.className == null || this.className.trim().length() == 0) && (mcObj.className == null || mcObj.className.trim().length() == 0))
				return true;
			else if ((this.className == null || this.className.trim().length() == 0) || (mcObj.className == null || mcObj.className.trim().length() == 0))
				return false;
			else
				return this.className.equals(mcObj.className);
		}
	}
	
	/***************************************
	 * Static Helper Methods
	 ***************************************/
	@SuppressWarnings("rawtypes")
	private static Map<String, Class> CLASS_MAP = Collections.synchronizedMap(new HashMap<String, Class>());
	@SuppressWarnings("rawtypes")
	public static Class forName(String className) throws ClassNotFoundException {
		Class clazz = CLASS_MAP.get(className);
		if (clazz == null) {
			clazz = Class.forName(className);
			if (clazz != null) {
				CLASS_MAP.put(className, clazz);
			}
		}
		
		return clazz;
	}

	private static IMappedClassManager mcm = null;
	@SuppressWarnings("rawtypes")
	public static MappedClassMethodPair getFieldSetters(Class theClass, String theFieldName) {
		Method theMethod = null;
		Method[] theMethods = theClass.getMethods();
		String theModifiedFieldName = theFieldName;
		if (theModifiedFieldName.indexOf("_") == 0)
			theModifiedFieldName = theModifiedFieldName.substring(1);
		
		for(Method nextMethod : theMethods) {
			if (nextMethod.getName().equalsIgnoreCase("set" + theModifiedFieldName)) {
				theMethod = nextMethod;
				break;
			}
		}
		
		if (mcm == null) {
			mcm = MappedClassManagerFactory.getMappedClassManager();
		}
		MappedClass mc = mcm.getMappedClassByClassName(theClass.getCanonicalName());

		MappedClassMethodPair result = new MappedClassMethodPair(mc, theMethod);
		return result;
	}

	public static class MappedClassMethodPair {
		public Method method;
		public MappedClass mappedClass;
		
		public MappedClassMethodPair(MappedClass mappedClass, Method method) {
			this.mappedClass = mappedClass;
			this.method = method;
		}
	}

}
