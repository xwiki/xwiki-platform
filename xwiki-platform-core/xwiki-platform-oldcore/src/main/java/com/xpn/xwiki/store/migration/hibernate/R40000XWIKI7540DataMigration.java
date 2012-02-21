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

import java.util.AbstractMap;
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
import org.hibernate.Query;
import org.hibernate.Session;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.LargeStringProperty;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.store.XWikiHibernateBaseStore.HibernateCallback;
import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.XWikiDBVersion;

/**
 * Migration for XWIKI7540: Merging Annotations with Comments requires that we add some extra fields to the
 * XWiki.XWikiComments class. These fields come from AnnotationCode.AnnotationClass and any existing annotations objects
 * (that are using AnnotationClass) need to be converted to use the updated XWikiComments class instead. Also, all the
 * comments in a document that was modified by this migration need to be sorted by date and given new object numbers so
 * that the comments order is not affected.
 *
 * @version $Id$
 * @since 4.0M1
 */
@Component
@Named("R40000XWIKI7540")
@Singleton
public class R40000XWIKI7540DataMigration extends AbstractHibernateDataMigration
{
    /** Compact wiki document reference serializer useful in mentioning documents in HQL queries. */
    @Inject
    @Named("compactwiki")
    private EntityReferenceSerializer<String> compactWikiStringEntityReferenceSerializer;

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
        getStore().executeWrite(getXWikiContext(), true, new HibernateCallback<Object>()
        {
            @Override
            public Object doInHibernate(Session session) throws HibernateException, XWikiException
            {
                try {
                    DocumentReference xwikiCommentsClassReference =
                        new DocumentReference(getXWikiContext().getDatabase(), "XWiki", "XWikiComments");

                    updateXwikiCommentsClass(session);
                    List<String> documentNamesToSort = updateExistingAnnotations(session, xwikiCommentsClassReference);
                    sortCommentsAndReassignObjectNumbers(session, documentNamesToSort);
                } catch (Exception e) {
                    throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                        XWikiException.ERROR_XWIKI_STORE_MIGRATION, getName() + " migration failed", e);
                }

                return Boolean.TRUE;
            }
        });
    }

    /**
     * Update the XWiki.XWikiComments class by adding the fields from the AnnotationCode.AnnotationClass that are used
     * by the Annotation feature.
     *
     * @param session the Hibernate session
     * @throws HibernateException if underlying Hibernate operations fail
     * @throws XWikiException if problems occur while loading the current XWikiComments class definition
     * @throws DataMigrationException if the update action somehow fails
     */
    private void updateXwikiCommentsClass(Session session) throws HibernateException, XWikiException,
        DataMigrationException
    {
        Query getCommentsClassXMLQuery =
            session.createQuery("SELECT doc.xWikiClassXML FROM XWikiDocument AS doc WHERE "
                + "doc.fullName='XWiki.XWikiComments'");
        String commentsClassXML = (String) getCommentsClassXMLQuery.uniqueResult();

        BaseClass commentsClass = new BaseClass();
        // Load current class definition.
        if (commentsClassXML != null) {
            commentsClass.fromXML(commentsClassXML);
        }

        // Add the new fields.
        boolean needsUpdate = false;
        needsUpdate |= commentsClass.addTextAreaField("selection", "Selection", 40, 5);
        needsUpdate |= commentsClass.addTextAreaField("selectionLeftContext", "Selection Left Context", 40, 5);
        needsUpdate |= commentsClass.addTextAreaField("selectionRightContext", "Selection Right Context", 40, 5);
        needsUpdate |= commentsClass.addTextAreaField("originalSelection", "Original Selection", 40, 5);
        needsUpdate |= commentsClass.addTextField("target", "Target", 30);
        needsUpdate |= commentsClass.addTextField("state", "State", 30);

        if (needsUpdate) {
            String updateCommentsClassXML = commentsClass.toXMLString();

            // Run the UPDATE statement.
            Query setUpdatedCommentsClassXMLQuery =
                session.createQuery("UPDATE XWikiDocument doc SET doc.xWikiClassXML = :value WHERE"
                    + " doc.fullName='XWiki.XWikiComments'");
            setUpdatedCommentsClassXMLQuery.setText("value", updateCommentsClassXML);
            int updateResult = setUpdatedCommentsClassXMLQuery.executeUpdate();
            if (updateResult != 1) {
                throw new DataMigrationException("Failed to update XWiki.XWikiComments class.");
            }
        }
    }

    /**
     * Get all the existing objects that use the AnnotationCode.AnnotationClass and make them use the updated
     * XWiki.XWikiComments class.
     *
     * @param session the Hibernate session
     * @param xwikiCommentsClassReference document reference to XWiki.XWikiComments
     * @return the list of affected document names
     * @throws HibernateException if underlying Hibernate operations fail
     */
    private List<String> updateExistingAnnotations(Session session, DocumentReference xwikiCommentsClassReference)
        throws HibernateException
    {
        // Get each annotation object and the "annotation" property corresponding to it.
        Query getExistingAnnotationsQuery =
            session.createQuery("SELECT obj, prop FROM BaseObject obj, LargeStringProperty prop WHERE "
                + "obj.className='AnnotationCode.AnnotationClass' AND prop.id.id=obj.id AND prop.name='annotation'");
        List<Object[]> queryResults = (List<Object[]>) getExistingAnnotationsQuery.list();

        List<String> documentNamesToSort = new ArrayList<String>();

        for (Object[] queryResult : queryResults) {
            BaseObject annotationObject = (BaseObject) queryResult[0];
            LargeStringProperty annotationProperty = (LargeStringProperty) queryResult[1];

            // Change the class from AnnotationCode.AnnotationClass to XWiki.XWikiComments.
            annotationObject.setXClassReference(xwikiCommentsClassReference);

            // Move the value of the old "annotation" property into the new "comment" property.
            annotationObject.setLargeStringValue("comment", annotationProperty.getValue());

            // Delete the old property from the db.
            session.delete(annotationProperty);

            // Save the updated annotation object.
            session.update(annotationObject);

            // Keep a list of the affected documents.
            String documentName =
                compactWikiStringEntityReferenceSerializer.serialize(annotationObject.getDocumentReference());
            if (!documentNamesToSort.contains(documentName)) {
                documentNamesToSort.add(documentName);
            }
        }
        return documentNamesToSort;
    }

    /**
     * Sort the comments by date and assign new object numbers based on that sorting. Do this only for documents that
     * were affected by the previous operation.
     *
     * @param session the Hibernate session
     * @param documentNamesToSort the list of documents for which to sort the comments by date.
     * @throws HibernateException if underlying Hibernate operations fail
     */
    private void sortCommentsAndReassignObjectNumbers(Session session, List<String> documentNamesToSort)
        throws HibernateException
    {
        if (documentNamesToSort.size() == 0) {
            return;
        }

        Query getCommentsQuery =
            session.createQuery("SELECT obj, date.value FROM BaseObject AS obj, DateProperty AS date WHERE "
                + "obj.className='XWiki.XWikiComments' AND date.id.id=obj.id AND obj.name IN (:affectedDocuments)");
        getCommentsQuery.setParameterList("affectedDocuments", documentNamesToSort);
        List<Object[]> queryResults = (List<Object[]>) getCommentsQuery.list();

        // Build a map that groups datedComments by documents.
        Map<DocumentReference, List<Entry<Date, BaseObject>>> documentToDatedObjectsMap =
            new HashMap<DocumentReference, List<Entry<Date, BaseObject>>>();

        for (Object[] queryResult : queryResults) {
            BaseObject comment = (BaseObject) queryResult[0];
            Date date = (Date) queryResult[1];
            DocumentReference commentedDocumentReference = comment.getDocumentReference();

            List<Entry<Date, BaseObject>> datedComments = documentToDatedObjectsMap.get(commentedDocumentReference);
            if (datedComments == null) {
                datedComments = new ArrayList<Map.Entry<Date, BaseObject>>();
                documentToDatedObjectsMap.put(commentedDocumentReference, datedComments);
            }

            Entry<Date, BaseObject> datedComment = new AbstractMap.SimpleEntry<Date, BaseObject>(date, comment);
            datedComments.add(datedComment);
        }

        // For each document, sort the comments by date.
        for (DocumentReference commentedDocumentReference : documentToDatedObjectsMap.keySet()) {
            List<Entry<Date, BaseObject>> datedComments = documentToDatedObjectsMap.get(commentedDocumentReference);

            Collections.sort(datedComments, new Comparator<Entry<Date, BaseObject>>()
            {
                @Override
                public int compare(Entry<Date, BaseObject> datedComment1, Entry<Date, BaseObject> datedComment2)
                {
                    return datedComment1.getKey().compareTo(datedComment2.getKey());
                }
            });

            // Reassign comment numbers for the current document, based on the previous sorting.
            for (int newObjectNumber = 0; newObjectNumber < datedComments.size(); newObjectNumber++) {
                Entry<Date, BaseObject> datedComment = datedComments.get(newObjectNumber);

                BaseObject comment = datedComment.getValue();
                comment.setNumber(newObjectNumber);

                // Update the stored object.
                session.update(comment);
            }
        }
    }
}
