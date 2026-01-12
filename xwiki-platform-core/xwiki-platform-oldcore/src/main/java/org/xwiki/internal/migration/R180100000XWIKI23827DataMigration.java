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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    private static final String OR_STATEMENT = " or ";
    private static final String SELECT_PROPERTIES_STATEMENT = "select prop from StringProperty as prop where %s";

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
    
    private static class PasswordPropertyValues
    {
        private final String className;
        private final List<String> properties;
        private final List<Long> objectIds;

        PasswordPropertyValues(String className)
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

        void addObjectId(Long objectId)
        {
            this.objectIds.add(objectId);
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
        List<PasswordPropertyValues> passwordPropertyValuesList = getPasswordPropertiesValues();
        for (PasswordPropertyValues passwordPropertyValues : passwordPropertyValuesList) {
            handlePasswordPropertiesValues(passwordPropertyValues);
        }
    }

    private List<PasswordPropertyValues> getPasswordPropertiesValues() throws XWikiException, DataMigrationException
    {
        XWiki wiki = getXWikiContext().getWiki();
        // Get XClass containing a Password Field and all objects of that xclass
        // TODO: should we limit? What if there's a very big number of objects?
        String passwordXClassQuery = "select doc.fullName, obj.id "
            + "from XWikiDocument doc, BaseObject as obj "
            + "where obj.className = doc.fullName and doc.xWikiClassXML like "
            + "'%<classType>com.xpn.xwiki.objects.classes.PasswordClass</classType>%' "
            + "order by doc.fullName";
        List<Object[]> results;
        try {
            results = wiki.getStore().getQueryManager()
                .createQuery(passwordXClassQuery, Query.HQL)
                .execute();
        } catch (QueryException e) {
            throw new DataMigrationException("Error while trying to query xclass containing PasswordProperty fields",
                e);
        }

        // this list will contain one occurence of PasswordPropertyValues per xclass
        List<PasswordPropertyValues> passwordPropertyValuesList = new ArrayList<>();
        PasswordPropertyValues passwordPropertyValues = null;
        for (Object[] result : results) {
            // it's an xclass we haven't visited yet so we're looking for the password properties
            // TODO: right now it only works because we don't paginate results
            if (passwordPropertyValues == null || !passwordPropertyValues.getClassName().equals(result[0])) {
                passwordPropertyValues = new PasswordPropertyValues(String.valueOf(result[0]));
                passwordPropertyValuesList.add(passwordPropertyValues);
                DocumentReference xclassDocReference =
                    this.documentReferenceResolver.resolve(passwordPropertyValues.getClassName());

                // we load the doc, get its xclass, iterate over the fields and memorize the names of password fields
                XWikiDocument document = wiki.getDocument(xclassDocReference, getXWikiContext());
                for (Object field : document.getXClass().getFieldList()) {
                    if (field instanceof PasswordClass passwordField) {
                        passwordPropertyValues.addProperty(passwordField.getName());
                    }
                }
            }
            // we then memorize the xobjects ids for that xclass
            passwordPropertyValues.addObjectId((Long) result[1]);
        }
        this.logger.info("[{}] xobjects containing password properties related to [{}] different xclass "
            + "to migrate found.", results.size(), passwordPropertyValuesList.size());
        return passwordPropertyValuesList;
    }

    private void handlePasswordPropertiesValues(PasswordPropertyValues passwordPropertyValues)
        throws XWikiException
    {
        int objectNumbers = passwordPropertyValues.getObjectIds().size();
        int propertyNumbers = passwordPropertyValues.getProperties().size();

        logger.info("Starting migration of [{}] objects containing [{}] password properties from xclass [{}].",
            objectNumbers,
            propertyNumbers,
            passwordPropertyValues.getClassName());

        // We build once for all the where statement parts with properties, that we will reuse for all objects.
        int loop = 0;
        StringBuilder propertyStatementBuilder = new StringBuilder();
        Map<String, Object> propertyBindings = new HashMap<>();
        for (String property : passwordPropertyValues.getProperties()) {
            String propertyBindingName = String.format("property_%s", loop);
            propertyBindings.put(propertyBindingName, property);
            propertyStatementBuilder.append("(prop.id.id = :%1$s and prop.id.name = :");
            propertyStatementBuilder.append(propertyBindingName);
            propertyStatementBuilder.append(")");
            loop++;

            if (loop < propertyNumbers) {
                propertyStatementBuilder.append(OR_STATEMENT);
            }
        }

        String propertyStatement = propertyStatementBuilder.toString();

        // The size of the number of xobject we deal with is defined by the total batch size we have and the number of
        // properties we want to deal with, to avoid having a too longer where statement.
        // So if the xclass contains a single password property the number of xobjects we deal with in a single update
        // is BATCH_SIZE.
        // For 2 properties, it's BATCH_SIZE / 2, etc.
        int objectBatchSize = Math.max(BATCH_SIZE / propertyNumbers, 1);
        int objectIndex = 0;

        do {
            objectIndex = migrateProperties(passwordPropertyValues, propertyStatement, propertyBindings, objectIndex,
                objectBatchSize);
        } while (objectIndex < objectNumbers);
    }

    private int migrateProperties(PasswordPropertyValues passwordPropertyValues, String propertyStatement,
        Map<String, Object> propertyBindings, int originalObjectIndex, int objectBatchSize) throws XWikiException
    {
        int objectIndex = originalObjectIndex;
        XWikiContext context = getXWikiContext();
        XWikiHibernateStore hibernateStore = context.getWiki().getHibernateStore();
        hibernateStore.beginTransaction(context);
        Session session = hibernateStore.getSession(context);
        int objectNumbers = passwordPropertyValues.getObjectIds().size();

        Map<String, Object> queryBindings = new HashMap<>(propertyBindings);
        StringBuilder whereStatement = new StringBuilder();
        for (int loopIndex = 0; objectIndex < objectNumbers && loopIndex < objectBatchSize; loopIndex++) {
            Long objectId = passwordPropertyValues.getObjectIds().get(objectIndex);
            String objectIdBindingName = String.format("objectId_%s", objectIndex);
            queryBindings.put(objectIdBindingName, objectId);
            whereStatement.append(String.format(propertyStatement, objectIdBindingName));

            if (objectIndex < objectNumbers - 1 && loopIndex < objectBatchSize - 1) {
                whereStatement.append(OR_STATEMENT);
            }
            objectIndex++;
        }

        org.hibernate.query.Query<StringProperty> query =
            session.createQuery(String.format(SELECT_PROPERTIES_STATEMENT, whereStatement), StringProperty.class);
        for (Map.Entry<String, Object> bindingEntry : queryBindings.entrySet()) {
            query.setParameter(bindingEntry.getKey(), bindingEntry.getValue());
        }

        for (StringProperty stringProperty : query.getResultList()) {
            PasswordProperty passwordProperty = new PasswordProperty();
            passwordProperty.setName(stringProperty.getName());
            passwordProperty.setId(stringProperty.getId());
            passwordProperty.setValue(stringProperty.getValue());
            session.delete(stringProperty);
            session.save(passwordProperty);
        }

        hibernateStore.endTransaction(context, true);
        logger.info("[{}] objects migrated on [{}] for xclass [{}].", objectIndex, objectNumbers,
            passwordPropertyValues.getClassName());
        return objectIndex;
    }
}
