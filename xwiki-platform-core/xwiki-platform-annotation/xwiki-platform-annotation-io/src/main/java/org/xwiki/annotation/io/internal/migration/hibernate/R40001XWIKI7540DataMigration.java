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
package org.xwiki.annotation.io.internal.migration.hibernate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.slf4j.Logger;
import org.xwiki.annotation.AnnotationConfiguration;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.WikiReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.DateProperty;
import com.xpn.xwiki.objects.LargeStringProperty;
import com.xpn.xwiki.objects.StringListProperty;
import com.xpn.xwiki.objects.StringProperty;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.store.XWikiHibernateBaseStore.HibernateCallback;
import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.XWikiDBVersion;
import com.xpn.xwiki.store.migration.hibernate.AbstractHibernateDataMigration;

/**
 * Migration for XWIKI7540: Merging Annotations with Comments requires some extra fields to the XWiki.XWikiComments
 * class. These fields come from AnnotationCode.AnnotationClass and any existing annotations objects (that are using
 * AnnotationClass) need to be converted to use the updated XWikiComments class instead. Also, all the comments in a
 * document that was modified by this migration need to be sorted by date and given new object numbers so that the
 * comments order is not affected.
 *
 * @version $Id$
 * @since 4.0M2
 */
@Component
@Named("R40001XWIKI7540")
@Singleton
public class R40001XWIKI7540DataMigration extends AbstractHibernateDataMigration
{
    /**
     * The comment class reference.
     */
    private static final EntityReference XWIKI_COMMENT_CLASS_REFERENCE =
        new EntityReference("XWikiComments", EntityType.DOCUMENT, new EntityReference("XWiki", EntityType.SPACE));

    /**
     * The annotation class reference.
     */
    private static final EntityReference XWIKI_ANNOTATION_CLASS_REFERENCE = new EntityReference("AnnotationClass",
        EntityType.DOCUMENT, new EntityReference("AnnotationCode", EntityType.SPACE));

    /**
     * Everybody logs... sometimes.
     */
    @Inject
    protected Logger logger;

    /**
     * Used to serialize document references when logging.
     */
    @Inject
    protected EntityReferenceSerializer<String> referenceSerializer;

    /**
     * Used to determine the current annotation class.
     */
    @Inject
    protected AnnotationConfiguration configuration;

    /**
     * Holds the work to be done by grouping datedComments by documents.
     */
    protected Map<DocumentReference, List<Entry<Date, BaseObject>>> documentToDatedObjectsMap = new HashMap<>();

    /**
     * Holds the work to be done by grouping properties by the objects to which they belong.
     */
    protected Map<BaseObject, List<BaseProperty>> objectToPropertiesMap = new HashMap<>();

    @Override
    public String getDescription()
    {
        return "See https://jira.xwiki.org/browse/XWIKI-7540";
    }

    @Override
    public XWikiDBVersion getVersion()
    {
        // XWiki 4.0, second migration.
        return new XWikiDBVersion(40001);
    }

    /**
     * Check if the migration can be executed by verifying if the current annotation class is the default one and that
     * the comments class does not have any custom mappings set up.
     * <p>
     * Note: We can not do this in {@link #shouldExecute(XWikiDBVersion)} because we need to read the database and we
     * can not do that until the previous migrations are executed.
     *
     * @return true if the migration can be executed, false otherwise.
     * @throws DataMigrationException if the annotation or comments class can not be properly retrieved
     */
    protected boolean checkAnnotationsAndComments() throws DataMigrationException
    {
        XWikiContext context = getXWikiContext();
        String resultOfSkippingDatabase = "Comments and anotations will remain separated";

        try {
            EntityReference currentAnnotationClassReference = this.configuration.getAnnotationClassReference();
            currentAnnotationClassReference =
                currentAnnotationClassReference.removeParent(new WikiReference(context.getWikiId()));
            if (!XWIKI_ANNOTATION_CLASS_REFERENCE.equals(currentAnnotationClassReference)) {
                this.logger.warn("Skipping database [{}] because it uses a custom annotation class. {}",
                    context.getWikiId(),
                    resultOfSkippingDatabase);
                return false;
            }

            BaseClass commentsClass = context.getWiki().getCommentsClass(context);
            if (commentsClass.hasCustomMapping()) {
                this.logger.warn("Skipping database [{}] because it uses a custom mapping for comments. {}",
                    context.getWikiId(), resultOfSkippingDatabase);
                return false;
            }
        } catch (Exception e) {
            // Should not happen
            String message = "Failed to check the current annotation and comments classes for customizations. "
                + "Migration will not execute";
            this.logger.error(message, e);
            throw new DataMigrationException(message, e);
        }

        return true;
    }

    @Override
    protected void hibernateMigrate() throws DataMigrationException, XWikiException
    {
        // Check if the migration can be executed.
        if (!checkAnnotationsAndComments()) {
            return;
        }

        // Clear any existing migration data/cache from previously migrated wikis.
        this.documentToDatedObjectsMap.clear();
        this.objectToPropertiesMap.clear();

        this.logger.info("Computing the work to be done.");

        // 1st step: populate the 2 maps with the work to be done.
        getStore().executeRead(getXWikiContext(), new GetWorkToBeDoneHibernateCallback());

        this.logger.info("There is a total of {} documents to migrate.",
            this.documentToDatedObjectsMap.keySet().size());

        // 2nd step: for each document, delete the old objects and create new (updated) ones. One transaction per
        // document.
        DoWorkOnDocumentHibernateCallback doWorkOnDocumentHibernateCallback = new DoWorkOnDocumentHibernateCallback();
        for (DocumentReference documentReference : this.documentToDatedObjectsMap.keySet()) {
            this.logger.info("Migrating document [{}]",
                this.referenceSerializer.serialize(documentReference, (Object[]) null));

            doWorkOnDocumentHibernateCallback.setDocumentReference(documentReference);
            getStore().executeWrite(getXWikiContext(), doWorkOnDocumentHibernateCallback);
        }
    }

    /**
     * Inner class that retrieves the documents, objects and properties that need to be migrated.
     *
     * @version $Id$
     */
    private final class GetWorkToBeDoneHibernateCallback implements HibernateCallback<Object>
    {
        @Override
        public Object doInHibernate(Session session) throws HibernateException, XWikiException
        {
            try {
                // Get all annotation object and comment object with every property that they have. Do this only for
                // documents that have annotation objects in them, and thus need to be migrated.
                Query<Object[]> getExistingAnnotationsAndCommentsQuery =
                    session.createQuery("SELECT obj, prop FROM BaseObject obj, BaseProperty prop WHERE "
                        + "(obj.className='AnnotationCode.AnnotationClass' OR obj.className='XWiki.XWikiComments') "
                        + "AND prop.id.id=obj.id AND obj.name in "
                        + "(SELECT doc.fullName FROM XWikiDocument doc, BaseObject ann WHERE "
                        + "ann.name=doc.fullName AND ann.className='AnnotationCode.AnnotationClass')", Object[].class);
                List<Object[]> queryResults = getExistingAnnotationsAndCommentsQuery.list();

                preProcessResults(queryResults);
            } catch (Exception e) {
                throw new XWikiException(XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_MIGRATION,
                    getName() + " failed to read the work to be done.", e);
            }

            return Boolean.TRUE;
        }

        /**
         * Pre-process the results into data structures that can be easily worked with.
         *
         * @param queryResults an array containing multiple [comment, property] arrays
         * @throws HibernateException if underlying Hibernate operations fail
         */
        private void preProcessResults(List<Object[]> queryResults) throws HibernateException
        {
            for (Object[] queryResult : queryResults) {
                BaseObject object = (BaseObject) queryResult[0];
                BaseProperty property = (BaseProperty) queryResult[1];
                DocumentReference documentReference = object.getDocumentReference();

                if (property instanceof DateProperty) {
                    List<Entry<Date, BaseObject>> datedObjects =
                        R40001XWIKI7540DataMigration.this.documentToDatedObjectsMap.get(documentReference);
                    if (datedObjects == null) {
                        datedObjects = new ArrayList<>();
                        R40001XWIKI7540DataMigration.this.documentToDatedObjectsMap.put(documentReference,
                            datedObjects);
                    }

                    Date date = (Date) property.getValue();
                    Entry<Date, BaseObject> datedObject = new HashMap.SimpleEntry<>(date, object);
                    datedObjects.add(datedObject);
                }

                List<BaseProperty> properties = R40001XWIKI7540DataMigration.this.objectToPropertiesMap.get(object);
                if (properties == null) {
                    properties = new ArrayList<>();
                    R40001XWIKI7540DataMigration.this.objectToPropertiesMap.put(object, properties);
                }
                properties.add(property);
            }
        }
    }

    /**
     * Inner class that is in charge of migrating each document.
     *
     * @version $Id$
     */
    private final class DoWorkOnDocumentHibernateCallback implements HibernateCallback<Object>
    {
        /**
         * @see #setDocumentReference(DocumentReference)
         */
        private DocumentReference documentReference;

        /**
         * @see #getMigratedObject(BaseObject, int)
         */
        private Map<BaseObject, BaseObject> oldToNewObjectMap;

        /**
         * @see #processObjects(Session)
         */
        private Map<Integer, Integer> oldToNewCommentNumberMap;

        /**
         * @param documentReference the document on which to work
         */
        public void setDocumentReference(DocumentReference documentReference)
        {
            this.documentReference = documentReference;
            this.oldToNewObjectMap = new HashMap<>();
            this.oldToNewCommentNumberMap = new HashMap<>();
        }

        @Override
        public Object doInHibernate(Session session) throws HibernateException, XWikiException
        {
            try {
                // Parse the maps, delete the old objects and properties, and create new (updated) ones that replace
                // them.
                processObjects(session);
            } catch (Exception e) {
                throw new XWikiException(XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_MIGRATION,
                    getName() + " failed to do the work for document "
                        + R40001XWIKI7540DataMigration.this.referenceSerializer.serialize(this.documentReference,
                        (Object[]) null),
                    e);
            }

            return Boolean.TRUE;
        }

        /**
         * Sort the objects by date and assign new object IDs. For objects that are annotations (using
         * AnnotationCode.AnnotationClass) convert them to XWiki.XWikiComments.
         *
         * @param session the Hibernate Session
         * @throws HibernateException if underlying Hibernate operations fail
         */
        private void processObjects(Session session) throws HibernateException
        {
            List<Entry<Date, BaseObject>> datedObjects =
                R40001XWIKI7540DataMigration.this.documentToDatedObjectsMap.get(this.documentReference);

            // Because the changes we need to do are part of the computed object ID, updating the objects and
            // properties in the session is not possible. Thus, we need to delete from the session all objects and
            // properties for the current document so that they do not clash IDs with the ones that will replace them
            // below.
            for (Entry<Date, BaseObject> datedObject : datedObjects) {
                BaseObject object = datedObject.getValue();

                for (BaseProperty property : R40001XWIKI7540DataMigration.this.objectToPropertiesMap.get(object)) {
                    session.delete(property);
                }

                session.delete(object);
            }

            // Flush and clear the session to be able to make sure the delete batch is processed before the insertions
            // batch, avoiding ID collisions in the DB at insert time.
            session.flush();
            session.clear();

            // Sort the objects by date. The objects were removed from the session but are still available in-memory.
            Collections.sort(datedObjects, new Comparator<Entry<Date, BaseObject>>()
            {
                @Override
                public int compare(Entry<Date, BaseObject> datedObject1, Entry<Date, BaseObject> datedObject2)
                {
                    return datedObject1.getKey().compareTo(datedObject2.getKey());
                }
            });

            // Reassign object numbers and convert annotations for the current document, based on the previous sorting.
            for (int newObjectNumber = 0; newObjectNumber < datedObjects.size(); newObjectNumber++) {
                BaseObject deletedObject = datedObjects.get(newObjectNumber).getValue();

                BaseObject newComment = getMigratedObject(deletedObject, newObjectNumber);

                // Only for simple comments, keeps track of converted object numbers. Used when migrating a comment's
                // "replyto" property.
                if (deletedObject.getRelativeXClassReference().equals(XWIKI_COMMENT_CLASS_REFERENCE)) {
                    this.oldToNewCommentNumberMap.put(deletedObject.getNumber(), newComment.getNumber());
                }

                // Remember the corresponding new objects generated in this phase to be used below, when migrating the
                // properties.
                this.oldToNewObjectMap.put(deletedObject, newComment);

                session.save(newComment);
            }

            // Migrate each of the deleted object's properties and link the new properties to the new objects.
            for (int newObjectNumber = 0; newObjectNumber < datedObjects.size(); newObjectNumber++) {
                BaseObject deletedObject = datedObjects.get(newObjectNumber).getValue();

                // Use the corresponding new object created above.
                BaseObject newComment = this.oldToNewObjectMap.get(deletedObject);

                List<BaseProperty> deletedProperties =
                    R40001XWIKI7540DataMigration.this.objectToPropertiesMap.get(deletedObject);
                for (BaseProperty deletedProperty : deletedProperties) {
                    BaseProperty newProperty = getMigratedProperty(deletedProperty, newComment);

                    session.save(newProperty);
                }
            }
        }

        /**
         * @param deletedObject the old object to migrate
         * @param newObjectNumber the new object ID to assign to the migrated object
         * @return an in-memory migrated version of the old object
         */
        private BaseObject getMigratedObject(BaseObject deletedObject, int newObjectNumber)
        {
            // Clone the deleted object and use the new number.
            BaseObject newObject = deletedObject.clone();
            newObject.setNumber(newObjectNumber);

            // If the deleted object is an annotation, make sure to use the comments class instead.
            if (deletedObject.getRelativeXClassReference().equals(XWIKI_ANNOTATION_CLASS_REFERENCE)) {
                newObject.setXClassReference(XWIKI_COMMENT_CLASS_REFERENCE);
            }

            return newObject;
        }

        /**
         * @param deletedProperty the old property to migrate
         * @param newComment the new comment to which to assign the migrated property
         * @return an in-memory migrated version of the old property
         */
        private BaseProperty getMigratedProperty(BaseProperty deletedProperty, BaseObject newComment)
        {
            BaseProperty newProperty = null;

            // Note: LargeStringProperty instances are a bit special because they share the same table with
            // StringListProperty and Hibernate gets confused when loading them. The result is that
            // StringListProperty instances are loaded instead of LargeStringProperty and, since we know our
            // classes well in this specific migration, we can just create the LargeStringProperty instances
            // ourselves from the loaded ones. It might be related to https://jira.xwiki.org/browse/XWIKI-4384
            if (deletedProperty instanceof StringListProperty stringListProperty) {
                // The "author" property was of type User List in AnnotationClass, and now it is going to be
                // String in XWikiComments.
                if ("author".equals(deletedProperty.getName())) {
                    newProperty = new StringProperty();
                } else {
                    newProperty = new LargeStringProperty();
                }

                // Extract the value as first element in the internal list. If the list has 0 elements, then the value
                // is null.
                String deletedPropertyValue = null;
                List<String> internalListValue = stringListProperty.getList();
                if (!internalListValue.isEmpty()) {
                    deletedPropertyValue = internalListValue.get(0);
                }

                newProperty.setValue(deletedPropertyValue);
                newProperty.setName(deletedProperty.getName());
            } else {
                newProperty = deletedProperty.clone();
            }
            newProperty.setId(newComment.getId());

            if ("annotation".equals(deletedProperty.getName())) {
                // If the deleted property was "annotation" (from AnnotationClass), then use the new
                // property "comment" for (XWikiComments).
                newProperty.setName("comment");
            } else if ("replyto".equals(deletedProperty.getName())) {
                // XWIKI-7745: We need to handle the fact that the "replyto" property needs to point to the new object
                // number of the comment it was previously assigned to, since the comment can now have a new number
                // assigned to it.
                if (deletedProperty.getValue() != null) {
                    int oldValue = (Integer) deletedProperty.getValue();
                    int newValue = this.oldToNewCommentNumberMap.get(oldValue);

                    newProperty.setValue(newValue);
                }
            }
            return newProperty;
        }
    }
}
