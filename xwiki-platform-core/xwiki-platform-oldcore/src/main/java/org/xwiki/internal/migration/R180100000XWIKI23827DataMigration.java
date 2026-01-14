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
package org.xwiki.internal.migration;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.PasswordProperty;
import com.xpn.xwiki.objects.StringProperty;
import com.xpn.xwiki.objects.classes.PasswordClass;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.XWikiDBVersion;
import com.xpn.xwiki.store.migration.hibernate.AbstractHibernateDataMigration;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

/**
 * Migration for moving password properties that used to be stored as StringProperty in a dedicated PasswordProperty
 * table.
 * The migration operation is done in following steps:
 * <ol>
 *     <li>
 *         We search for all XClass containing at least one PasswordClass property (by checking the xclass XML) and
 *     their associated objects
 *     </li>
 *     <li>
 *         We load the xclass doc to find the name of the properties being PasswordClass
 *     </li>
 *     <li>
 *         We then query the StringProperty table to retrieve all properties
 *     </li>
 *     <li>
 *         Finally we perform the couple deletion of the old property / insertion of the new property
 *     </li>
 * </ol>
 *
 * @version $Id$
 * @since 18.0.0RC1
 */
@Component
@Singleton
@Named("180100000XWIKI23827")
public class R180100000XWIKI23827DataMigration extends AbstractHibernateDataMigration
{
    // We limit to 100 as each occurrence in the batch adds a where statement such as:
    // (prop.id.id = :objectId_0 and prop.id.name = :property_0)
    private static final int BATCH_SIZE = 100;

    private static final String SELECT_PROPERTIES_STATEMENT = "select prop from StringProperty as prop "
        + "where prop.id.name in :propNames and prop.id.id in :objectIds";

    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @Inject
    private Logger logger;

    @Override
    public String getDescription()
    {
        return "Migrate passwords values to a new dedicated table.";
    }

    @Override
    public XWikiDBVersion getVersion()
    {
        return new XWikiDBVersion(180100000);
    }
    
    private static class XClassWithPasswordProperties
    {
        private final String className;
        private final List<String> properties;
        private final List<Long> objectIds;

        XClassWithPasswordProperties(String className)
        {
            this.className = className;
            this.properties = new ArrayList<>();
            this.objectIds = new ArrayList<>();
        }

        String getClassName()
        {
            return this.className;
        }

        void addProperty(String property)
        {
            this.properties.add(property);
        }

        void addObjectIds(List<Long> objectIds)
        {
            this.objectIds.addAll(objectIds);
        }

        public List<String> getProperties()
        {
            return properties;
        }

        public List<Long> getObjectIds()
        {
            return objectIds;
        }
    }

    @Override
    protected void hibernateMigrate() throws DataMigrationException, XWikiException
    {
        List<XClassWithPasswordProperties> xClassWithPasswordPropertiesList = getXClassWithPasswordProperties();
        for (XClassWithPasswordProperties xclass : xClassWithPasswordPropertiesList) {
            retrieveXObjectsIds(xclass);
            if (!xclass.getObjectIds().isEmpty()) {
                handlePasswordPropertiesValues(xclass);
            }
        }
    }

    private List<XClassWithPasswordProperties> getXClassWithPasswordProperties()
        throws XWikiException, DataMigrationException
    {
        XWiki wiki = getXWikiContext().getWiki();
        String passwordXClassQuery = "select doc.fullName"
            + "from XWikiDocument doc"
            + "where doc.xWikiClassXML like "
            + "'%<classType>com.xpn.xwiki.objects.classes.PasswordClass</classType>%' "
            + "order by doc.fullName";
        List<String> results;
        try {
            results = wiki.getStore().getQueryManager()
                .createQuery(passwordXClassQuery, Query.HQL)
                .execute();
        } catch (QueryException e) {
            throw new DataMigrationException("Error while trying to query xclass containing PasswordProperty fields",
                e);
        }

        List<XClassWithPasswordProperties> xClassWithPasswordPropertiesList = new ArrayList<>();
        for (String className : results) {
            XClassWithPasswordProperties xClassWithPasswordProperties = new XClassWithPasswordProperties(className);
            DocumentReference xclassDocReference = this.documentReferenceResolver.resolve(className);

            // we load the doc, get its xclass, iterate over the fields and memorize the names of password fields
            // FIXME: ensure to invalidate the doc in cache
            XWikiDocument document = wiki.getDocument(xclassDocReference, getXWikiContext());
            for (Object field : document.getXClass().getFieldList()) {
                if (field instanceof PasswordClass passwordField) {
                    xClassWithPasswordProperties.addProperty(passwordField.getName());
                }
            }
            // ensure to avoid false positives
            if (!xClassWithPasswordProperties.getProperties().isEmpty()) {
                xClassWithPasswordPropertiesList.add(xClassWithPasswordProperties);
            }
        }
        this.logger.info("[{}] different xclass found containing password properties values to migrate found.",
            xClassWithPasswordPropertiesList.size());
        return xClassWithPasswordPropertiesList;
    }

    private void retrieveXObjectsIds(XClassWithPasswordProperties xClassWithPasswordProperties)
        throws DataMigrationException
    {
        XWiki wiki = getXWikiContext().getWiki();
        String className = xClassWithPasswordProperties.getClassName();
        String objectIdsQuery =  "select obj.id "
            + "from BaseObject as obj "
            + "where obj.className = :className "
            + "order by obj.id";
        List<Long> results;
        try {
            results = wiki.getStore().getQueryManager()
                .createQuery(objectIdsQuery, Query.HQL)
                .bindValue("className", className)
                .execute();
        } catch (QueryException e) {
            throw new DataMigrationException(
                String.format("Error while trying to get xobject ids for xclass [%s]", className),
                e);
        }
        xClassWithPasswordProperties.addObjectIds(results);
        logger.info("[{}] objects to migrate found for xclass [{}].", results.size(), className);
    }

    private void handlePasswordPropertiesValues(XClassWithPasswordProperties xClassWithPasswordProperties)
        throws XWikiException
    {
        int objectNumbers = xClassWithPasswordProperties.getObjectIds().size();
        int propertyNumbers = xClassWithPasswordProperties.getProperties().size();

        logger.info("Starting migration of [{}] objects containing [{}] password properties from xclass [{}].",
            objectNumbers,
            propertyNumbers,
            xClassWithPasswordProperties.getClassName());

        // The size of the number of xobject we deal with is defined by the total batch size we have and the number of
        // properties we want to deal with, to avoid having a too longer where statement.
        // So if the xclass contains a single password property the number of xobjects we deal with in a single update
        // is BATCH_SIZE.
        // For 2 properties, it's BATCH_SIZE / 2, etc.
        int objectBatchSize = Math.max(BATCH_SIZE / propertyNumbers, 1);
        int objectIndex = 0;

        do {
            objectIndex = migrateProperties(xClassWithPasswordProperties, objectIndex, objectBatchSize);
        } while (objectIndex < objectNumbers);
    }

    private int migrateProperties(XClassWithPasswordProperties xclassWithPasswordProperties, int originalObjectIndex,
        int objectBatchSize) throws XWikiException
    {

        XWikiContext context = getXWikiContext();
        XWikiHibernateStore hibernateStore = context.getWiki().getHibernateStore();
        hibernateStore.beginTransaction(context);
        Session session = hibernateStore.getSession(context);
        int objectNumbers = xclassWithPasswordProperties.getObjectIds().size();
        int objectEndIndex = Math.min(originalObjectIndex + objectBatchSize, objectNumbers);
        List<Long> objectsIdsToMigrate = new ArrayList<>(xclassWithPasswordProperties.getObjectIds()
            .subList(originalObjectIndex, objectEndIndex));

        org.hibernate.query.Query<StringProperty> query =
            session.createQuery(SELECT_PROPERTIES_STATEMENT, StringProperty.class);
        query.setParameter("propNames", xclassWithPasswordProperties.getProperties());
        query.setParameter("objectIds", objectsIdsToMigrate);

        for (StringProperty stringProperty : query.getResultList()) {
            PasswordProperty passwordProperty = new PasswordProperty();
            passwordProperty.setName(stringProperty.getName());
            passwordProperty.setId(stringProperty.getId());
            passwordProperty.setValue(stringProperty.getValue());
            session.delete(stringProperty);
            session.save(passwordProperty);
        }

        hibernateStore.endTransaction(context, true);
        logger.info("[{}] objects migrated on [{}] for xclass [{}].", objectEndIndex, objectNumbers,
            xclassWithPasswordProperties.getClassName());
        return objectEndIndex;
    }
}
