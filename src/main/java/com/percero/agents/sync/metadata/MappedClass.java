package com.percero.agents.sync.metadata;

import java.lang.annotation.Annotation;
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
import javax.persistence.JoinColumn;
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
import com.percero.agents.sync.services.DataProviderManager;
import com.percero.agents.sync.services.IDataProvider;
import com.percero.agents.sync.vo.BaseDataObject;
import com.percero.agents.sync.vo.ClassIDPair;
import com.percero.framework.bl.IManifest;
import com.percero.framework.bl.ManifestHelper;
import com.percero.framework.metadata.IMappedClass;
import com.percero.framework.metadata.IMappedQuery;
import com.percero.framework.vo.IPerceroObject;
import com.percero.util.MappedClassUtils;

@SuppressWarnings("unchecked")
public class MappedClass implements IMappedClass {

	private static Logger logger = Logger.getLogger(MappedClass.class);

	public static Map<Class<?>, List<EntityImplementation>> entityInterfacesMappedClasses = Collections
			.synchronizedMap(new HashMap<Class<?>, List<EntityImplementation>>());

	public static Boolean allMappedClassesInitialized = false;

	@SuppressWarnings("rawtypes")
	public static void processManifest(IManifest manifest) {
		if (!allMappedClassesInitialized) {
			try {
				IMappedClassManager mcm = MappedClassManagerFactory
						.getMappedClassManager();
				Set<MappedClass> mappedClasses = new HashSet<MappedClass>();
				ManifestHelper.setManifest(manifest);
				Iterator<String> itrUuidMap = manifest.getUuidMap().keySet()
						.iterator();
				while (itrUuidMap.hasNext()) {
					String nextUuid = itrUuidMap.next();
					Class nextClass = manifest.getUuidMap().get(nextUuid);

					MappedClass mc = mcm.getMappedClassByClassName(nextClass
							.getCanonicalName());
					mappedClasses.add(mc);
				}

				// Initialize the Fields
				Iterator<MappedClass> itrMappedClasses = mappedClasses
						.iterator();
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
							MappedClass nextMc = mcm
									.getMappedClassByClassName(clazz
											.getCanonicalName());
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

			} catch (Exception e) {
				logger.error("Unable to process manifest.", e);
			}
		}
	}

	@SuppressWarnings("rawtypes")
	public static List<EntityImplementation> findEntityImplementation(
			Class interfaceClazz) {
		return entityInterfacesMappedClasses.get(interfaceClazz);
	}

	private static Comparator<MappedField> fieldComparator;

	static {
		fieldComparator = new Comparator<MappedField>() {
			public int compare(MappedField o1, MappedField o2) {
				return o1.getField().getName()
						.compareToIgnoreCase(o2.getField().getName());
			}
		};
	}

	public int ID = 0;
	public Boolean needsReadCleaning = false;

	public Boolean getNeedsReadCleaning() {
		if (getReadAccessRightsFieldReferences() != null
				&& !getReadAccessRightsFieldReferences().isEmpty()) {
			return true;
		} else if (getReadQuery() != null) {
			return true;
		} else if (getReadAllQuery() != null) {
			return true;
		} else {
			return false;
		}
	}

	public Boolean fieldsInitialized = false;
	public Boolean queriesInitialized = false;
	public Boolean relationshipsInitialized = false;
	// private Boolean isInitializing = false;

	public String dataProviderName = "";

	public String getDataProviderName() {
		return dataProviderName;
	}

	public void setDataProviderName(String dataProviderName) {
		this.dataProviderName = dataProviderName;

		// Reset the DataProvider
		if (dataProvider != null) {
			dataProvider = null;
		}
	}

	public String className = "";
	public String tableName = "";
	public String tableSchema = "";
	public MappedField idMappedField = null;
	public List<MappedField> requiredFields = Collections
			.synchronizedList(new ArrayList<MappedField>());

	public Set<MappedField> toManyFields = Collections
			.synchronizedSet(new HashSet<MappedField>());
	public Set<MappedFieldPerceroObject> toOneFields = Collections
			.synchronizedSet(new HashSet<MappedFieldPerceroObject>());

	private Set<MappedFieldPerceroObject> sourceMappedFields = Collections
			.synchronizedSet(new HashSet<MappedFieldPerceroObject>());

	public Set<MappedFieldPerceroObject> getSourceMappedFields() {
		return sourceMappedFields;
	}

	private Set<MappedField> targetMappedFields = Collections
			.synchronizedSet(new HashSet<MappedField>());

	public Set<MappedField> getTargetMappedFields() {
		return targetMappedFields;
	}

	public Set<MappedField> propertyFields = Collections
			.synchronizedSet(new HashSet<MappedField>());
	public List<Field> entityFields = Collections
			.synchronizedList(new ArrayList<Field>());
	public List<Field> mapFields = Collections
			.synchronizedList(new ArrayList<Field>());
	public List<Field> listFields = Collections
			.synchronizedList(new ArrayList<Field>());
	public List<MappedField> nonLazyLoadingFields = Collections
			.synchronizedList(new ArrayList<MappedField>());
	public Set<MappedFieldPerceroObject> externalizablePerceroObjectFields = Collections
			.synchronizedSet(new HashSet<MappedFieldPerceroObject>());
	public Set<MappedField> externalizableFields = Collections
			.synchronizedSet(new TreeSet<MappedField>(fieldComparator));
	private Map<MappedField, MappedField> cascadeRemoveFieldReferences = Collections
			.synchronizedMap(new HashMap<MappedField, MappedField>());
	private Map<MappedField, MappedField> nulledOnRemoveFieldReferences = Collections
			.synchronizedMap(new HashMap<MappedField, MappedField>());
	@SuppressWarnings("rawtypes")
	public Map<Class, EntityImplementation> entityImplementations = Collections
			.synchronizedMap(new HashMap<Class, EntityImplementation>());
	public MappedClass parentMappedClass = null;
	public List<MappedClass> childMappedClasses = Collections
			.synchronizedList(new ArrayList<MappedClass>());

	public List<MappedClass> getChildMappedClasses() {
		return this.childMappedClasses;
	}

	IDataProvider dataProvider = null;

	public IDataProvider getDataProvider() {
		if (dataProvider == null) {
			dataProvider = DataProviderManager.getInstance()
					.getDataProviderByName(dataProviderName);
		}
		return dataProvider;
	}

	/**
	 * readAccessRightsFieldReferences holds all MappedFields that have some
	 * sort of readAccessRights associated with that field. This means that this
	 * field needs to be recalculated for each User. If a MappedClass has NO
	 * fields in this list AND NO readQuery, then all objects of that type do
	 * NOT need to be recalculated for each User.
	 */
	public Set<MappedField> readAccessRightsFieldReferences = new HashSet<MappedField>();

	public Set<MappedField> getReadAccessRightsFieldReferences() {
		return readAccessRightsFieldReferences;
	}

	public List<List<MappedField>> uniqueConstraints = new ArrayList<List<MappedField>>();
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
		} catch (Exception e) {
			logger.error("Unable to instantiate class " + className, e);
		}

		MappedClassManagerFactory.getMappedClassManager();

		// Look for EntityInterfaces
		Class nextClazz = clazz;
		while (nextClazz != null) {
			EntityInterface entityInterface = (EntityInterface) nextClazz
					.getAnnotation(EntityInterface.class);
			if (entityInterface != null) {
				if (entityInterface.interfaceClass() != null) {
					List<EntityImplementation> allEntityImplementations = MappedClass.entityInterfacesMappedClasses
							.get(entityInterface.interfaceClass());
					if (allEntityImplementations == null) {
						allEntityImplementations = new ArrayList<EntityImplementation>();
						MappedClass.entityInterfacesMappedClasses.put(
								entityInterface.interfaceClass(),
								allEntityImplementations);
					}

					EntityImplementation entityImpl = processEntityInterface(entityInterface);
					allEntityImplementations.add(entityImpl);
				}
			}

			EntityInterfaces entityInterfaces = (EntityInterfaces) nextClazz
					.getAnnotation(EntityInterfaces.class);
			if (entityInterfaces != null) {
				for (EntityInterface nextEntityInterface : entityInterfaces
						.entityInterfaces()) {
					if (nextEntityInterface.interfaceClass() != null) {
						List<EntityImplementation> allEntityImplementations = MappedClass.entityInterfacesMappedClasses
								.get(nextEntityInterface.interfaceClass());
						if (allEntityImplementations == null) {
							allEntityImplementations = new ArrayList<EntityImplementation>();
							MappedClass.entityInterfacesMappedClasses.put(
									nextEntityInterface.interfaceClass(),
									allEntityImplementations);
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

	protected EntityImplementation processEntityInterface(
			EntityInterface entityInterface) {
		if (entityInterface == null || entityInterface.interfaceClass() == null) {
			logger.warn("Invalid EntityInterface on class " + className);
			return null;
		}

		// Check to see if interface has already been processed.
		EntityImplementation entityImpl = entityImplementations
				.get(entityInterface.interfaceClass());
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
	protected MappedField handleCreateMappedFieldFromClass(Class fieldClass) {
		MappedField newMappedField = null;
		if (fieldClass == int.class)
			newMappedField = new MappedFieldInt();
		else if (fieldClass == Integer.class)
			newMappedField = new MappedFieldInteger();
		else if (fieldClass == float.class)
			newMappedField = new MappedFieldFloat();
		else if (fieldClass == Float.class)
			newMappedField = new MappedFieldFloat();
		else if (fieldClass == double.class)
			newMappedField = new MappedFieldDouble();
		else if (fieldClass == Double.class)
			newMappedField = new MappedFieldDouble();
		else if (fieldClass == boolean.class)
			newMappedField = new MappedFieldBool();
		else if (fieldClass == Boolean.class)
			newMappedField = new MappedFieldBoolean();
		else if (fieldClass == String.class)
			newMappedField = new MappedFieldString();
		else if (fieldClass == Date.class)
			newMappedField = new MappedFieldDate();
		/**
		 * else if (nextFieldClass == Map.class) nextMappedField = new
		 * MappedFieldMap(); else if (nextFieldClass == List.class)
		 * nextMappedField = new MappedFieldList();
		 **/
		else if (implementsInterface(fieldClass, IPerceroObject.class)) {
			newMappedField = new MappedFieldPerceroObject();
			externalizablePerceroObjectFields.add((MappedFieldPerceroObject) newMappedField);
		}
		else if (implementsInterface(fieldClass, Map.class))
			newMappedField = new MappedFieldMap();
		else if (implementsInterface(fieldClass, List.class))
			newMappedField = new MappedFieldList();

		else
			newMappedField = new MappedField();
		
		return newMappedField;
	}

	public void initializeFields() {
		if (fieldsInitialized) {
			return;
		}

		try {
			if (clazz == null) {
				clazz = MappedClass.forName(className);
			}

			List<Field> fields = MappedClassUtils.getClassFields(clazz);
			for (Field nextField : fields) {
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

				Method theGetter = MappedClassUtils.getFieldGetters(clazz, nextField);

				// Ignore this field if marked as Transient.
				if (hasAnnotation(nextField, theGetter, Transient.class))
					continue;

				Externalize externalize = retrieveAnnotation(nextField, theGetter, Externalize.class);
				if (externalize == null) {
					// Only process Externalizeable fields.
					continue;
				}

				Method theSetter = MappedClassUtils.getFieldSetters(clazz, nextField);

				MappedField nextMappedField = handleCreateMappedFieldFromClass(nextField.getType());
				nextMappedField.setMappedClass(this);
				nextMappedField.setField(nextField);
				nextMappedField.setGetter(theGetter);
				nextMappedField.setSetter(theSetter);
				externalizableFields.add(nextMappedField);

				nextMappedField.setUseLazyLoading(externalize.useLazyLoading());
				if (!nextMappedField.getUseLazyLoading())
					this.hasNonLazyLoadProperties = true;

				if (!externalize.useLazyLoading())
					nonLazyLoadingFields.add(nextMappedField);

				handleAnnotation_OneToMany(nextField, theGetter, nextMappedField);
				handleAnnotation_ManyToOne(nextField, theGetter, nextMappedField);
				handleAnnotation_OneToOne(nextField, theGetter, nextMappedField);

				Boolean isPropertyField = true;
				if (nextField.getType().isAnnotationPresent(Entity.class)) {
//				if (hasAnnotation(nextField.getType(), null, Entity.class)) {
					entityFields.add(nextField);
					toOneFields.add((MappedFieldPerceroObject) nextMappedField);
					isPropertyField = false;
				}

				if (inheritsFrom(nextField.getType(), Map.class)) {
					mapFields.add(nextField);
					toManyFields.add(nextMappedField);
					isPropertyField = false;
				}

				if (inheritsFrom(nextField.getType(), List.class)) {
					listFields.add(nextField);
					toManyFields.add(nextMappedField);
					isPropertyField = false;
				}

				if (isPropertyField) {
					propertyFields.add(nextMappedField);
				}

				handleAnnotation_Id(nextField, theGetter, nextMappedField);
				handleAnnotation_Column(nextField, theGetter, nextMappedField);
				handleAnnotation_JoinColumn(nextField, theGetter, nextMappedField);
				handleAnnotation_AccessRights(nextField, theGetter, nextMappedField);

				// Check to see if this has any PropertyInterfaces that need to
				// be addressed.
				handleAnnotation_PropertyInterface(nextField, theGetter, nextMappedField);
				handleAnnotation_PropertyInterfaces(nextField, theGetter, nextMappedField);
			}
		} catch (Exception e) {
			logger.error("Error parsing MappedClass " + this.className, e);
		}
		fieldsInitialized = true;
	}

	/**
	 * @param nextField
	 * @param theGetter
	 * @param nextMappedField
	 * @return
	 */
	protected PropertyInterfaces handleAnnotation_PropertyInterfaces(Field nextField, Method theGetter,
			MappedField nextMappedField) {
		PropertyInterfaces propertyInterfaces = retrieveAnnotation(nextField, theGetter, PropertyInterfaces.class);
		if (propertyInterfaces != null) {
			for (PropertyInterface nextPropInterface : propertyInterfaces.propertyInterfaces()) {
				processMappedFieldPropertyInterface(nextPropInterface, nextMappedField);
			}
		}
		return propertyInterfaces;
	}

	/**
	 * @param nextField
	 * @param theGetter
	 * @param nextMappedField
	 * @return
	 */
	protected PropertyInterface handleAnnotation_PropertyInterface(Field nextField, Method theGetter,
			MappedField nextMappedField) {
		PropertyInterface propInterface = retrieveAnnotation(nextField, theGetter, PropertyInterface.class);
		if (propInterface != null) {
			processMappedFieldPropertyInterface(propInterface, nextMappedField);
		}
		return propInterface;
	}

	/**
	 * @param nextField
	 * @param theGetter
	 * @param nextMappedField
	 * @return
	 */
	protected OneToOne handleAnnotation_OneToOne(Field nextField, Method theGetter, MappedField nextMappedField) {
		OneToOne oneToOne = retrieveAnnotation(nextField, theGetter, OneToOne.class);
		if (oneToOne != null) {
			if (StringUtils.hasText(oneToOne.mappedBy())) {
				getTargetMappedFields().add(nextMappedField);
			} else {
				getSourceMappedFields().add((MappedFieldPerceroObject) nextMappedField);
			}
		}
		return oneToOne;
	}

	/**
	 * @param nextField
	 * @param theGetter
	 * @param nextMappedField
	 */
	protected void handleAnnotation_ManyToOne(Field nextField, Method theGetter, MappedField nextMappedField) {
		if (hasAnnotation(nextField, theGetter, ManyToOne.class)) {
			getSourceMappedFields().add((MappedFieldPerceroObject) nextMappedField);
		}
	}

	/**
	 * @param nextField
	 * @param theGetter
	 * @param nextMappedField
	 * @return
	 */
	protected OneToMany handleAnnotation_OneToMany(Field nextField, Method theGetter, MappedField nextMappedField) {
		OneToMany oneToMany = retrieveAnnotation(nextField, theGetter, OneToMany.class);
		if (oneToMany != null) {
			toManyFields.add(nextMappedField);
			getTargetMappedFields().add(nextMappedField);
			((MappedFieldList) nextMappedField).setListClass(oneToMany.targetEntity());
		}
		return oneToMany;
	}

	/**
	 * @param nextField
	 * @param theGetter
	 * @param nextMappedField
	 */
	protected void handleAnnotation_Id(Field nextField, Method theGetter, MappedField nextMappedField) {
		if (hasAnnotation(nextField, theGetter, Id.class)) {
			idMappedField = nextMappedField;

			List<MappedField> uniqueConstraintList = new ArrayList<MappedField>(1);
			uniqueConstraintList.add(nextMappedField);
			uniqueConstraints.add(uniqueConstraintList);

			// Check to see if this class has a Generated ID.
			hasGeneratedId = hasAnnotation(nextField, theGetter, GeneratedValue.class);
		}
	}

	/**
	 * @param nextField
	 * @param theGetter
	 * @param nextMappedField
	 * @return
	 */
	protected JoinColumn handleAnnotation_JoinColumn(Field nextField, Method theGetter, MappedField nextMappedField) {
		JoinColumn joinColumn = retrieveAnnotation(nextField, theGetter, JoinColumn.class);
		if (joinColumn != null) {
			if (StringUtils.hasText(joinColumn.name())) {
				nextMappedField.setJoinColumnName(joinColumn.name());
			}
		}
		return joinColumn;
	}

	/**
	 * @param nextField
	 * @param theGetter
	 * @param nextMappedField
	 * @return
	 */
	protected Column handleAnnotation_Column(Field nextField, Method theGetter, MappedField nextMappedField) {
		Column column = retrieveAnnotation(nextField, theGetter, Column.class);
		if (column != null) {
			if (column.unique()) {
				List<MappedField> uniqueConstraintList = new ArrayList<MappedField>(1);
				uniqueConstraintList.add(nextMappedField);
				uniqueConstraints.add(uniqueConstraintList);
			}

			if (column.name() != null && column.name().trim().length() > 0)
				nextMappedField.setColumnName(column.name());
			else
				nextMappedField.setColumnName(nextField.getName());
		}
		return column;
	}

	/**
	 * @param nextField
	 * @param theGetter
	 * @param nextMappedField
	 * @return
	 */
	protected AccessRights handleAnnotation_AccessRights(Field nextField, Method theGetter, MappedField nextMappedField) {
		AccessRights accessRights = retrieveAnnotation(nextField, theGetter, AccessRights.class);
		if (accessRights != null) {
			// Get NamedQueries for handling Access Rights.
			for (AccessRight nextAccessRight : accessRights.value()) {
				/*
				 * if
				 * (nextAccessRight.type().equalsIgnoreCase("createQuery"
				 * )) { if (nextAccessRight.query().indexOf("jpql:") >=
				 * 0) nextMappedField.createQuery = new JpqlQuery();
				 * else nextMappedField.createQuery = new MappedQuery();
				 * nextMappedField
				 * .createQuery.setQuery(nextAccessRight.query()); }
				 * else if
				 * (nextAccessRight.type().equalsIgnoreCase("updateQuery"
				 * )) { if (nextAccessRight.query().indexOf("jpql:") >=
				 * 0) nextMappedField.updateQuery = new JpqlQuery();
				 * else nextMappedField.updateQuery = new MappedQuery();
				 * nextMappedField
				 * .updateQuery.setQuery(nextAccessRight.query()); }
				 * else
				 */
				if (nextAccessRight.type()
						.equalsIgnoreCase("readQuery")) {
					if (nextAccessRight.query().indexOf("sql:") >= 0) {
						nextMappedField.setReadQuery(new SqlQuery(
								nextAccessRight.query().substring(
										nextAccessRight.query()
												.indexOf("sql:") + 4)));
					} else {
						// Whatever type of Query this is, it is not
						// supported.
						continue;
						// nextMappedField.setReadQuery(new
						// MappedQuery());
					}
					nextMappedField.setHasReadAccessRights(true);
					readAccessRightsFieldReferences
							.add(nextMappedField);
				} /*
				 * else if
				 * (nextAccessRight.type().equalsIgnoreCase("deleteQuery"
				 * )) { if (nextAccessRight.query().indexOf("jpql:") >=
				 * 0) nextMappedField.deleteQuery = new JpqlQuery();
				 * else nextMappedField.deleteQuery = new MappedQuery();
				 * nextMappedField
				 * .deleteQuery.setQuery(nextAccessRight.query()); }
				 */

				// Add to queries list.
				IMappedQuery nextQuery = null;
				if (nextAccessRight.query().indexOf("sql:") >= 0) {
					nextQuery = new SqlQuery(nextAccessRight.query()
							.substring(
									nextAccessRight.query().indexOf(
											"sql:") + 4));
				} else {
					// Unsupported Query type
					continue;
					// nextQuery = new MappedQuery();
				}
				nextQuery.setQueryName(nextAccessRight.type());

				nextMappedField.queries.add(nextQuery);
			}
		}
		return accessRights;
	}

	/**
	 * @param mappedField
	 * @param annotationClass
	 * @return
	 */
	private <T extends Annotation> T retrieveAnnotation(MappedField mappedField, Class<T> annotationClass) {
		if (mappedField != null) {
			return retrieveAnnotation(mappedField.getField(), mappedField.getGetter(), annotationClass);
		}
		else {
			return null;
		}
	}
	
	/**
	 * @param nextField
	 * @param theGetter
	 * @param annotationClass
	 * @return
	 */
	private <T extends Annotation> T retrieveAnnotation(Field nextField, Method theGetter, Class<T> annotationClass) {
		T annotation = (theGetter != null ? theGetter.getAnnotation(annotationClass) : null);
		if (annotation == null)
			annotation = (nextField != null ? nextField.getAnnotation(annotationClass) : null);
		return annotation;
	}

	/**
	 * @param mappedField
	 * @param annotationClass
	 * @return
	 */
	private boolean hasAnnotation(MappedField mappedField, Class annotationClass) {
		if (mappedField != null) {
			return hasAnnotation(mappedField.getField(),  mappedField.getGetter(), annotationClass);
		}
		else {
			return false;
		}
	}
	/**
	 * @param nextField
	 * @param theGetter
	 * @param annotationClass
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	private boolean hasAnnotation(Field nextField, Method theGetter, Class annotationClass) {
		boolean result = (theGetter != null ? theGetter.isAnnotationPresent(annotationClass) : false);
		if (!result)
			result = (nextField != null ? nextField.isAnnotationPresent(annotationClass) : false);
		return result;
	}
	
	private void processMappedFieldPropertyInterface(
			PropertyInterface propInterface, MappedField mappedField) {
		if (propInterface == null
				|| !StringUtils.hasText(propInterface.propertyName())) {
			logger.warn("Invalid PropertyInterface for " + className + "."
					+ mappedField.getField().getName());
			return;
		}

		EntityImplementation entityImpl = entityImplementations
				.get(propInterface.entityInterfaceClass());
		if (entityImpl != null) {
			Iterator<PropertyImplementation> itrPropImpls = entityImpl.propertyImplementations
					.iterator();
			while (itrPropImpls.hasNext()) {
				PropertyImplementation nextPropImpl = itrPropImpls.next();
				if (nextPropImpl.propertyName.equals(propInterface
						.propertyName())) {
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

			for (PropertyInterfaceParam nextPropInterfaceParam : propInterface
					.params()) {
				PropertyImplementationParam param = new PropertyImplementationParam();
				param.propertyImplementation = propImpl;
				param.name = nextPropInterfaceParam.name();
				param.value = nextPropInterfaceParam.value();
				propImpl.params.add(param);
			}
		} else {
			// Need to crearte EntityImplementation as well.
			entityImpl = new EntityImplementation();
			entityImpl.entityInterfaceClass = propInterface
					.entityInterfaceClass();
			entityImpl.mappedClass = this;

			PropertyImplementation propImpl = new PropertyImplementation();
			propImpl.entityImplementation = entityImpl;
			propImpl.propertyName = propInterface.propertyName();
			propImpl.mappedField = mappedField;
			entityImpl.propertyImplementations.add(propImpl);

			for (PropertyInterfaceParam nextPropInterfaceParam : propInterface
					.params()) {
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
		IMappedClassManager mcm = MappedClassManagerFactory
				.getMappedClassManager();
		try {
			Class clazz = MappedClass.forName(className);
			Class tempClazz = clazz;
			while (!tempClazz.equals(Object.class)) {
				// Get Entity name, if exists.
				if (!StringUtils.hasText(tableName)) {
					Entity entity = (Entity) tempClazz
							.getAnnotation(Entity.class);
					if (entity != null) {
						tableName = entity.name();
					}
				}

				if (!StringUtils.hasText(tableSchema)) {
					Table table = (Table) tempClazz.getAnnotation(Table.class);
					if (table != null && StringUtils.hasText(table.schema())) {
						tableSchema = table.schema();
					}
				}

				if (!StringUtils.hasText(dataProviderName)) {
					DataProvider dataProvider = (DataProvider) tempClazz
							.getAnnotation(DataProvider.class);
					if (dataProvider != null) {
						setDataProviderName(dataProvider.name());
					}
				}

				// Get native queries for handling AccessRights
				/**
				 * readAllQuery Added by Jonathan Samples because initial
				 * download for certain entities was disasterously slow using
				 * the old style
				 */
				PerceroNamedNativeQueries nativeQueries = (PerceroNamedNativeQueries) tempClazz
						.getAnnotation(PerceroNamedNativeQueries.class);
				if (nativeQueries != null) {
					for (PerceroNamedNativeQuery q : nativeQueries.value()) {
						if (q.name().equalsIgnoreCase("readAllQuery")) {
							logger.debug("Adding readAllQuery to mappedClass: "
									+ className);
							readAllQuery = new SqlQuery(q.query());
						}
					}
				}

				// Get NamedQueries for handling Access Rights.
				NamedQueries namedQueries = (NamedQueries) tempClazz
						.getAnnotation(NamedQueries.class);
				if (namedQueries != null) {
					for (NamedQuery nextNamedQuery : namedQueries.value()) {

						if (nextNamedQuery.name().equalsIgnoreCase(
								"createQuery")) {
							createQuery = QueryFactory
									.createQuery(nextNamedQuery.query());
						} else if (nextNamedQuery.name().equalsIgnoreCase(
								"updateQuery")) {
							updateQuery = QueryFactory
									.createQuery(nextNamedQuery.query());
						} else if (nextNamedQuery.name().equalsIgnoreCase(
								"readQuery")) {
							readQuery = QueryFactory.createQuery(nextNamedQuery
									.query());

							Iterator<MappedFieldPerceroObject> itrToOneFields = toOneFields
									.iterator();
							while (itrToOneFields.hasNext()) {
								MappedFieldPerceroObject nextMappedField = itrToOneFields
										.next();
								MappedClass referencedMappedClass = mcm
										.getMappedClassByClassName(nextMappedField
												.getField().getType()
												.getCanonicalName());
								// Need to find the corresponding field.
								for (MappedField nextRefMappedField : referencedMappedClass.toManyFields) {
									if (nextRefMappedField instanceof MappedFieldList) {
										OneToMany refOneToMany = retrieveAnnotation(nextRefMappedField, OneToMany.class);

										if (refOneToMany != null
												&& refOneToMany.targetEntity()
														.getCanonicalName()
														.equals(this.className)
												&& nextMappedField
														.getField()
														.getName()
														.equals(refOneToMany
																.mappedBy())) {
											// Found the referenced field.
											nextRefMappedField
													.setHasReadAccessRights(true);
											referencedMappedClass.readAccessRightsFieldReferences
													.add(nextRefMappedField);
											break;
										}
									} else if (nextRefMappedField instanceof MappedFieldPerceroObject) {
										OneToOne refOneToOne = retrieveAnnotation(nextRefMappedField, OneToOne.class);

										if (refOneToOne != null
												&& refOneToOne.targetEntity()
														.getCanonicalName()
														.equals(this.className)
												&& nextMappedField
														.getField()
														.getName()
														.equals(refOneToOne
																.mappedBy())) {
											// Found the referenced field.
											nextRefMappedField
													.setHasReadAccessRights(true);
											referencedMappedClass.readAccessRightsFieldReferences
													.add(nextRefMappedField);
											break;
										}
									}
								}
							}

						} else if (nextNamedQuery.name().equalsIgnoreCase(
								"deleteQuery")) {
							deleteQuery = QueryFactory
									.createQuery(nextNamedQuery.query());
						}

						// Add to queries list.
						IMappedQuery nextQuery = QueryFactory
								.createQuery(nextNamedQuery.query());
						nextQuery.setQueryName(nextNamedQuery.name());

						queries.add(nextQuery);
					}
				}

				Table table = (Table) tempClazz.getAnnotation(Table.class);

				if (table != null) {
					for (UniqueConstraint nextUniqueConstraint : table
							.uniqueConstraints()) {
						// TODO: Add an Array of MappedFields instead of a
						// UniqueConstraint.
						List<MappedField> listMappedFields = new ArrayList<MappedField>();
						for (String nextColumnName : nextUniqueConstraint
								.columnNames()) {
							Iterator<MappedField> itrAllMappedFields = externalizableFields
									.iterator();
							while (itrAllMappedFields.hasNext()) {
								MappedField nextMappedField = itrAllMappedFields
										.next();
								if (nextColumnName
										.equalsIgnoreCase(nextMappedField
												.getColumnName())) {
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
		} catch (Exception e) {
			logger.error("Error parsing MappedClass " + this.className, e);
		}

		queriesInitialized = true;
	}

	public void initializeRelationships() {
		if (relationshipsInitialized) {
			return;
		}
		IMappedClassManager mcm = MappedClassManagerFactory
				.getMappedClassManager();
		try {

			// NOTE: These sections have to be done after the code above so that
			// we know all MappedClasses in the toManyFields list have been
			// initialized
			Iterator<MappedField> itrExternalizeFields = externalizableFields
					.iterator();
			while (itrExternalizeFields.hasNext()) {
				MappedField nextMappedField = itrExternalizeFields.next();

				if (hasAnnotation(nextMappedField, OneToMany.class)) {
					// // This must be a source MappedField
					// sourceMappedFields.add((MappedFieldPerceroObject)
					// nextMappedField);

					// toManyFields.add(nextMappedField);
					ParameterizedType listType = (ParameterizedType) nextMappedField
							.getField().getGenericType();
					Class<?> listClass = (Class<?>) listType
							.getActualTypeArguments()[0];
					MappedClass referencedMappedClass = mcm
							.getMappedClassByClassName(listClass
									.getCanonicalName());
					if (!referencedMappedClass.fieldsInitialized) {
						referencedMappedClass.initializeFields();
					}
					if (referencedMappedClass.getReadQuery() != null
							&& StringUtils.hasText(referencedMappedClass
									.getReadQuery().getQuery())) {
						nextMappedField.setHasReadAccessRights(true);
						readAccessRightsFieldReferences.add(nextMappedField);
					}

				}

				ManyToOne manyToOne = retrieveAnnotation(nextMappedField, ManyToOne.class);
				OneToOne oneToOne = retrieveAnnotation(nextMappedField, OneToOne.class);

				if (manyToOne != null && !manyToOne.optional()
						|| oneToOne != null && !oneToOne.optional()) {
					requiredFields.add(nextMappedField);

					MappedClass referencedMappedClass = mcm
							.getMappedClassByClassName(nextMappedField
									.getField().getType().getName());
					if (!referencedMappedClass.fieldsInitialized) {
						referencedMappedClass.initializeFields();
					}

					if (referencedMappedClass.getReadQuery() != null
							&& StringUtils.hasText(referencedMappedClass
									.getReadQuery().getQuery())) {
						nextMappedField.setHasReadAccessRights(true);
						readAccessRightsFieldReferences.add(nextMappedField);
					}

					MappedField reverseMappedField = null;

					// Find the reverse field.
					for (MappedField nextRefMappedField : referencedMappedClass.toManyFields) {
						if (nextRefMappedField instanceof MappedFieldList) {
							OneToMany refOneToMany = retrieveAnnotation(nextRefMappedField, OneToMany.class);

							if (refOneToMany != null) {

								Boolean inheritsFrom = inheritsFrom(this.clazz, refOneToMany.targetEntity());
								if (inheritsFrom
										&& nextMappedField.getField().getName().equals(refOneToMany.mappedBy())) {
//								if (this.clazz == refOneToMany.targetEntity()
//										&& nextMappedField
//												.getField()
//												.getName()
//												.equals(refOneToMany.mappedBy())) {
									// Found the referenced field.
									reverseMappedField = nextRefMappedField;
									break;
								}
							}
						} else if (nextRefMappedField instanceof MappedFieldPerceroObject) {
							OneToOne refOneToOne = retrieveAnnotation(nextRefMappedField, OneToOne.class);

							if (refOneToOne != null) {
								 Boolean inheritsFrom =
								 inheritsFrom(this.clazz,
								 refOneToOne.targetEntity());
								 if (inheritsFrom &&
								 nextMappedField.getField().getName().equals(refOneToOne.mappedBy()))
								 {
//								if (this.clazz == refOneToOne.targetEntity()
//										&& nextMappedField.getField().getName()
//												.equals(refOneToOne.mappedBy())) {
									// Found the referenced field.
									reverseMappedField = nextRefMappedField;
									break;
								}
							}
						}
					}

					if (reverseMappedField == null) {
						// Find the reverse field.
						for (MappedField nextRefMappedField : referencedMappedClass.toOneFields) {
							if (nextRefMappedField instanceof MappedFieldPerceroObject) {
								OneToOne refOneToOne = retrieveAnnotation(nextRefMappedField, OneToOne.class);

								if (refOneToOne != null) {
									if (StringUtils.hasText(refOneToOne.mappedBy())) {
										 Boolean inheritsFrom = inheritsFrom(this.clazz,
												 nextRefMappedField.getField().getType());
										if (inheritsFrom &&
												 nextMappedField.getField().getName().equals(refOneToOne.mappedBy()))
										 {
												// Found the referenced field.
												reverseMappedField = nextRefMappedField;
												break;
										 }
									}
									else {
										if (this.clazz == nextRefMappedField
												.getField().getType()
												&& nextMappedField
														.getField()
														.getName()
														.equals(refOneToOne
																.mappedBy())) {
											// Found the referenced field.
											reverseMappedField = nextRefMappedField;
											break;
										}
									}
								}
							}
						}
					}

					if (!referencedMappedClass.cascadeRemoveFieldReferences
							.keySet().contains(nextMappedField)) {
						if (reverseMappedField == null) {
							if (manyToOne != null) {
								if (!manyToOne.targetEntity().getName()
										.equalsIgnoreCase("void")) {
									System.out.println("IS THIS CORRECT?");
								}
							}
							if (oneToOne != null) {
								if (!oneToOne.targetEntity().getName()
										.equalsIgnoreCase("void")) {
									System.out.println("IS THIS CORRECT?");
								}
							}
						}
						referencedMappedClass.addCascadeRemoveFieldReferences(
								nextMappedField, reverseMappedField);
					}

					nextMappedField.setReverseMappedField(reverseMappedField);

				} else if (manyToOne != null
						&& manyToOne.optional()
						|| oneToOne != null
						&& oneToOne.optional()
						&& (oneToOne.mappedBy() == null || oneToOne.mappedBy()
								.isEmpty())) {
					MappedClass referencedMappedClass = mcm
							.getMappedClassByClassName(nextMappedField
									.getField().getType().getName());

					if (referencedMappedClass.getReadQuery() != null
							&& StringUtils.hasText(referencedMappedClass
									.getReadQuery().getQuery()))
						nextMappedField.setHasReadAccessRights(true);

					MappedField reverseMappedField = null;

					// Find the reverse field.
					for (MappedField nextRefMappedField : referencedMappedClass.toManyFields) {
						if (nextRefMappedField instanceof MappedFieldList) {
							OneToMany refOneToMany = retrieveAnnotation(nextRefMappedField, OneToMany.class);

							if (refOneToMany != null) {
								 Boolean inheritsFrom =
								 inheritsFrom(this.clazz,
								 refOneToMany.targetEntity());
								 if (inheritsFrom &&
								 nextMappedField.getField().getName().equals(refOneToMany.mappedBy()))
								 {
//								if (this.clazz == refOneToMany.targetEntity()
//										&& nextMappedField
//												.getField()
//												.getName()
//												.equals(refOneToMany.mappedBy())) {
									// Found the referenced field.
									reverseMappedField = nextRefMappedField;
									break;
								}
							}
						} else if (nextRefMappedField instanceof MappedFieldPerceroObject) {
							OneToOne refOneToOne = retrieveAnnotation(nextRefMappedField, OneToOne.class);

							if (refOneToOne != null) {
								if (StringUtils.hasText(refOneToOne.mappedBy())) {
									 Boolean inheritsFrom = inheritsFrom(this.clazz,
											 refOneToOne.targetEntity());
									 if (inheritsFrom && nextMappedField.getField().getName().equals(refOneToOne.mappedBy()))
									 {
											if (this.clazz == refOneToOne.targetEntity()
													&& nextMappedField.getField().getName()
															.equals(refOneToOne.mappedBy())) {
												// Found the referenced field.
												reverseMappedField = nextRefMappedField;
												break;
											}
									 }
								}
								else {
									if (this.clazz == refOneToOne.targetEntity()
											&& nextMappedField.getField().getName()
													.equals(refOneToOne.mappedBy())) {
										// Found the referenced field.
										reverseMappedField = nextRefMappedField;
										break;
									}
								}
							}
						}
					}

					if (reverseMappedField == null) {
						// Find the reverse field.
						for (MappedField nextRefMappedField : referencedMappedClass.toOneFields) {
							if (nextRefMappedField instanceof MappedFieldPerceroObject) {
								OneToOne refOneToOne = retrieveAnnotation(nextRefMappedField, OneToOne.class);

								if (refOneToOne != null) {
									// Boolean inheritsFrom =
									// inheritsFrom(this.clazz,
									// nextRefMappedField.getField().getType());
									// if (inheritsFrom &&
									// nextMappedField.getField().getName().equals(refOneToOne.mappedBy()))
									// {
									if (this.clazz == nextRefMappedField.getField().getType()
											&& nextMappedField.getField().getName().equals(refOneToOne.mappedBy())) {
										// Found the referenced field.
										reverseMappedField = nextRefMappedField;
										break;
									}
								}
							}
						}
					}

					if (!referencedMappedClass.nulledOnRemoveFieldReferences
							.keySet().contains(nextMappedField)) {
						if (reverseMappedField == null) {
							if (manyToOne != null) {
								if (!manyToOne.targetEntity().getName()
										.equalsIgnoreCase("void")) {
									System.out.println("IS THIS CORRECT?");
								}
							}
							if (oneToOne != null) {
								if (!oneToOne.targetEntity().getName()
										.equalsIgnoreCase("void")) {
									System.out.println("IS THIS CORRECT?");
								}
							}
						}
						referencedMappedClass.addNulledOnRemoveFieldReferences
								(nextMappedField, reverseMappedField);
						readAccessRightsFieldReferences.add(nextMappedField);
					}

					nextMappedField.setReverseMappedField(reverseMappedField);
				}

				// Check to see if this has any RelationshipInterfaces that need
				// to be addresed.
				RelationshipInterface relationshipInterface = retrieveAnnotation(nextMappedField, RelationshipInterface.class);
				if (relationshipInterface != null) {
					processMappedFieldRelationshipInterface(relationshipInterface,
							nextMappedField);
				}
				RelationshipInterfaces relationshipInterfaces = retrieveAnnotation(nextMappedField, RelationshipInterfaces.class);
				if (relationshipInterfaces != null) {
					for (RelationshipInterface nextPropInterface : relationshipInterfaces
							.relationshipInterfaces()) {
						processMappedFieldRelationshipInterface(
								nextPropInterface, nextMappedField);
					}
				}
			}

			// Now check Read AccessRights on all toMany fields.
			Iterator<MappedField> itrToManyFields = toManyFields.iterator();
			while (itrToManyFields.hasNext()) {
				MappedField nextMappedField = itrToManyFields.next();
				ParameterizedType listType = (ParameterizedType) nextMappedField
						.getField().getGenericType();
				Class<?> listClass = (Class<?>) listType
						.getActualTypeArguments()[0];
				MappedClass referencedMappedClass = mcm
						.getMappedClassByClassName(listClass.getCanonicalName());
				if (referencedMappedClass.getReadQuery() != null
						&& StringUtils.hasText(referencedMappedClass
								.getReadQuery().getQuery())) {
					nextMappedField.setHasReadAccessRights(true);
					readAccessRightsFieldReferences.add(nextMappedField);
				}
			}
		} catch (Exception e) {
			logger.error("Error parsing MappedClass " + this.className, e);
		}

		relationshipsInitialized = true;
	}

	private void addCascadeRemoveFieldReferences(MappedField nextMappedField, MappedField reverseMappedField) {
		cascadeRemoveFieldReferences.put(nextMappedField, reverseMappedField);
	}

	public Map<MappedField, MappedField> getNulledOnRemoveFieldReferences() {
		Map<MappedField, MappedField> result = new HashMap<MappedField, MappedField>(this.nulledOnRemoveFieldReferences.size());
		MappedClass mappedClass = this;
		while (mappedClass != null) {
			result.putAll(mappedClass.nulledOnRemoveFieldReferences);
			mappedClass = mappedClass.parentMappedClass;
		}
		
		return result;
	}
	
	public Map<MappedField, MappedField> getCascadeRemoveFieldReferences() {
		Map<MappedField, MappedField> result = new HashMap<MappedField, MappedField>(this.cascadeRemoveFieldReferences.size());
		MappedClass mappedClass = this;
		while (mappedClass != null) {
			result.putAll(mappedClass.cascadeRemoveFieldReferences);
			mappedClass = mappedClass.parentMappedClass;
		}
		
		return result;
	}

	private void addNulledOnRemoveFieldReferences(MappedField nextMappedField, MappedField reverseMappedField) {
		nulledOnRemoveFieldReferences.put(nextMappedField, reverseMappedField);
	}

	private void processMappedFieldRelationshipInterface(
			RelationshipInterface relInterface, MappedField mappedField) {
		if (relInterface == null
				|| !StringUtils.hasText(relInterface.sourceVarName())) {
			logger.warn("Invalid RelationshipInterface for " + className + "."
					+ mappedField.getField().getName());
			return;
		}

		EntityImplementation entityImpl = entityImplementations
				.get(relInterface.entityInterfaceClass());
		if (entityImpl != null) {
			Iterator<RelationshipImplementation> itrRelImpls = entityImpl.relationshipImplementations
					.iterator();
			while (itrRelImpls.hasNext()) {
				RelationshipImplementation nextRelImpl = itrRelImpls.next();
				if (nextRelImpl.sourceVarName.equals(relInterface
						.sourceVarName())) {
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
		} else {
			// Need to crearte EntityImplementation as well.
			entityImpl = new EntityImplementation();
			entityImpl.entityInterfaceClass = relInterface
					.entityInterfaceClass();
			entityImpl.mappedClass = this;

			RelationshipImplementation relImpl = new RelationshipImplementation();
			relImpl.entityImplementation = entityImpl;
			relImpl.sourceVarName = relInterface.sourceVarName();
			relImpl.sourceMappedField = mappedField;
			entityImpl.relationshipImplementations.add(relImpl);
		}

		/**
		 * // Find the existing RelationshipImplementation and set its
		 * mappedField. Iterator<Class> itrClasses =
		 * entityImplementations.keySet().iterator(); while
		 * (itrClasses.hasNext()) { Class nextInterfaceClazz =
		 * itrClasses.next(); Iterator<EntityImplementation> itrEntityImpls =
		 * entityImplementations.get(nextInterfaceClazz).iterator(); while
		 * (itrEntityImpls.hasNext()) { EntityImplementation nextEntityImpl =
		 * itrEntityImpls.next(); Iterator<RelationshipImplementation>
		 * itrRelImpls = nextEntityImpl.relationshipImplementations.iterator();
		 * while (itrRelImpls.hasNext()) { RelationshipImplementation
		 * nextRelImpl = itrRelImpls.next(); if
		 * (nextRelImpl.sourceVarName.equals(relInterface.sourceVarName())) { //
		 * Found relationship implementation. nextRelImpl.sourceMappedField =
		 * mappedField; return; } } } }
		 * 
		 * // No corresponding Relationship Implementation.
		 * logger.warn("No RelationshipImplementation found on " + className +
		 * "." + mappedField.getField().getName() + " of type " +
		 * relInterface.sourceVarName());
		 */
	}

	@SuppressWarnings("rawtypes")
	private boolean inheritsFrom(Class a, Class b) {
		if (a.equals(b))
			return true;

		Class s = a.getSuperclass();
		while (true) {
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

		for (Class nextInterface : interfaces) {
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
		IMappedClassManager mcm = MappedClassManagerFactory
				.getMappedClassManager();

		try {
			Class clazz = MappedClass.forName(className);
			MappedClass superMappedClass = mcm.getMappedClassByClassName(clazz
					.getSuperclass().getName());
			return superMappedClass;
		} catch (Exception e) {
			return null;
		}
	}

	public MappedField getMappedFieldByName(String fieldName) {
		MappedField result = null;
		for (MappedField nextMappedField : externalizableFields) {
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
			if ((this.className == null || this.className.trim().length() == 0)
					&& (mcObj.className == null || mcObj.className.trim()
							.length() == 0))
				return true;
			else if ((this.className == null || this.className.trim().length() == 0)
					|| (mcObj.className == null || mcObj.className.trim()
							.length() == 0))
				return false;
			else
				return this.className.equals(mcObj.className);
		}
	}

	/***************************************
	 * Helper Methods
	 ***************************************/
	public Map<ClassIDPair, MappedField> getRelatedClassIdPairMappedFieldMap(
			IPerceroObject perceroObject, Boolean isShellObject)
			throws Exception {
		Map<ClassIDPair, MappedField> results = new HashMap<ClassIDPair, MappedField>();

		Iterator<MappedFieldPerceroObject> itrToOneFieldsToUpdate = toOneFields
				.iterator();
		while (itrToOneFieldsToUpdate.hasNext()) {
			MappedFieldPerceroObject nextToOneField = itrToOneFieldsToUpdate
					.next();

			// If no reverse mapped field, then nothing to do.
			if (nextToOneField.getReverseMappedField() != null) {

				// If no PerceroObject, then we need to retrieve ALL objects of
				// this type.
				if (perceroObject == null) {
					MappedClass reverseMappedClass = nextToOneField
							.getReverseMappedField().getMappedClass();
					IDataProvider reverseDataProvider = reverseMappedClass
							.getDataProvider();
					List<IPerceroObject> relatedObjects = reverseDataProvider
							.getAllByName(reverseMappedClass.className, null,
									null, false, null);
					Iterator<IPerceroObject> itrRelatedObjects = relatedObjects
							.iterator();
					while (itrRelatedObjects.hasNext()) {
						IPerceroObject nextRelatedObject = itrRelatedObjects
								.next();
						results.put(
								BaseDataObject.toClassIdPair(nextRelatedObject),
								nextToOneField.getReverseMappedField());
					}
				} else if (isShellObject) {
					// If this is a Shell Object, then we need to ask the
					// IDataProvider to get the related objects.
					List<IPerceroObject> relatedObjects = getDataProvider()
							.findAllRelatedObjects(perceroObject,
									nextToOneField, true, null);
					Iterator<IPerceroObject> itrRelatedObjects = relatedObjects
							.iterator();
					while (itrRelatedObjects.hasNext()) {
						IPerceroObject nextRelatedObject = itrRelatedObjects
								.next();
						results.put(
								BaseDataObject.toClassIdPair(nextRelatedObject),
								nextToOneField.getReverseMappedField());
					}
				} else {
					IPerceroObject toOneObject = (IPerceroObject) nextToOneField
							.getGetter().invoke(perceroObject);
					if (toOneObject != null) {
						results.put(BaseDataObject.toClassIdPair(toOneObject),
								nextToOneField.getReverseMappedField());
					}
				}
			}
		}

		Iterator<MappedField> itrToManyFieldsToUpdate = toManyFields.iterator();
		while (itrToManyFieldsToUpdate.hasNext()) {
			MappedField nextToManyField = itrToManyFieldsToUpdate.next();
			if (nextToManyField instanceof MappedFieldPerceroObject) {
				MappedFieldPerceroObject nextPerceroObjectField = (MappedFieldPerceroObject) nextToManyField;

				if (nextPerceroObjectField.getReverseMappedField() != null) {

					// If no PerceroObject, then we need to retrieve ALL objects
					// of this type.
					if (perceroObject == null) {
						MappedClass reverseMappedClass = nextToManyField
								.getReverseMappedField().getMappedClass();
						IDataProvider reverseDataProvider = reverseMappedClass
								.getDataProvider();
						List<IPerceroObject> relatedObjects = reverseDataProvider
								.getAllByName(reverseMappedClass.className,
										null, null, false, null);
						Iterator<IPerceroObject> itrRelatedObjects = relatedObjects
								.iterator();
						while (itrRelatedObjects.hasNext()) {
							IPerceroObject nextRelatedObject = itrRelatedObjects
									.next();
							results.put(BaseDataObject
									.toClassIdPair(nextRelatedObject),
									nextToManyField.getReverseMappedField());
						}
					} else if (isShellObject) {
						// If this is a Shell Object, then we need to ask the
						// IDataProvider to get the related objects.
						List<IPerceroObject> relatedObjects = getDataProvider()
								.findAllRelatedObjects(perceroObject,
										nextToManyField, true, null);
						Iterator<IPerceroObject> itrRelatedObjects = relatedObjects
								.iterator();
						while (itrRelatedObjects.hasNext()) {
							IPerceroObject nextRelatedObject = itrRelatedObjects
									.next();
							results.put(BaseDataObject
									.toClassIdPair(nextRelatedObject),
									nextToManyField.getReverseMappedField());
						}
					} else {
						IPerceroObject toOneObject = (IPerceroObject) nextPerceroObjectField
								.getGetter().invoke(perceroObject);
						if (toOneObject != null) {
							results.put(BaseDataObject
									.toClassIdPair(toOneObject),
									nextPerceroObjectField
											.getReverseMappedField());
						}
					}
				}
			} else if (nextToManyField instanceof MappedFieldList) {
				MappedFieldList nextListField = (MappedFieldList) nextToManyField;

				if (nextListField.getReverseMappedField() != null) {

					// If no PerceroObject, then we need to retrieve ALL objects
					// of this type.
					if (perceroObject == null) {
						MappedClass reverseMappedClass = nextToManyField
								.getReverseMappedField().getMappedClass();
						IDataProvider reverseDataProvider = reverseMappedClass
								.getDataProvider();
						List<IPerceroObject> relatedObjects = reverseDataProvider
								.getAllByName(reverseMappedClass.className,
										null, null, false, null);
						Iterator<IPerceroObject> itrRelatedObjects = relatedObjects
								.iterator();
						while (itrRelatedObjects.hasNext()) {
							IPerceroObject nextRelatedObject = itrRelatedObjects
									.next();
							results.put(BaseDataObject
									.toClassIdPair(nextRelatedObject),
									nextToManyField.getReverseMappedField());
						}
					} else if (isShellObject) {
						// If this is a Shell Object, then we need to ask the
						// IDataProvider to get the related objects.
						List<IPerceroObject> relatedObjects = getDataProvider()
								.findAllRelatedObjects(perceroObject,
										nextToManyField, true, null);
						Iterator<IPerceroObject> itrRelatedObjects = relatedObjects
								.iterator();
						while (itrRelatedObjects.hasNext()) {
							IPerceroObject nextRelatedObject = itrRelatedObjects
									.next();
							results.put(BaseDataObject
									.toClassIdPair(nextRelatedObject),
									nextToManyField.getReverseMappedField());
						}
					} else {
						List<IPerceroObject> listObjects = (List<IPerceroObject>) nextListField
								.getGetter().invoke(perceroObject);
						if (listObjects != null && !listObjects.isEmpty()) {
							Iterator<IPerceroObject> itrListObjects = listObjects
									.iterator();
							while (itrListObjects.hasNext()) {
								IPerceroObject nextListObject = itrListObjects
										.next();
								results.put(BaseDataObject
										.toClassIdPair(nextListObject),
										nextListField.getReverseMappedField());
							}
						}
					}
				}
			}
		}

		return results;
	}

	/***************************************
	 * Static Helper Methods
	 ***************************************/
	@SuppressWarnings("rawtypes")
	private static Map<String, Class> CLASS_MAP = Collections
			.synchronizedMap(new HashMap<String, Class>());

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
	public static MappedClassMethodPair getFieldSetters(Class theClass,
			String theFieldName) {
		Method theMethod = null;
		Method[] theMethods = theClass.getMethods();
		String theModifiedFieldName = theFieldName;
		if (theModifiedFieldName.indexOf("_") == 0)
			theModifiedFieldName = theModifiedFieldName.substring(1);

		for (Method nextMethod : theMethods) {
			if (nextMethod.getName().equalsIgnoreCase(
					"set" + theModifiedFieldName)) {
				theMethod = nextMethod;
				break;
			}
		}

		if (mcm == null) {
			mcm = MappedClassManagerFactory.getMappedClassManager();
		}
		MappedClass mc = mcm.getMappedClassByClassName(theClass
				.getCanonicalName());

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
	
	public IPerceroObject newPerceroObject() throws ClassNotFoundException, InstantiationException, IllegalAccessException {
		IPerceroObject result = null;
		result = (IPerceroObject) clazz.newInstance();		
		return result;
	}
	
}
