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
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
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
import com.xpn.xwiki.store.XWikiHibernateBaseStore.HibernateCallback;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.XWikiDBVersion;
import com.xpn.xwiki.util.Util;

/**
 * Migration for XWIKI-6990 Reduce the likelihood of having same (hibernate) document id for different documents.
 * This data migration convert document ID to a new hash algorithm.
 *
 * @version $Id$
 * @since 4.0M1
 */
@Component
@Named("R40000XWIKI6990")
public class R40000XWIKI6990DataMigration extends AbstractHibernateDataMigration
{
    /** Document classes to migrate.*/
    private static final Class<?>[] DOC_CLASSES = new Class<?>[] {
        XWikiDocument.class,
        XWikiRCSNodeInfo.class,
    };

    /** Document related classes to migrate. (DOCID is the document identifier) */
    private static final Class<?>[] DOCLINK_CLASSES = new Class<?>[] {
        XWikiLink.class,
        XWikiAttachment.class,
        DeletedAttachment.class
    };

    /** Property classes to migrate. (ID is the object identifier) */
    private static final Class<?>[] PROPERTY_CLASS = new Class<?>[] {
        DateProperty.class,
        DBStringListProperty.class,
        DoubleProperty.class,
        FloatProperty.class,
        IntegerProperty.class,
        LargeStringProperty.class,
        LongProperty.class,
        StringListProperty.class,
        StringProperty.class,
        BaseProperty.class
    };

    /** Statistics classes to migrate. (ID is the stats identifier) */
    private static final Class<?>[] STATS_CLASSES = new Class<?>[] {
        DocumentStats.class,
        RefererStats.class,
        VisitStats.class
    };

    /** Mark internal mapping. */
    private static final String INTERNAL = "internal";

    /** Stub statistic class used to compute new ids from existing objects. */
    private static class StatsIdComputer extends XWikiStats
    {
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
            return name;
        }

        @Override
        public int getNumber()
        {
            return number;
        }
    }

    /**
     * Specialized HibernateCallback for id conversion.
     */
    private interface IdConversionHibernateCallback extends HibernateCallback<Object>
    {
        /**
         * Set the new identifier.
         * @param newId the new identifier
         */
        void setNewId(long newId);

        /**
         * Set the old identifier.
         * @param oldId the old identifier
         */
        void setOldId(long oldId);
    }
    
    /**
     * Base implementation of the hibernate callback to convert identifier.
     */
    private abstract static class AbstractIdConversionHibernateCallback implements IdConversionHibernateCallback
    {
        /** Name for the id column. */
        public static final String ID = "id";
        
        /** Name for the subid column. */
        public static  final String IDID = "id.id";
        
        /** Name for the docid column. */
        public static final String DOCID = "docId";

        /** Place holder for new id. */
        private static final String NEWID = "newid";

        /** Place holder for old id. */
        private static final String OLDID = "oldid";

        /** The old identifier. */
        private long oldId;

        /** The new identifier. */
        private long newId;

        /** The new identifier. */
        private Session session;

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
        public Object doInHibernate(Session session)
        {
            this.session = session;
            doUpdate();
            this.session = null;
            return null;
        }

        /**
         * Implement this method to execute updates using {@code executeIdUpdate()}.
         */
        public abstract void doUpdate();

        /**
         * Update object id in a given field for a given object class.
         * 
         * @param klass the class of the persisted object
         * @param field the field name of the persisted object
         */
        public void executeIdUpdate(Class<?> klass, String field)
        {
            executeIdUpdate(klass.getName(), field);
        }
        
        /**
         * Update object id in a given field of a given table.
         *
         * @param name the entity name of the table
         * @param field the field name of the column
         */
        public void executeIdUpdate(String name, String field)
        {
            StringBuilder sb = new StringBuilder(128);
            sb.append("update ").append(name)
                .append(" klass set klass.").append(field).append('=').append(':').append(NEWID)
                .append(" where klass.").append(field).append('=').append(':').append(OLDID);
            session.createQuery(sb.toString())
                .setLong(NEWID, newId)
                .setLong(OLDID, oldId)
                .executeUpdate();
        }

        /**
         * Update object id in a given native field of a given native table.
         *
         * @param name the native name of the table
         * @param field the native name of the column
         */
        public void executeSqlIdUpdate(String name, String field)
        {
            StringBuilder sb = new StringBuilder(128);
            sb.append("UPDATE ").append(name)
                .append(" SET ").append(field).append('=').append(':').append(NEWID)
                .append(" WHERE ").append(field).append('=').append(':').append(OLDID);
            session.createSQLQuery(sb.toString())
                .setLong(NEWID, newId)
                .setLong(OLDID, oldId)
                .executeUpdate();
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
        void processCustomMapping(XWikiHibernateStore store, String name, String mapping,
            boolean hasDynamicMapping) throws XWikiException;
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
            logger.info("[{}] - {}", getName(), String.format(message, params));
        } else {
            logger.info("[{}] - {}", getName(), message);
        }
    }

    /**
     * Calls callback for each custom mapped XClass defined. If needed, the mapping is added and
     * injected at the end of the processing into the hibernate session factory.
     * 
     * @param store the hibernate store
     * @param callback the callback to be called
     * @param context th current XWikiContext
     * @return the ORed boolean result of the callbacks
     * @throws DataMigrationException when an migration error occurs
     * @throws XWikiException when an unexpected error occurs
     */
    private void processCustomMappings(final XWikiHibernateStore store, final CustomMappingCallback callback,
        final XWikiContext context) 
        throws DataMigrationException, XWikiException
    {
        if (store.executeRead(context, true, new HibernateCallback<Boolean>()
            {
                @Override
                @SuppressWarnings("unchecked")
                public Boolean doInHibernate(Session session) throws XWikiException
                {
                    boolean hasProcessedMapping = false;
                    try {
                        boolean hasDynamicMapping = context.getWiki().hasDynamicCustomMappings();
                        SAXReader saxReader = new SAXReader();

                        // Inspect all defined classes for custom mapped ones...
                        for (Object[] result : (List<Object[]>) (session.createQuery(
                            "select doc.fullName, doc.xWikiClassXML from " + XWikiDocument.class.getName()
                                + " as doc where (doc.xWikiClassXML like '<%')")
                            .list()))
                        {
                            String docName = (String) result[0];
                            String classXML = (String) result[1];

                            Element el = saxReader.read(new StringReader(classXML)).getRootElement()
                                .element("customMapping");

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
                        throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                            XWikiException.ERROR_XWIKI_STORE_MIGRATION, getName() + " migration failed", e);
                    }
                    return hasProcessedMapping;
                }
            }))
        {
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
                        getStore().executeWrite(getXWikiContext(), true, callback);
                    } catch (Exception e) {
                        throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                            XWikiException.ERROR_XWIKI_STORE_MIGRATION, getName() + " migration failed", e);
                    }
                    it.remove();
                }
            }
        }

        if (!map.isEmpty()) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                XWikiException.ERROR_XWIKI_STORE_MIGRATION,
                getName() + " migration failed. Unresolved circular reference during id migration.");
        }
    }
    
    private List<String[]> getCollectionProperties(PersistentClass pClass) {
        List<String[]> list = new ArrayList<String[]>();

        if (pClass != null) {
            Iterator it = pClass.getPropertyIterator();
            while (it.hasNext()) {
                Property property = (Property) it.next();
                if (property.getType().isCollectionType()) {
                    org.hibernate.mapping.Collection coll = (org.hibernate.mapping.Collection) property.getValue();
                    list.add(new String[]{ coll.getCollectionTable().getName(), 
                        ((Column) coll.getKey().getColumnIterator().next()).getName() });
                }
            }
        }
        
        return list;
    }
    
    @Override
    public void hibernateMigrate() throws DataMigrationException, XWikiException
    {
        final Map<Long, Long> docs = new HashMap<Long, Long>();
        final List<String> customMappedClasses = new ArrayList<String>();
        final Map<Long, Long> objs = new HashMap<Long, Long>();
        final Queue<Map<Long, Long>> stats = new LinkedList<Map<Long, Long>>();
        
        // Get ids conversion list
        getStore().executeRead(getXWikiContext(), true, new HibernateCallback<Object>()
        {
            @SuppressWarnings("unchecked")
            private void fillDocumentIdConversion(Session session, Map<Long, Long> map)
            {
                String database = getXWikiContext().getDatabase();

                for (Object[] result : (List<Object[]>) (session.createQuery(
                    "select doc.id, doc.space, doc.name, doc.defaultLanguage, doc.language from "
                        + XWikiDocument.class.getName() + " as doc")
                    .list()))
                {
                    long oldId = (Long) result[0];
                    String space = (String) result[1];
                    String name = (String) result[2];
                    String defaultLanguage = (String) result[3];
                    String language = (String) result[4];

                    // Use a real document, since we need the language to be appended.
                    // TODO: Change this when the locale is integrated
                    XWikiDocument doc =
                        new XWikiDocument(new DocumentReference(database, space, name));
                    doc.setDefaultLanguage(defaultLanguage);
                    doc.setLanguage(language);
                    long newId = doc.getId();

                    if (oldId != newId) {
                        map.put(oldId, newId);
                    }
                }
                
                logProgress("Retrieved %d document IDs to be converted.",map.size());
            }

            @SuppressWarnings("unchecked")
            private void fillObjectIdConversion(Session session, Map<Long, Long> map)
            {
                for (Object[] result : (List<Object[]>) (session.createQuery(
                    "select obj.id, obj.name, obj.className, obj.number from " + BaseObject.class.getName()
                        + " as obj")
                    .list()))
                {
                    long oldId = (Long) result[0];
                    String docName = (String) result[1];
                    String className = (String) result[2];
                    Integer number = (Integer) result[3];

                    BaseObjectReference objRef = new BaseObjectReference(
                        resolver.resolve(className), number, resolver.resolve(docName));
                    long newId = Util.getHash(serializer.serialize(objRef));

                    if (oldId != newId) {
                        map.put(oldId, newId);
                    }
                }

                logProgress("Retrieved %d object IDs to be converted.",map.size());
            }

            private void fillCustomMappingMap(XWikiHibernateStore store, XWikiContext context)
                throws XWikiException, DataMigrationException
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

            @SuppressWarnings("unchecked")
            private void fillStatsConversionMap(Session session, Class<?> klass, Map<Long, Long> map)
            {
                for (Object[] result : (List<Object[]>) (session.createQuery(
                    "select stats.id, stats.name, stats.number from " + klass.getName() + " as stats")
                    .list()))
                {
                    long oldId = (Long) result[0];
                    String statsName = (String) result[1];
                    Integer number = (Integer) result[2];

                    long newId = statsIdComputer.getId(statsName, number);

                    if (oldId != newId) {
                        map.put(oldId, newId);
                    }
                }

                String klassName = klass.getName().substring(klass.getName().lastIndexOf('.') + 1);            
                logProgress("Retrieved %d %s statistics IDs to be converted.", map.size(),
                    klassName.substring(0, klassName.length() - 5).toLowerCase());
            }

            @Override
            @SuppressWarnings("unchecked")
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
                        Map<Long, Long> map = new HashMap<Long, Long>();
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

        Configuration configuration = getStore().getConfiguration();

        // Proceed to document id conversion
        if (!docs.isEmpty()) {
            final List<String[]> docsColl = new ArrayList<String[]>();
            for (Class<?> docClass : DOC_CLASSES) {
                docsColl.addAll(getCollectionProperties(configuration.getClassMapping(docClass.getName())));
            }
            for (Class<?> docClass : DOCLINK_CLASSES) {
                docsColl.addAll(getCollectionProperties(configuration.getClassMapping(docClass.getName())));
            }
    
            logProgress("Converting %d document IDs in %d tables and %d collection tables...", 
                docs.size(), DOC_CLASSES.length + DOCLINK_CLASSES.length, docsColl.size());
            convertDbId(docs, new AbstractIdConversionHibernateCallback()
            {
                @Override
                public void doUpdate()
                {
                    for (String[] coll : docsColl) {
                        executeSqlIdUpdate(coll[0], coll[1]);
                    }
    
                    for (Class<?> doclinkClass : DOCLINK_CLASSES) {
                        executeIdUpdate(doclinkClass, DOCID);
                    }
                    executeIdUpdate(XWikiRCSNodeInfo.class, ID + '.' + DOCID);
                    executeIdUpdate(XWikiDocument.class, ID);
                }
            });
            logProgress("All document IDs has been converted successfully.");
        } else {
            logProgress("No document IDs to convert, skipping.");
        }

        // Proceed to object id conversion
        if (!objs.isEmpty()) {
            final List<String[]> objsColl = new ArrayList<String[]>();
            objsColl.addAll(getCollectionProperties(configuration.getClassMapping(BaseObject.class.getName())));
            for (Class<?> propertyClass : PROPERTY_CLASS) {
                objsColl.addAll(getCollectionProperties(configuration.getClassMapping(propertyClass.getName())));
            }
            for (String customClass : customMappedClasses) {
                objsColl.addAll(getCollectionProperties(configuration.getClassMapping(customClass)));
            }
    
            logProgress("Converting %d object IDs in %d tables, %d custom mapped tables and %d collection tables...",
                objs.size(), PROPERTY_CLASS.length + 1, customMappedClasses.size(), objsColl.size());
            convertDbId(objs, new AbstractIdConversionHibernateCallback()
            {
                @Override
                public void doUpdate()
                {
                    for (String[] coll : objsColl) {
                        executeSqlIdUpdate(coll[0], coll[1]);
                    }
    
                    for (String customMappedClass : customMappedClasses) {
                        executeIdUpdate(customMappedClass, ID);
                    }
    
                    for (Class<?> propertyClass : PROPERTY_CLASS) {
                        executeIdUpdate(propertyClass, IDID);
                    }
    
                    executeIdUpdate(BaseObject.class, ID);
                }
            });
            logProgress("All object IDs has been converted successfully.");
        } else {
            logProgress("No object IDs to convert, skipping.");
        }

        // Proceed to statistics id conversions
        for (final Class<?> statsClass : STATS_CLASSES) {
            Map<Long,Long> map = stats.poll();
            String klassName = statsClass.getName().substring(statsClass.getName().lastIndexOf('.') + 1);
            klassName = klassName.substring(0, klassName.length() - 5).toLowerCase();

            if (!map.isEmpty()) {
                final List<String[]> statsColl = new ArrayList<String[]>();
                statsColl.addAll(getCollectionProperties(configuration.getClassMapping(statsClass.getName())));

                logProgress("Converting %d %s statistics IDs in 1 tables and %d collection tables...",
                    map.size(), klassName, statsColl.size());
                convertDbId(stats.poll(), new AbstractIdConversionHibernateCallback()
                {
                    @Override
                    public void doUpdate()
                    {
                        for (String[] coll : statsColl) {
                            executeSqlIdUpdate(coll[0], coll[1]);
                        }
                        executeIdUpdate(statsClass, ID);
                    }
                });
                logProgress("All %s statistics IDs has been converted successfully.", klassName);
            } else {
                logProgress("No %s statistics IDs to convert, skipping.", klassName);
            }
        }
    }
    
    /**
     * Create liquibase change log to modify column type to BIGINT (except for Oracle).
     * 
     * @param sb append the result into this string builder
     * @param table the table name
     * @param column the column name
     */
    private void appendModifyColumn(StringBuilder sb, String table, String column)
    {
        sb.append("  <changeSet id=\"R").append(this.getVersion().getVersion())
            .append("-").append(String.format("%03d", logCount++)).append("\" author=\"dgervalle\">\n")
            .append("    <preConditions onFail=\"MARK_RAN\"><not><dbms type=\"oracle\" /></not></preConditions>\n")
            .append("    <comment>Upgrade identifier [").append(column).append("] from table [").append(table)
            .append("] to BIGINT type</comment >\n")
            .append("    <modifyDataType tableName=\"").append(table)
            .append("\"  columnName=\"").append(column)
            .append("\" newDataType=\"BIGINT\"/>\n")
            .append("  </changeSet>");
    }

    /**
     * Append change log to fix identifier type of a given persistent class.
     * 
     * @param sb the string builder to append to
     * @param pClass the persistent class to process
     */
    private void appendClassChangeLog(StringBuilder sb, PersistentClass pClass)
    {
        if (pClass != null) {
            appendModifyColumn(sb, pClass.getTable().getName(),
                ((Column) pClass.getKey().getColumnIterator().next()).getName());

            for (String[] collProp : getCollectionProperties(pClass)) {
                appendModifyColumn(sb, collProp[0], collProp[1]);
            }
        }
    }

    @Override
    public String getLiquibaseChangeLog() throws DataMigrationException
    {
        final Configuration configuration = getStore().getConfiguration();
        final StringBuilder sb = new StringBuilder(6000);
        final List<Class<?>> classes = new ArrayList<Class<?>>();
        
        classes.add(BaseObject.class);
        Collections.addAll(classes, PROPERTY_CLASS);
        Collections.addAll(classes, STATS_CLASSES);
        
        logCount = 0;

        // Process internal classes
        for (Class<?> klass : classes) {
            appendClassChangeLog(sb, configuration.getClassMapping(klass.getName()));
        }

        // Process dynamic and custom mapping
        final XWikiContext context = getXWikiContext();

        try {
            processCustomMappings((XWikiHibernateStore) getStore(), new CustomMappingCallback()
            {
                @Override
                public void processCustomMapping(XWikiHibernateStore store, String name, String mapping,
                    boolean hasDynamicMapping) throws XWikiException
                {
                    if (INTERNAL.equals(mapping) || hasDynamicMapping) {
                        appendClassChangeLog(sb, configuration.getClassMapping(name));
                    }
                }
            }, context);
        } catch (XWikiException e) {
            throw new DataMigrationException("Unable to process custom mapped classes during schema updated", e);
        }

        logProgress("%d schema updates required.", logCount);
        return sb.toString();
    }
}
