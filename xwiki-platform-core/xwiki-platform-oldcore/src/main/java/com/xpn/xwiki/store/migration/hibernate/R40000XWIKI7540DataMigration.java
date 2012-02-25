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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Named;
import javax.inject.Singleton;

import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.DateProperty;
import com.xpn.xwiki.objects.LargeStringProperty;
import com.xpn.xwiki.objects.StringListProperty;
import com.xpn.xwiki.objects.StringProperty;
import com.xpn.xwiki.store.XWikiHibernateBaseStore.HibernateCallback;
import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.XWikiDBVersion;

/**
 * Migration for XWIKI7540: Merging Annotations with Comments requires some extra fields to the XWiki.XWikiComments
 * class. These fields come from AnnotationCode.AnnotationClass and any existing annotations objects (that are using
 * AnnotationClass) need to be converted to use the updated XWikiComments class instead. Also, all the comments in a
 * document that was modified by this migration need to be sorted by date and given new object numbers so that the
 * comments order is not affected.
 * 
 * @version $Id$
 * @since 4.0M1
 */
@Component
@Named("R40000XWIKI7540")
@Singleton
public class R40000XWIKI7540DataMigration extends AbstractHibernateDataMigration
{
    @Override
    public String getDescription()
    {
        return "See http://jira.xwiki.org/browse/XWIKI-7540";
    }

    @Override
    public XWikiDBVersion getVersion()
    {
        // XWiki 4.0
        return new XWikiDBVersion(40000);
    }

    @Override
    protected void hibernateMigrate() throws DataMigrationException, XWikiException
    {
        getStore().executeWrite(getXWikiContext(), true, new R40000XWIKI7540HibernateCallback());
    }

    /**
     * Inner class for the HibernateCallback in order to avoid using an anonymous one.
     * 
     * @version $Id$
     */
    private class R40000XWIKI7540HibernateCallback implements HibernateCallback<Object>
    {
        @Override
        public Object doInHibernate(Session session) throws HibernateException, XWikiException
        {
            try {
                // Get each annotation object and every property corresponding to it.
                Query getExistingAnnotationsAndCommentsQuery =
                    session.createQuery("SELECT obj, prop FROM BaseObject obj, BaseProperty prop WHERE "
                        + "(obj.className='AnnotationCode.AnnotationClass' OR obj.className='XWiki.XWikiComments') "
                        + "AND prop.id.id=obj.id AND obj.name in "
                        + "(SELECT doc.fullName FROM XWikiDocument doc, BaseObject ann WHERE "
                        + "ann.name=doc.fullName AND ann.className='AnnotationCode.AnnotationClass')");
                List<Object[]> queryResults = (List<Object[]>) getExistingAnnotationsAndCommentsQuery.list();

                // Build a map that groups datedComments by documents and a map that groups objects with their
                // properties.
                Map<DocumentReference, List<Entry<Date, BaseObject>>> documentToDatedObjectsMap =
                    new HashMap<DocumentReference, List<Entry<Date, BaseObject>>>();
                Map<BaseObject, List<BaseProperty>> objectToPropertiesMap =
                    new HashMap<BaseObject, List<BaseProperty>>();

                // Populate the maps and delete the existing session objects and properties.
                preProcessResults(queryResults, documentToDatedObjectsMap, objectToPropertiesMap, session);

                // Parse the maps and create new objects and properties in the session that replace the old ones.
                processObjects(documentToDatedObjectsMap, objectToPropertiesMap, session);
            } catch (Exception e) {
                throw new XWikiException(XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_MIGRATION,
                    getName() + " migration failed", e);
            }

            return Boolean.TRUE;
        }
    }

    /**
     * Pre-process the results into data structures that can be easily worked with. Also make sure to delete from the
     * hibernate session all the objects and properties because they might clash IDs with the new ones that we will
     * replace them later on. This is caused by the fact that the changes that we need to do are part of the computed
     * object ID, making updates impossible.
     * 
     * @param queryResults an array containing multiple [comment, property] arrays
     * @param documentToDatedObjectsMap empty map that is to contain a mapping of documents to dated objects
     * @param objectToPropertiesMap empty map that is to contain a mapping of objects to their properties
     * @param session the Hibernate session
     * @throws HibernateException if underlying Hibernate operations fail
     */
    private void preProcessResults(List<Object[]> queryResults,
        Map<DocumentReference, List<Entry<Date, BaseObject>>> documentToDatedObjectsMap,
        Map<BaseObject, List<BaseProperty>> objectToPropertiesMap, Session session) throws HibernateException
    {
        for (Object[] queryResult : queryResults) {
            BaseObject object = (BaseObject) queryResult[0];
            BaseProperty property = (BaseProperty) queryResult[1];
            DocumentReference documentReference = object.getDocumentReference();

            if (property instanceof DateProperty) {
                List<Entry<Date, BaseObject>> datedObjects = documentToDatedObjectsMap.get(documentReference);
                if (datedObjects == null) {
                    datedObjects = new ArrayList<Map.Entry<Date, BaseObject>>();
                    documentToDatedObjectsMap.put(documentReference, datedObjects);
                }

                Date date = (Date) ((DateProperty) property).getValue();
                Entry<Date, BaseObject> datedObject = new HashMap.SimpleEntry<Date, BaseObject>(date, object);
                datedObjects.add(datedObject);
            }

            List<BaseProperty> properties = objectToPropertiesMap.get(object);
            if (properties == null) {
                properties = new ArrayList<BaseProperty>();
                objectToPropertiesMap.put(object, properties);
            }
            properties.add(property);

            // Delete from the session both object and property since they might clash IDs with the ones that will
            // replace them later on.
            session.delete(object);
            session.delete(property);
        }

        // Flush and clear the session to be able to make sure the delete batch is processed before the insertions
        // batch, avoiding collisions in the DB at insert time.
        session.flush();
        session.clear();
    }

    /**
     * Sort the objects by date and assign new object IDs. For objects that are annotations (using
     * AnnotationCode.AnnotationClass) convert them to XWiki.XWikiComments.
     * 
     * @param documentToDatedObjectsMap a mapping of documents to dated objects
     * @param objectToPropertiesMap a mapping of objects to their properties
     * @param session the Hibernate Session
     * @throws HibernateException if underlying Hibernate operations fail
     */
    private void processObjects(Map<DocumentReference, List<Entry<Date, BaseObject>>> documentToDatedObjectsMap,
        Map<BaseObject, List<BaseProperty>> objectToPropertiesMap, Session session) throws HibernateException
    {
        DocumentReference xwikiCommentsClassReference =
            new DocumentReference(getXWikiContext().getDatabase(), "XWiki", "XWikiComments");
        DocumentReference annotationClassReference =
            new DocumentReference(getXWikiContext().getDatabase(), "AnnotationCode", "AnnotationClass");

        // For each document
        for (DocumentReference document : documentToDatedObjectsMap.keySet()) {
            List<Entry<Date, BaseObject>> datedObjects = documentToDatedObjectsMap.get(document);

            // Sort the objects by date.
            Collections.sort(datedObjects, new Comparator<Entry<Date, BaseObject>>()
            {
                @Override
                public int compare(Entry<Date, BaseObject> datedObject1, Entry<Date, BaseObject> datedObject2)
                {
                    return datedObject1.getKey().compareTo(datedObject2.getKey());
                }
            });

            // Reassign object numbers for the current document, based on the previous sorting.
            for (int newObjectNumber = 0; newObjectNumber < datedObjects.size(); newObjectNumber++) {
                Entry<Date, BaseObject> datedComment = datedObjects.get(newObjectNumber);

                BaseObject deletedObject = datedComment.getValue();

                // Clone the deleted object and use the new number.
                BaseObject newComment = deletedObject.clone();
                newComment.setNumber(newObjectNumber);

                // If the deleted object is an annotation, make sure to use the comments class instead.
                if (deletedObject.getXClassReference().equals(annotationClassReference)) {
                    newComment.setXClassReference(xwikiCommentsClassReference);
                }

                // Save it as a new object in the database.
                session.save(newComment);

                // Do the same for each of the deleted object's properties and link the new properties to
                // the new object.
                List<BaseProperty> deletedProperties = objectToPropertiesMap.get(deletedObject);
                for (BaseProperty deletedProperty : deletedProperties) {
                    BaseProperty newProperty = null;

                    // Note: LargeStringProperty instances are a bit special because they share the same table with
                    // StringListProperty and Hibernate gets confused when loading them. The result is that
                    // StringListProperty instances are loaded instead of LargeStringProperty and, since we know our
                    // classes well in this specific migration, we can just create the LargeStringProperty instances
                    // ourselves from the loaded ones. It might be related to http://jira.xwiki.org/browse/XWIKI-4384
                    if (deletedProperty instanceof StringListProperty) {
                        // The "author" property was of type User List in AnnotationClass and now it is going to be
                        // String in XWikiComments.
                        if ("author".equals(deletedProperty.getName())) {
                            newProperty = new StringProperty();
                        } else {
                            newProperty = new LargeStringProperty();
                        }

                        String deletedPropertyValue = ((StringListProperty) deletedProperty).getList().get(0);
                        newProperty.setValue(deletedPropertyValue);
                        newProperty.setName(deletedProperty.getName());
                    } else {
                        newProperty = deletedProperty.clone();
                    }
                    newProperty.setId(newComment.getId());

                    // If the deleted property was "annotation" (from AnnotationClass), then name the new
                    // property "comment" for (XWikiComments).
                    if ("annotation".equals(deletedProperty.getName())) {
                        newProperty.setName("comment");
                    }

                    session.save(newProperty);
                }
            }
        }
    }
}
