/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package com.xpn.xwiki.store.migration.hibernate;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.hibernate.Session;
import org.hibernate.boot.Metadata;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.Index;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.PrimaryKey;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Table;
import org.hibernate.query.NativeQuery;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.DeletedAttachment;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.XWikiLink;
import com.xpn.xwiki.doc.rcs.XWikiRCSNodeInfo;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseObjectReference;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.DBStringListProperty;
import com.xpn.xwiki.objects.DateProperty;
import com.xpn.xwiki.objects.DoubleProperty;
import com.xpn.xwiki.objects.FloatProperty;
import com.xpn.xwiki.objects.IntegerProperty;
import com.xpn.xwiki.objects.LargeStringProperty;
import com.xpn.xwiki.objects.LongProperty;
import com.xpn.xwiki.objects.StringListProperty;
import com.xpn.xwiki.objects.StringProperty;
import com.xpn.xwiki.stats.impl.DocumentStats;
import com.xpn.xwiki.stats.impl.RefererStats;
import com.xpn.xwiki.stats.impl.VisitStats;
import com.xpn.xwiki.stats.impl.XWikiStats;
import com.xpn.xwiki.store.DatabaseProduct;
import com.xpn.xwiki.store.XWikiHibernateBaseStore;
import com.xpn.xwiki.store.XWikiHibernateBaseStore.HibernateCallback;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.XWikiDBVersion;
import com.xpn.xwiki.util.Util;

/**
 * Migration for XWIKI-6990 Reduce the likelihood of having same (hibernate) document id for different documents. This
 * data migration convert document ID to a new hash algorithm.
 *
 * @version $Id$
 * @since 4.0M1
 */
@Component
@Named("R40000XWIKI6990")
@Singleton
public class R40000XWIKI6990DataMigration extends AbstractHibernateDataMigration
{
    /** Document classes to migrate, using the document id in the first column of their key. */
    private static final Class<?>[] DOC_CLASSES =
        new Class<?>[] { XWikiDocument.class, XWikiRCSNodeInfo.class, XWikiLink.class };

    /** Document related classes to migrate, using a property docId without FK information. */
    private static final Class<?>[] DOCLINK_CLASSES = new Class<?>[] { XWikiAttachment.class, DeletedAttachment.class };

    /** Property classes to migrate, using the object id in the first column of their key. */
    private static final Class<?>[] PROPERTY_CLASS = new Class<?>[] { DateProperty.class, DBStringListProperty.class,
    DoubleProperty.class, FloatProperty.class, IntegerProperty.class, LargeStringProperty.class, LongProperty.class,
    StringListProperty.class, StringProperty.class, BaseProperty.class };

    /** Statistics classes to migrate. (ID is the stats identifier) */
    private static final Class<?>[] STATS_CLASSES =
        new Class<?>[] { DocumentStats.class, RefererStats.class, VisitStats.class };

    /** Mark internal mapping. */
    private static final String INTERNAL = "internal";

    /** Stub statistic class used to compute new ids from existing objects. */
    private static final class StatsIdComputer extends XWikiStats
    {
        private static final long serialVersionUID = 1L;

        /** Name of the statistic. */
        private String name;

        /** Number of the statistic. */
        private int number;

        /**
         * Return the new identifier for a statistic having given name and number.
         *
         * @param name the name of the statistic
         * @param number the number of the statistic
         * @return the hash to use for the new id of the statistic
         */
        public long getId(String name, int number)
        {
            this.name = name;
            this.number = number;
            return super.getId();
        }

        @Override
        public String getName()
        {
            return this.name;
        }

        @Override
        public int getNumber()
        {
            return this.number;
        }
    }

    /**
     * Specialized HibernateCallback for id conversion.
     */
    private interface IdConversionHibernateCallback extends HibernateCallback<Object>
    {
        /**
         * Set the new identifier.
         *
         * @param newId the new identifier
         */
        void setNewId(long newId);

        /**
         * Set the old identifier.
         *
         * @param oldId the old identifier
         */
        void setOldId(long oldId);
    }

    /**
     * Base class for hibernate callback to convert identifier.
     */
    private abstract static class AbstractUpdateHibernateCallback implements HibernateCallback<Object>
    {
        /** Place holder for new id. */
        protected static final String NEWID = "newid";

        /** Place holder for old id. */
        protected static final String OLDID = "oldid";

        /** The new identifier. */
        protected Session session;

        /** The current timer. */
        public int timer;

        @Override
        public Object doInHibernate(Session session)
        {
            this.timer = 0;
            this.session = session;
            doUpdate();
            this.session = null;
            return null;
        }

        /**
         * Implement this method to execute an update.
         */
        public abstract void doUpdate();
    }

    /**
     * Base implementation of the hibernate callback to convert identifier using individual updates (safe-mode).
     */
    private abstract static class AbstractIdConversionHibernateCallback extends AbstractUpdateHibernateCallback
        implements IdConversionHibernateCallback
    {
        /** Name for the id column. */
        public static final String ID = "id";

        /** Name for the subid column. */
        public static final String IDID = "id.id";

        /** Name for the docid column. */
        public static final String DOCID = "docId";

        /** The old identifier. */
        private long oldId;

        /** The new identifier. */
        private long newId;

        @Override
        public void setNewId(long newId)
        {
            this.newId = newId;
        }

        @Override
        public void setOldId(long oldId)
        {
            this.oldId = oldId;
        }

        @Override
        public void doUpdate()
        {
            doSingleUpdate();
        }

        /**
         * Implement this method to execute a single ID update using {@code executeIdUpdate()}.
         */
        public abstract void doSingleUpdate();

        /**
         * Update object id in a given field for a given object class.
         *
         * @param klass the class of the persisted object
         * @param field the field name of the persisted object
         * @return the time elapsed during the operation
         */
        public long executeIdUpdate(Class<?> klass, String field)
        {
            return executeIdUpdate(klass.getName(), field);
        }

        /**
         * Update object id in a given field of a given table.
         *
         * @param name the entity name of the table
         * @param field the field name of the column
         * @return the time elapsed during the operation
         */
        public long executeIdUpdate(String name, String field)
        {
            StringBuilder sb = new StringBuilder(128);
            sb.append("update ").append(name).append(" klass set klass.").append(field).append('=').append(':')
                .append(NEWID).append(" where klass.").append(field).append('=').append(':').append(OLDID);
            long now = System.nanoTime();
            this.session.createQuery(sb.toString()).setParameter(NEWID, this.newId).setParameter(OLDID, this.oldId)
                .executeUpdate();
            return System.nanoTime() - now;
        }

        /**
         * Update object id in a given native field of a given native table.
         *
         * @param name the native name of the table
         * @param field the native name of the column
         * @return the time elapsed during the operation
         */
        public long executeSqlIdUpdate(String name, String field)
        {
            StringBuilder sb = new StringBuilder(128);
            sb.append("UPDATE ").append(name).append(" SET ").append(field).append('=').append(':').append(NEWID)
                .append(" WHERE ").append(field).append('=').append(':').append(OLDID);
            long now = System.nanoTime();
            this.session.createSQLQuery(sb.toString()).setParameter(NEWID, this.newId).setParameter(OLDID, this.oldId)
                .executeUpdate();
            return System.nanoTime() - now;
        }
    }

    /**
     * A callback interface for processing custom mapped classes.
     */
    private interface CustomMappingCallback
    {
        /**
         * Callback to process a custom mapped class.
         *
         * @param store the hibernate store
         * @param name the name of the Xclass
         * @param mapping the custom mapping of the Xclass
         * @param hasDynamicMapping true if dynamic mapping is activated
         * @throws com.xpn.xwiki.XWikiException if an error occurs during processing.
         */
        void processCustomMapping(XWikiHibernateStore store, String name, String mapping, boolean hasDynamicMapping)
            throws XWikiException;
    }

    /** Statistics ids computer. */
    private StatsIdComputer statsIdComputer = new StatsIdComputer();

    /**
     * Logger.
     */
    @Inject
    private Logger logger;

    /** Resolve document names. */
    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> resolver;

    /** Serialize references to identifiers. */
    @Inject
    @Named("local/uid")
    private EntityReferenceSerializer<String> serializer;

    /** Counter for change log rules. */
    private int logCount;

    /** True if migrating MySQL. */
    private boolean isMySQL;

    /** True if migrating MySQL tables using MyISAM engine. */
    private boolean isMySQLMyISAM;

    /** True if migrating Oracle database. */
    private boolean isOracle;

    /** True if migrating Microsoft SQL server database. */
    private boolean isMSSQL;

    /** Tables in which update of foreign keys will be cascade from primary keys by a constraints. */
    private Set<Table> fkTables = new HashSet<>();

    /** Hold the current store configuration. */
    private Metadata metadata;

    @Override
    public String getDescription()
    {
        return "Convert document IDs to use the new improved hash algorithm.";
    }

    @Override
    public XWikiDBVersion getVersion()
    {
        return new XWikiDBVersion(40000);
    }

    /**
     * Log progress of the migration procedure at info level.
     *
     * @param message the message to log
     * @param params some params, that will be inserted using String.format
     */
    private void logProgress(String message, Object... params)
    {
        if (params.length > 0) {
            this.logger.info("[{}] - {}", getName(), String.format(message, params));
        } else {
            this.logger.info("[{}] - {}", getName(), message);
        }
    }

    /**
     * Calls callback for each custom mapped XClass defined. If needed, the mapping is added and injected at the end of
     * the processing into the hibernate session factory.
     *
     * @param store the hibernate store
     * @param callback the callback to be called
     * @param context th current XWikiContext
     * @throws XWikiException when an unexpected error occurs
     */
    private void processCustomMappings(final XWikiHibernateStore store, final CustomMappingCallback callback,
        final XWikiContext context) throws XWikiException
    {
        if (store.executeRead(context, session -> {
            boolean hasProcessedMapping = false;
            try {
                boolean hasDynamicMapping = context.getWiki().hasDynamicCustomMappings();
                SAXReader saxReader = new SAXReader();
                @SuppressWarnings("unchecked")
                List<Object[]> results = session.createQuery("select doc.fullName, doc.xWikiClassXML from "
                    + XWikiDocument.class.getName() + " as doc where (doc.xWikiClassXML like '<%')").list();

                // Inspect all defined classes for custom mapped ones...
                for (Object[] result : results) {
                    String docName = (String) result[0];
                    String classXML = (String) result[1];

                    Element el = saxReader.read(new StringReader(classXML)).getRootElement().element("customMapping");

                    String mapping = (el != null) ? el.getText() : "";

                    if (StringUtils.isEmpty(mapping) && "XWiki.XWikiPreferences".equals(docName)) {
                        mapping = INTERNAL;
                    }

                    if (StringUtils.isNotEmpty(mapping)) {
                        hasProcessedMapping |= (!INTERNAL.equals(mapping) && hasDynamicMapping
                            && store.injectCustomMapping(docName, mapping, context));
                        callback.processCustomMapping(store, docName, mapping, hasDynamicMapping);
                    }
                }
            } catch (Exception e) {
                throw new XWikiException(XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_MIGRATION,
                    getName() + " migration failed", e);
            }
            return hasProcessedMapping;
        })) {
            store.injectUpdatedCustomMappings(context);
        }
    }

    /**
     * Generic procedure to convert identifiers with some protection against conflicting ids.
     *
     * @param map the conversion map
     * @param callback the callback implementing the hibernate actions
     * @throws XWikiException if an error occurs during convertion
     */
    private void convertDbId(final Map<Long, Long> map, IdConversionHibernateCallback callback) throws XWikiException
    {
        int count = map.size() + 1;
        while (!map.isEmpty() && count > map.size()) {
            count = map.size();
            for (Iterator<Map.Entry<Long, Long>> it = map.entrySet().iterator(); it.hasNext();) {
                Map.Entry<Long, Long> entry = it.next();

                if (!map.containsKey(entry.getValue())) {
                    callback.setOldId(entry.getKey());
                    callback.setNewId(entry.getValue());

                    try {
                        getStore().executeWrite(getXWikiContext(), callback);
                    } catch (Exception e) {
                        throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                            XWikiException.ERROR_XWIKI_STORE_MIGRATION,
                            getName() + " migration failed while converting ID from [" + entry.getKey() + "] to ["
                                + entry.getValue() + "]",
                            e);
                    }
                    it.remove();
                }
            }
        }

        if (!map.isEmpty()) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_MIGRATION,
                getName() + " migration failed. Unresolved circular reference during id migration.");
        }
    }

    /**
     * Retrieve the list of table that store collections of the provided persisted class, and that need to be manually
     * updated, since no cascaded update has been added for them.
     *
     * @param pClass the persisted class to analyse
     * @return a list of dual string, the first is the table name, and the second is the key in that table.
     */
    private List<String[]> getCollectionProperties(PersistentClass pClass)
    {
        List<String[]> list = new ArrayList<>();

        if (pClass != null) {
            for (org.hibernate.mapping.Collection coll : getCollection(pClass)) {
                Table collTable = coll.getCollectionTable();
                if (!this.fkTables.contains(collTable)) {
                    list.add(new String[] { collTable.getName(), getKeyColumnName(coll) });
                }
            }
        }

        return list;
    }

    /**
     * Retrieve the list of collection properties of the provided persisted class.
     *
     * @param pClass the persisted class to analyze
     * @return a list of Hibernate collections
     */
    private List<org.hibernate.mapping.Collection> getCollection(PersistentClass pClass)
    {
        List<org.hibernate.mapping.Collection> list = new ArrayList<>();

        if (pClass != null) {
            @SuppressWarnings("unchecked")
            Iterator<Property> it = pClass.getPropertyIterator();
            while (it.hasNext()) {
                Property property = it.next();
                if (property.getType().isCollectionType()) {
                    list.add((org.hibernate.mapping.Collection) property.getValue());
                }
            }
        }

        return list;
    }

    /**
     * get hibernate mapping of the given class or entity name.
     *
     * @param className the class or entity name
     * @return a list of pair of table name and the property field name.
     * @throws DataMigrationException if mapping cannot be found
     */
    private PersistentClass getClassMapping(String className) throws DataMigrationException
    {
        PersistentClass pClass = this.metadata.getEntityBinding(className);

        if (pClass == null) {
            throw new DataMigrationException(
                String.format("Could not migrate IDs for class [%s] : no hibernate mapping found. "
                    + "For example, this error commonly happens if you have copied a document defining an internally "
                    + "mapped class (like XWiki.XWikiPreferences) and never used the newly created class OR if you "
                    + "have forgotten to customize the hibernate mapping while using your own internally custom mapped "
                    + "class. In the first and most common case, to fix this issue and migrate your wiki, you should "
                    + "delete the offending and useless class definition or the whole document defining that class "
                    + "from your original wiki before the migration.", className));
        }

        return pClass;
    }

    /**
     * get name of the first column of the key of a given collection property.
     *
     * @param coll the collection property
     * @return the column name of the key
     */
    private String getKeyColumnName(org.hibernate.mapping.Collection coll)
    {
        return ((Column) coll.getKey().getColumnIterator().next()).getName();
    }

    /**
     * get name of the first column of the key of a given pClass.
     *
     * @param pClass the persistent class
     * @return the column name of the key
     */
    private String getKeyColumnName(PersistentClass pClass)
    {
        return getColumnName(pClass, null);
    }

    /**
     * get column name (first one) of a property of the given pClass.
     *
     * @param pClass the persistent class
     * @param propertyName the name of the property, or null to return the first column of the key
     * @return the column name of the property
     */
    private String getColumnName(PersistentClass pClass, String propertyName)
    {
        if (propertyName != null) {
            return ((Column) pClass.getProperty(propertyName).getColumnIterator().next()).getName();
        }
        return ((Column) pClass.getKey().getColumnIterator().next()).getName();
    }

    @Override
    public void hibernateMigrate() throws DataMigrationException, XWikiException
    {
        final Map<Long, Long> docs = new HashMap<>();
        final List<String> customMappedClasses = new ArrayList<>();
        final Map<Long, Long> objs = new HashMap<>();
        final Queue<Map<Long, Long>> stats = new LinkedList<>();

        // Get ids conversion list
        getStore().executeRead(getXWikiContext(), new HibernateCallback<Object>()
        {
            private void fillDocumentIdConversion(Session session, Map<Long, Long> map)
            {
                String database = getXWikiContext().getWikiId();
                @SuppressWarnings("unchecked")
                List<Object[]> results =
                    session.createQuery("select doc.id, doc.space, doc.name, doc.defaultLanguage, doc.language from "
                        + XWikiDocument.class.getName() + " as doc").list();

                for (Object[] result : results) {
                    long oldId = (Long) result[0];
                    String space = (String) result[1];
                    String name = (String) result[2];
                    String defaultLanguage = (String) result[3];
                    String language = (String) result[4];

                    // Use a real document, since we need the language to be appended.
                    // TODO: Change this when the locale is integrated
                    XWikiDocument doc = new XWikiDocument(new DocumentReference(database, space, name));
                    doc.setDefaultLanguage(defaultLanguage);
                    doc.setLanguage(language);
                    long newId = doc.getId();

                    if (oldId != newId) {
                        map.put(oldId, newId);
                    }
                }

                logProgress("Retrieved %d document IDs to be converted.", map.size());
            }

            private void fillObjectIdConversion(Session session, Map<Long, Long> map)
            {
                @SuppressWarnings("unchecked")
                List<Object[]> results = session.createQuery(
                    "select obj.id, obj.name, obj.className, obj.number from " + BaseObject.class.getName() + " as obj")
                    .list();
                for (Object[] result : results) {
                    long oldId = (Long) result[0];
                    String docName = (String) result[1];
                    String className = (String) result[2];
                    Integer number = (Integer) result[3];

                    BaseObjectReference objRef =
                        new BaseObjectReference(R40000XWIKI6990DataMigration.this.resolver.resolve(className), number,
                            R40000XWIKI6990DataMigration.this.resolver.resolve(docName));
                    long newId = Util.getHash(R40000XWIKI6990DataMigration.this.serializer.serialize(objRef));

                    if (oldId != newId) {
                        map.put(oldId, newId);
                    }
                }

                logProgress("Retrieved %d object IDs to be converted.", map.size());
            }

            private void fillCustomMappingMap(XWikiHibernateStore store, XWikiContext context) throws XWikiException
            {
                processCustomMappings(store, new CustomMappingCallback()
                {
                    @Override
                    public void processCustomMapping(XWikiHibernateStore store, String name, String mapping,
                        boolean hasDynamicMapping) throws XWikiException
                    {
                        if (INTERNAL.equals(mapping) || hasDynamicMapping) {
                            customMappedClasses.add(name);
                        }
                    }
                }, context);

                logProgress("Retrieved %d custom mapped classes to be processed.", customMappedClasses.size());
            }

            private void fillStatsConversionMap(Session session, Class<?> klass, Map<Long, Long> map)
            {
                @SuppressWarnings("unchecked")
                List<Object[]> results = session
                    .createQuery("select stats.id, stats.name, stats.number from " + klass.getName() + " as stats")
                    .list();
                for (Object[] result : results) {
                    long oldId = (Long) result[0];
                    String statsName = (String) result[1];
                    Integer number = (Integer) result[2];

                    // Do not try to convert broken records which would cause duplicated ids
                    if (statsName != null && !statsName.startsWith(".") && !statsName.endsWith(".")) {
                        long newId = R40000XWIKI6990DataMigration.this.statsIdComputer.getId(statsName, number);

                        if (oldId != newId) {
                            map.put(oldId, newId);
                        }
                    } else {
                        R40000XWIKI6990DataMigration.this.logger
                            .debug("Skipping invalid statistical entry [{}] with name [{}]", oldId, statsName);
                    }
                }

                String klassName = klass.getName().substring(klass.getName().lastIndexOf('.') + 1);
                logProgress("Retrieved %d %s statistics IDs to be converted.", map.size(),
                    klassName.substring(0, klassName.length() - 5).toLowerCase());
            }

            @Override
            public Object doInHibernate(Session session) throws XWikiException
            {
                try {
                    fillDocumentIdConversion(session, docs);

                    fillObjectIdConversion(session, objs);

                    // Retrieve custom mapped classes
                    if (getStore() instanceof XWikiHibernateStore) {
                        fillCustomMappingMap((XWikiHibernateStore) getStore(), getXWikiContext());
                    }

                    // Retrieve statistics ID conversion
                    for (Class<?> statsClass : STATS_CLASSES) {
                        Map<Long, Long> map = new HashMap<>();
                        fillStatsConversionMap(session, statsClass, map);
                        stats.add(map);
                    }

                    session.clear();
                } catch (Exception e) {
                    throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                        XWikiException.ERROR_XWIKI_STORE_MIGRATION, getName() + " migration failed", e);
                }
                return null;
            }
        });

        // Cache the metadata
        this.metadata = getStore().getMetadata();

        // Proceed to document id conversion
        if (!docs.isEmpty()) {
            final List<String[]> docsColl = new ArrayList<>();
            for (Class<?> docClass : DOC_CLASSES) {
                docsColl.addAll(getCollectionProperties(getClassMapping(docClass.getName())));
            }
            for (Class<?> docClass : DOCLINK_CLASSES) {
                docsColl.addAll(getCollectionProperties(getClassMapping(docClass.getName())));
            }

            logProgress("Converting %d document IDs in %d tables and %d collection tables...", docs.size(),
                DOC_CLASSES.length + DOCLINK_CLASSES.length, docsColl.size());

            final long[] times = new long[DOC_CLASSES.length + DOCLINK_CLASSES.length + docsColl.size()];
            convertDbId(docs, new AbstractIdConversionHibernateCallback()
            {
                @Override
                public void doSingleUpdate()
                {
                    for (String[] coll : docsColl) {
                        times[this.timer++] += executeSqlIdUpdate(coll[0], coll[1]);
                    }

                    for (Class<?> doclinkClass : DOCLINK_CLASSES) {
                        times[this.timer++] += executeIdUpdate(doclinkClass, DOCID);
                    }
                    times[this.timer++] += executeIdUpdate(XWikiLink.class, DOCID);
                    times[this.timer++] += executeIdUpdate(XWikiRCSNodeInfo.class, ID + '.' + DOCID);
                    times[this.timer++] += executeIdUpdate(XWikiDocument.class, ID);
                }
            });
            if (this.logger.isDebugEnabled()) {
                int timer = 0;
                for (String[] coll : docsColl) {
                    this.logger.debug("Time elapsed for {} collection: {} ms", coll[0], times[timer++] / 1000000);
                }
                for (Class<?> doclinkClass : DOCLINK_CLASSES) {
                    this.logger.debug("Time elapsed for {} class: {} ms", doclinkClass.getName(),
                        times[timer++] / 1000000);
                }
                this.logger.debug("Time elapsed for {} class: {} ms", XWikiRCSNodeInfo.class.getName(),
                    times[timer++] / 1000000);
                this.logger.debug("Time elapsed for {} class: {} ms", XWikiDocument.class.getName(),
                    times[timer++] / 1000000);
            }

            logProgress("All document IDs has been converted successfully.");
        } else {
            logProgress("No document IDs to convert, skipping.");
        }

        // Proceed to object id conversion
        if (!objs.isEmpty()) {
            // Name of classes that need manual updates
            final List<String> classToProcess = new ArrayList<>();
            // Name of custom classes that need manual updates
            final List<String> customClassToProcess = new ArrayList<>();
            // Pair table,key for collection table that need manual updates
            final List<String[]> objsColl = new ArrayList<>();

            objsColl.addAll(getCollectionProperties(getClassMapping(BaseObject.class.getName())));
            for (Class<?> propertyClass : PROPERTY_CLASS) {
                String className = propertyClass.getName();
                PersistentClass klass = getClassMapping(className);

                // Add collection table that will not be updated by cascaded updates
                objsColl.addAll(getCollectionProperties(klass));

                // Skip classes that will be updated by cascaded updates
                if (!this.fkTables.contains(klass.getTable())) {
                    classToProcess.add(className);
                }
            }
            for (String customClass : customMappedClasses) {
                PersistentClass klass = getClassMapping(customClass);

                // Add collection table that will not be updated by cascaded updates
                objsColl.addAll(getCollectionProperties(klass));

                // Skip classes that will be updated by cascaded updates
                if (!this.fkTables.contains(klass.getTable())) {
                    customClassToProcess.add(customClass);
                }
            }

            logProgress("Converting %d object IDs in %d tables, %d custom mapped tables and %d collection tables...",
                objs.size(), classToProcess.size() + 1, customClassToProcess.size(), objsColl.size());

            final long[] times = new long[classToProcess.size() + 1 + customClassToProcess.size() + objsColl.size()];
            convertDbId(objs, new AbstractIdConversionHibernateCallback()
            {
                @Override
                public void doSingleUpdate()
                {
                    for (String[] coll : objsColl) {
                        times[this.timer++] += executeSqlIdUpdate(coll[0], coll[1]);
                    }

                    for (String customMappedClass : customClassToProcess) {
                        times[this.timer++] += executeIdUpdate(customMappedClass, ID);
                    }

                    for (String propertyClass : classToProcess) {
                        times[this.timer++] += executeIdUpdate(propertyClass, IDID);
                    }

                    times[this.timer++] += executeIdUpdate(BaseObject.class, ID);
                }
            });
            if (this.logger.isDebugEnabled()) {
                int timer = 0;
                for (String[] coll : objsColl) {
                    this.logger.debug("Time elapsed for {} collection: {} ms", coll[0], times[timer++] / 1000000);
                }
                for (String customMappedClass : customClassToProcess) {
                    this.logger.debug("Time elapsed for {} custom table: {} ms", customMappedClass,
                        times[timer++] / 1000000);
                }
                for (String propertyClass : classToProcess) {
                    this.logger.debug("Time elapsed for {} property table: {} ms", propertyClass,
                        times[timer++] / 1000000);
                }
                this.logger.debug("Time elapsed for {} class: {} ms", BaseObject.class.getName(),
                    times[timer++] / 1000000);
            }

            logProgress("All object IDs has been converted successfully.");
        } else {
            logProgress("No object IDs to convert, skipping.");
        }

        // Proceed to statistics id conversions
        for (final Class<?> statsClass : STATS_CLASSES) {

            Map<Long, Long> map = stats.poll();
            String klassName = statsClass.getName().substring(statsClass.getName().lastIndexOf('.') + 1);
            klassName = klassName.substring(0, klassName.length() - 5).toLowerCase();

            if (!map.isEmpty()) {
                final List<String[]> statsColl = new ArrayList<>();
                statsColl.addAll(getCollectionProperties(getClassMapping(statsClass.getName())));

                logProgress("Converting %d %s statistics IDs in 1 tables and %d collection tables...", map.size(),
                    klassName, statsColl.size());

                final long[] times = new long[statsColl.size() + 1];
                convertDbId(map, new AbstractIdConversionHibernateCallback()
                {
                    @Override
                    public void doSingleUpdate()
                    {
                        for (String[] coll : statsColl) {
                            times[this.timer++] += executeSqlIdUpdate(coll[0], coll[1]);
                        }
                        times[this.timer++] += executeIdUpdate(statsClass, ID);
                    }
                });
                if (this.logger.isDebugEnabled()) {
                    int timer = 0;
                    for (String[] coll : statsColl) {
                        this.logger.debug("Time elapsed for {} collection: {} ms", coll[0], times[timer++] / 1000000);
                    }
                    this.logger.debug("Time elapsed for {} class: {} ms", statsClass.getName(),
                        times[timer++] / 1000000);
                }

                logProgress("All %s statistics IDs has been converted successfully.", klassName);
            } else {
                logProgress("No %s statistics IDs to convert, skipping.", klassName);
            }
        }
    }

    /**
     * Append a drop primary key constraint command for the given table.
     *
     * @param sb append the result into this string builder
     * @param table the table
     */
    private void appendDropPrimaryKey(StringBuilder sb, Table table)
    {
        final String tableName = table.getName();
        String pkName = table.getPrimaryKey().getName();

        // MS-SQL require a constraints name, and the one provided from the mapping is necessarily appropriate
        // since during database creation, that name has not been used, and a name has been assigned by the
        // database itself. We need to retrieve that name from the schema.
        if (this.isMSSQL) {
            try {
                pkName = getStore().failSafeExecuteRead(getXWikiContext(), session -> {
                    // Retrieve the constraint name from the database
                    return (String) session
                        .createSQLQuery("SELECT CONSTRAINT_NAME FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS"
                            + " WHERE TABLE_NAME = :tableName AND CONSTRAINT_TYPE = 'PRIMARY KEY'")
                        .setParameter("tableName", tableName).uniqueResult();
                });
            } catch (Exception e) {
                // ignored since it is really unlikely to happen
                this.logger.debug("Fail retrieving the primary key constraints name", e);
            }
        }

        sb.append("    <dropPrimaryKey tableName=\"").append(tableName);

        if (pkName != null) {
            sb.append("\"  constraintName=\"").append(pkName);
        }

        sb.append("\"/>\n");
    }

    /**
     * Append a add primary key constraint command for the given table.
     *
     * @param sb append the result into this string builder
     * @param table the table name
     */
    private void appendAddPrimaryKey(StringBuilder sb, Table table)
    {
        PrimaryKey pk = table.getPrimaryKey();
        String pkName = pk.getName();

        sb.append("    <addPrimaryKey tableName=\"").append(table.getName()).append("\"  columnNames=\"");

        Iterator<Column> columns = pk.getColumnIterator();
        while (columns.hasNext()) {
            Column column = columns.next();
            sb.append(column.getName());
            if (columns.hasNext()) {
                sb.append(",");
            }
        }

        if (pkName != null) {
            sb.append("\"  constraintName=\"").append(pkName);
        }

        sb.append("\"/>\n");
    }

    /**
     * Append a drop index command for the given index.
     *
     * @param sb append the result into this string builder
     * @param index the index
     */
    private void appendDropIndex(StringBuilder sb, Index index)
    {
        sb.append("    <dropIndex indexName=\"").append(index.getName()).append("\"  tableName=\"")
            .append(index.getTable().getName()).append("\"/>\n");
    }

    /**
     * Append a add index command for the given index.
     *
     * @param sb append the result into this string builder
     * @param index the index
     */
    private void appendAddIndex(StringBuilder sb, Index index)
    {
        sb.append("    <createIndex tableName=\"").append(index.getTable().getName()).append("\"  indexName=\"")
            .append(index.getName()).append("\">\n");

        Iterator<Column> columns = index.getColumnIterator();
        while (columns.hasNext()) {
            Column column = columns.next();
            sb.append("      <column name=\"").append(column.getName()).append("\"/>\n");
        }

        sb.append("</createIndex>\n");
    }

    /**
     * Append a modify data type to BIGINT command for the given column and table.
     *
     * @param sb append the result into this string builder
     * @param table the table name
     * @param column the column name
     */
    private void appendModifyColumn(StringBuilder sb, String table, String column)
    {
        sb.append("    <modifyDataType tableName=\"").append(table).append("\"  columnName=\"").append(column)
            .append("\" newDataType=\"BIGINT\"/>\n");

        // MS-SQL drop the NOT NULL constraints while modifying datatype, so we add it back
        if (this.isMSSQL) {
            sb.append("    <addNotNullConstraint tableName=\"").append(table).append("\"  columnName=\"").append(column)
                .append("\" columnDataType=\"BIGINT\"/>\n");
        }
    }

    /**
     * Create liquibase change log to modify the column type to BIGINT. If the database is MSSQL, drop PK constraints
     * and indexes during operation.
     *
     * @param sb append the result into this string builder
     * @param table the table name
     * @param column the column name
     */
    private void appendDataTypeChangeLog(StringBuilder sb, Table table, String column)
    {
        String tableName = table.getName();

        sb.append("  <changeSet id=\"R").append(this.getVersion().getVersion()).append('-')
            .append(Util.getHash(String.format("modifyDataType-%s-%s", table, column))).append("\" author=\"xwiki\">\n")
            .append("    <comment>Upgrade identifier [").append(column).append("] from table [").append(tableName)
            .append("] to BIGINT type</comment >\n");

        // MS-SQL require that primary key constraints and all indexes related to the changed column be dropped before
        // changing the column type.
        if (this.isMSSQL) {
            if (table.hasPrimaryKey()) {
                appendDropPrimaryKey(sb, table);
            }

            // We drop all index related to the table, this is overkill, but does not hurt
            for (Iterator<Index> it = table.getIndexIterator(); it.hasNext();) {
                Index index = it.next();
                appendDropIndex(sb, index);
            }
        }

        appendModifyColumn(sb, tableName, column);

        // Add back dropped PK constraints and indexes for MS-SQL
        if (this.isMSSQL) {
            if (table.hasPrimaryKey()) {
                appendAddPrimaryKey(sb, table);
            }

            for (Iterator<Index> it = table.getIndexIterator(); it.hasNext();) {
                Index index = it.next();
                appendAddIndex(sb, index);
            }
        }

        sb.append("  </changeSet>\n");
        this.logCount++;
    }

    /**
     * Append change log to fix identifier type of a given persistent class. Collection table storing properties of this
     * persistent class will also be updated.
     *
     * @param sb the string builder to append to
     * @param pClass the persistent class to process
     */
    private void appendDataTypeChangeLogs(StringBuilder sb, PersistentClass pClass)
    {
        if (pClass != null) {
            appendDataTypeChangeLog(sb, pClass.getTable(), getKeyColumnName(pClass));

            // Update identifiers in ALL collection tables
            for (org.hibernate.mapping.Collection coll : getCollection(pClass)) {
                appendDataTypeChangeLog(sb, coll.getCollectionTable(), getKeyColumnName(coll));
            }
        }
    }

    /**
     * Check that a table contains at least a foreign key that refer to a primary key in its reference table.
     *
     * @param table the table to analyse
     * @return true if the table contains at least a FK that refer to a PK
     */
    private boolean checkFKtoPKinTable(Table table)
    {
        @SuppressWarnings("unchecked")
        Iterator<ForeignKey> fki = table.getForeignKeyIterator();
        while (fki.hasNext()) {
            ForeignKey fk = fki.next();
            if (fk.isReferenceToPrimaryKey()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Retrieve a list of tables used to store the given persistent class, that need to be processed for FK constraints.
     * The list include the main table use to persist the class, if this table has FK, as well as, all the collection
     * table used for storing this persisted class properties.
     *
     * @param pClass the persistent class to analyze
     * @return a list of table
     */
    private List<Table> getForeignKeyTables(PersistentClass pClass)
    {
        List<Table> list = new ArrayList<>();

        if (pClass != null) {
            Table table = pClass.getTable();
            if (checkFKtoPKinTable(table)) {
                list.add(table);
            }

            @SuppressWarnings("unchecked")
            Iterator<Property> it = pClass.getPropertyIterator();
            while (it.hasNext()) {
                Property property = it.next();
                if (property.getType().isCollectionType()) {
                    org.hibernate.mapping.Collection coll = (org.hibernate.mapping.Collection) property.getValue();
                    Table collTable = coll.getCollectionTable();
                    if (checkFKtoPKinTable(collTable)) {
                        list.add(collTable);
                    }
                }
            }
        }

        return list;
    }

    /**
     * Append commands to drop all foreign keys of a given table.
     *
     * @param sb the string builder to append to
     * @param table the table to process
     */
    @SuppressWarnings("unchecked")
    private void appendDropForeignKeyChangeLog(StringBuilder sb, Table table)
    {
        Iterator<ForeignKey> fki = table.getForeignKeyIterator();

        // Preamble
        String tableName = table.getName();
        sb.append("  <changeSet id=\"R").append(this.getVersion().getVersion()).append('-')
            .append(Util.getHash(String.format("dropForeignKeyConstraint-%s", tableName)))
            .append("\" author=\"xwiki\" runOnChange=\"true\" runAlways=\"true\" failOnError=\"false\">\n")
            .append("    <comment>Drop foreign keys on table [").append(tableName).append("]</comment>\n");

        // Concrete Property types should each have a foreign key referencing the BaseProperty
        // Other classes don't have any foreign keys at all, in which case the fast exit path above was used
        while (fki.hasNext()) {
            ForeignKey fk = fki.next();
            // Drop the old constraint
            if (fk.isReferenceToPrimaryKey()) {
                sb.append("    <dropForeignKeyConstraint baseTableName=\"").append(tableName)
                    .append("\" constraintName=\"").append(fk.getName()).append("\" />\n");
            }
        }
        // All done!
        sb.append("  </changeSet>\n");
        this.logCount++;
    }

    /**
     * Append change log to add foreign keys with CASCADEd updates.
     *
     * @param sb the string builder to append to the add tasks
     * @param table the table to process
     */
    @SuppressWarnings("unchecked")
    private void appendAddForeignKeyChangeLog(StringBuilder sb, Table table)
    {
        Iterator<ForeignKey> fki = table.getForeignKeyIterator();

        // Preamble
        String tableName = table.getName();
        sb.append("  <changeSet id=\"R").append(this.getVersion().getVersion()).append('-')
            .append(Util.getHash(String.format("addForeignKeyConstraint-%s", tableName)))
            .append("\" author=\"xwiki\" runOnChange=\"true\" runAlways=\"true\">\n")
            .append("    <comment>Add foreign keys on table [").append(tableName)
            .append("] to use ON UPDATE CASCADE</comment>\n");

        // Concrete Property types should each have a foreign key referencing the BaseProperty
        // Other classes don't have any foreign keys at all, in which case the fast exit path above was used
        while (fki.hasNext()) {
            ForeignKey fk = fki.next();

            if (fk.isReferenceToPrimaryKey()) {
                // Recreate the constraint
                sb.append("    <addForeignKeyConstraint constraintName=\"").append(fk.getName())
                    .append("\" baseTableName=\"").append(tableName).append("\"  baseColumnNames=\"");

                // Reuse the data from the old foreign key
                // Columns in the current table
                Iterator<Column> columns = fk.getColumnIterator();
                while (columns.hasNext()) {
                    Column column = columns.next();
                    sb.append(column.getName());
                    if (columns.hasNext()) {
                        sb.append(",");
                    }
                }
                sb.append("\" referencedTableName=\"").append(fk.getReferencedTable().getName())
                    .append("\" referencedColumnNames=\"");

                // Columns in the referenced table
                columns = fk.getReferencedTable().getPrimaryKey().getColumnIterator();
                while (columns.hasNext()) {
                    Column column = columns.next();
                    sb.append(column.getName());
                    if (columns.hasNext()) {
                        sb.append(",");
                    }
                }

                // The important part: cascaded updates
                if (this.isOracle) {
                    // Oracle doesn't support cascaded updates, but allow the constraint to be checked
                    // at the commit level (normal checking is done at the statement level).
                    sb.append("\" initiallyDeferred=\"true\"/>\n");
                } else {
                    sb.append("\" onUpdate=\"CASCADE\"/>\n");
                }
            }
        }
        // All done!
        sb.append("  </changeSet>\n");
        this.logCount++;
    }

    /**
     * Detect database products and initialize isMySQLMyISAM and isOracle. isMySQLMyISAM is true if the xwikidoc table
     * use the MyISAM engine in MySQL or in MariaDB, false otherwise or on any failure. isOracle is true if the we
     * access an Oracle
     * database.
     *
     * @param store the store to be checked
     */
    private void detectDatabaseProducts(XWikiHibernateBaseStore store)
    {
        DatabaseProduct product = store.getDatabaseProductName();
        if (product != DatabaseProduct.MYSQL && product != DatabaseProduct.MARIADB) {
            this.isOracle = (product == DatabaseProduct.ORACLE);
            this.isMSSQL = (product == DatabaseProduct.MSSQL);
            return;
        }

        this.isMySQL = true;

        String createTable = store.failSafeExecuteRead(getXWikiContext(), session -> {
            NativeQuery<Object[]> query = session.createSQLQuery("SHOW TABLE STATUS like 'xwikidoc'");
            return (String) query.uniqueResult()[1];
        });

        this.isMySQLMyISAM = (createTable != null && createTable.equals("MyISAM"));
    }

    @Override
    public String getLiquibaseChangeLog() throws DataMigrationException
    {
        final XWikiHibernateBaseStore store = getStore();
        this.metadata = store.getMetadata();
        final StringBuilder sb = new StringBuilder(12000);
        final List<PersistentClass> classes = new ArrayList<>();

        detectDatabaseProducts(store);

        if (this.logger.isDebugEnabled()) {
            if (this.isOracle) {
                this.logger
                    .debug("Oracle database detected, proceeding to all updates manually with deferred constraints.");
            }
            if (this.isMySQL && !this.isMySQLMyISAM) {
                this.logger
                    .debug("MySQL or MariaDB innoDB database detected, proceeding to simplified updates with "
                        + "cascaded updates.");
            }
            if (this.isMySQLMyISAM) {
                this.logger
                    .debug("MySQL or MariaDB MyISAM database detected, proceeding to all updates manually without constraints.");
            }
            if (this.isMSSQL) {
                this.logger
                    .debug("Microsoft SQL Server database detected, proceeding to simplified updates with cascaded u"
                        + "pdates. During data type changes, Primary Key constraints and indexes are temporarily dropped.");
            }
        }

        // Build the list of classes to check for updates
        classes.add(getClassMapping(BaseObject.class.getName()));
        for (Class<?> klass : PROPERTY_CLASS) {
            classes.add(getClassMapping(klass.getName()));
        }
        for (Class<?> klass : STATS_CLASSES) {
            classes.add(getClassMapping(klass.getName()));
        }

        // Initialize the counter of Change Logs
        this.logCount = 0;

        // Manual updates of PK and FK will fails if any FK constraints are active on these keys in most decent DBs.
        // Since Hibernate does not activate cascaded updates, we need to rewrite FK constrains. Moreover, some DBs
        // does not allow changing field types of fields involved in FK constraints, so we will drop FK constraints
        // during type updates.
        // Since FK constraints does not fail in MySQL on a MyISAM table, but MyISAM do not support FK constraints, and
        // do not prevent type changes, we skip all this processing for MySQL table stored using the MyISAM engine.
        if (!this.isMySQLMyISAM) {
            for (PersistentClass klass : classes) {
                this.fkTables.addAll(getForeignKeyTables(klass));
            }
        }

        // Drop all FK constraints
        for (Table table : this.fkTables) {
            appendDropForeignKeyChangeLog(sb, table);
        }

        // Process internal classes
        for (PersistentClass klass : classes) {
            // The same table mapped for StringListProperty and LargeStringProperty
            if (klass.getMappedClass() != StringListProperty.class) {
                // Update key types
                appendDataTypeChangeLogs(sb, klass);
            }
        }

        // Process dynamic and custom mapping
        final XWikiContext context = getXWikiContext();

        try {
            processCustomMappings((XWikiHibernateStore) store, new CustomMappingCallback()
            {
                @Override
                public void processCustomMapping(XWikiHibernateStore store, String name, String mapping,
                    boolean hasDynamicMapping) throws XWikiException
                {
                    if (INTERNAL.equals(mapping) || hasDynamicMapping) {
                        PersistentClass klass = R40000XWIKI6990DataMigration.this.metadata.getEntityBinding(name);
                        if (!R40000XWIKI6990DataMigration.this.isMySQLMyISAM) {
                            List<Table> tables = getForeignKeyTables(klass);
                            for (Table table : tables) {
                                if (!R40000XWIKI6990DataMigration.this.fkTables.contains(table)) {
                                    // Drop FK constraints for custom mapped class
                                    appendDropForeignKeyChangeLog(sb, table);
                                    R40000XWIKI6990DataMigration.this.fkTables.add(table);
                                }
                            }
                        }

                        // Update key types for custom mapped class
                        appendDataTypeChangeLogs(sb, klass);
                    }
                }
            }, context);
        } catch (XWikiException e) {
            throw new DataMigrationException("Unable to process custom mapped classes during schema updated", e);
        }

        // Add FK constraints back, activating cascaded updates
        for (Table table : this.fkTables) {
            appendAddForeignKeyChangeLog(sb, table);
        }

        // Oracle doesn't support cascaded updates, so we still need to manually update each table
        if (this.isOracle) {
            this.fkTables.clear();
        }

        logProgress("%d schema updates required.", this.logCount);
        if (this.logger.isDebugEnabled()) {
            this.logger.debug("About to execute this Liquibase XML: {}", sb.toString());
        }
        return sb.toString();
    }
}
