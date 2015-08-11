package com.percero.agents.auth.services;


//import ru.minogin.core.client.bean.Bean;
//import ru.minogin.core.server.reflection.ReflectionUtils;

public class Dehibernator {
/**    private static final Logger logger = Logger.getLogger(Dehibernator.class);

    private IdentityHashMap<Object, Object> processed;

    @SuppressWarnings("unchecked")
    public <T> T clean(T object) {
        processed = new IdentityHashMap<Object, Object>();

        logger.debug("Cleaning: " + (object != null ? object.getClass() : null));
        return (T) doClean(object);
    }

    @SuppressWarnings("unchecked")
    private Object doClean(Object dirty) {
        logger.debug("Do clean: " + (dirty != null ? dirty.getClass() : null));

        if (dirty == null)
            return null;

        if (processed.containsKey(dirty)) {
            logger.debug("Object already cleaned, skipping.");

            return processed.get(dirty);
        }

        if (isPrimitive(dirty)) {
            logger.debug("Object is primitive, skipping.");

            return dirty;
        }

        if (dirty instanceof List) {
            logger.debug("Object is a List");

            List<Object> dirtyList = (List<Object>) dirty;
            List<Object> cleanList = new ArrayList<Object>();
            processed.put(dirtyList, cleanList);
            for (Object value : dirtyList) {
                cleanList.add(doClean(value));
            }
            return cleanList;
        }

        if (dirty instanceof PersistentBag) {
            logger.debug("Object is a PersistentBag");

            PersistentBag dirtyList = (PersistentBag) dirty;
            List<Object> cleanList = new ArrayList<Object>();
            processed.put(dirtyList, cleanList);
            if (dirtyList.wasInitialized()) {
                for (Object value : dirtyList) {
                    cleanList.add(doClean(value));
                }
            }
            return cleanList;
        }

        if (dirty instanceof PersistentSortedSet) {
            logger.debug("Object is a PersistentSortedSet");

            PersistentSortedSet dirtySet = (PersistentSortedSet) dirty;
            Set<Object> cleanSet = new TreeSet<Object>();
            processed.put(dirtySet, cleanSet);
            if (dirtySet.wasInitialized()) {
                for (Object value : dirtySet) {
                    cleanSet.add(doClean(value));
                }
            }
            return cleanSet;
        }

        if (dirty instanceof PersistentSet) {
            logger.debug("Object is a PersistentSet");

            PersistentSet dirtySet = (PersistentSet) dirty;
            Set<Object> cleanSet = new HashSet<Object>();
            processed.put(dirtySet, cleanSet);
            if (dirtySet.wasInitialized()) {
                for (Object value : dirtySet) {
                    cleanSet.add(doClean(value));
                }
            }
            return cleanSet;
        }

        if (dirty instanceof PersistentMap) {
            logger.debug("Object is a PersistentMap");

            PersistentMap dirtyMap = (PersistentMap) dirty;
            Map<Object, Object> cleanMap = new LinkedHashMap<Object, Object>();
            processed.put(dirtyMap, cleanMap);
            if (dirtyMap.wasInitialized()) {
                for (Object key : dirtyMap.keySet()) {
                    Object value = dirtyMap.get(key);
                    cleanMap.put(doClean(key), doClean(value));
                }
            }
            return cleanMap;
        }

        if (dirty instanceof List) {
            logger.debug("Object is a List");

            List<Object> dirtyList = (List<Object>) dirty;
            List<Object> cleanList = new ArrayList<Object>();
            processed.put(dirtyList, cleanList);
            for (Object value : dirtyList) {
                cleanList.add(doClean(value));
            }
            return cleanList;
        }

        if (dirty instanceof LinkedHashMap) {
            logger.debug("Object is a LinkedHashMap");

            Map<Object, Object> dirtyMap = (Map<Object, Object>) dirty;
            Map<Object, Object> cleanMap = new LinkedHashMap<Object, Object>();
            processed.put(dirtyMap, cleanMap);
            for (Object key : dirtyMap.keySet()) {
                Object value = dirtyMap.get(key);
                cleanMap.put(doClean(key), doClean(value));
            }
            return cleanMap;
        }

        if (dirty instanceof HashMap) {
            logger.debug("Object is a HashMap");

            Map<Object, Object> dirtyMap = (Map<Object, Object>) dirty;
            Map<Object, Object> cleanMap = new HashMap<Object, Object>();
            processed.put(dirtyMap, cleanMap);
            for (Object key : dirtyMap.keySet()) {
                Object value = dirtyMap.get(key);
                cleanMap.put(doClean(key), doClean(value));
            }
            return cleanMap;
        }

        if (dirty instanceof LinkedHashSet<?>) {
            logger.debug("Object is a LinkedHashSet");

            Set<Object> dirtySet = (LinkedHashSet<Object>) dirty;
            Set<Object> cleanSet = new LinkedHashSet<Object>();
            processed.put(dirtySet, cleanSet);
            for (Object value : dirtySet) {
                cleanSet.add(doClean(value));
            }
            return cleanSet;
        }

        if (dirty instanceof HashSet<?>) {
            logger.debug("Object is a HashSet");

            Set<Object> dirtySet = (HashSet<Object>) dirty;
            Set<Object> cleanSet = new HashSet<Object>();
            processed.put(dirtySet, cleanSet);
            for (Object value : dirtySet) {
                cleanSet.add(doClean(value));
            }
            return cleanSet;
        }

        if (dirty instanceof TreeSet<?>) {
            logger.debug("Object is a TreeSet");

            Set<Object> dirtySet = (TreeSet<Object>) dirty;
            Set<Object> cleanSet = new TreeSet<Object>();
            processed.put(dirtySet, cleanSet);
            for (Object value : dirtySet) {
                cleanSet.add(doClean(value));
            }
            return cleanSet;
        }

        if (dirty instanceof HibernateProxy) {
            logger.debug("Object is a HibernateProxy");

            HibernateProxy proxy = (HibernateProxy) dirty;
            LazyInitializer lazyInitializer = proxy.getHibernateLazyInitializer();
            if (lazyInitializer.isUninitialized()) {
                logger.debug("It is uninitialized, skipping");

                processed.put(dirty, null);
                return null;
            }
            else {
                logger.debug("It is initialized, getting implementati");

                dirty = lazyInitializer.getImplementation();
            }
        }

/**        if (dirty instanceof Bean) {
            logger.debug("Object is a Bean");

            Bean bean = (Bean) dirty;
            processed.put(bean, bean);
            for (String property : bean.getPropertyNames()) {
                bean.set(property, doClean(bean.get(property)));
            }
            return bean;
        }**

        processed.put(dirty, dirty);
        for (String property : ReflectionUtils.getProperties(dirty)) {
            logger.debug("Processing property " + property);

            Object value = ReflectionUtils.get(dirty, property);
            ReflectionUtils.setIfPossible(dirty, property, doClean(value));
        }
        return dirty;
    }

    private boolean isPrimitive(Object object) {
        if (object instanceof String)
            return true;

        if (object instanceof Date)
            return true;

        if (object instanceof Enum)
            return true;

        Class<? extends Object> xClass = object.getClass();
        if (xClass.isPrimitive())
            return true;

        return false;
    }
**/
}
