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
package org.xwiki.search.solr.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;

import javax.inject.Inject;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.ObjectPropertyReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.search.solr.internal.api.SolrIndexException;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseObjectReference;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.classes.PasswordClass;

/**
 * Default implementation.
 * 
 * @version $Id$
 * @since 4.3M2
 */
// FIXME: REFACTOR into multiple implementations of the IndexableReferenceExtractor interface, just like we did for
// SolrMetadataExtractor
@Component
@InstantiationStrategy(ComponentInstantiationStrategy.SINGLETON)
public class DefaultIndexableReferenceExtractor implements IndexableReferenceExtractor
{
    /**
     * Reference to String serializer.
     */
    @Inject
    protected EntityReferenceSerializer<String> serializer;

    /**
     * Execution component.
     */
    @Inject
    protected Execution execution;

    /**
     * DocumentAccessBridge component.
     */
    @Inject
    protected DocumentAccessBridge documentAccessBridge;

    @Override
    public List<EntityReference> getReferences(EntityReference startReference) throws SolrIndexException
    {
        List<EntityReference> references = new ArrayList<EntityReference>();

        try {
            switch (startReference.getType()) {
                case WIKI:
                    WikiReference wikiReference = new WikiReference(startReference);

                    references.addAll(getReferencesFor(wikiReference));

                    break;
                case SPACE:
                    SpaceReference spaceReference = new SpaceReference(startReference);

                    references.addAll(getReferencesFor(spaceReference));

                    break;
                case DOCUMENT:
                    DocumentReference documentReference = new DocumentReference(startReference);

                    references.addAll(getReferencesFor(documentReference));

                    break;
                case OBJECT:
                    BaseObjectReference objectReference = new BaseObjectReference(startReference);

                    references.addAll(getReferencesFor(objectReference));

                    break;
                case OBJECT_PROPERTY:
                    ObjectPropertyReference objectPropertyReference = new ObjectPropertyReference(startReference);

                    references.addAll(getReferencesFor(objectPropertyReference));

                    break;
                case ATTACHMENT:
                    // Just in case. Not much to do here.
                    AttachmentReference attachmentReference = new AttachmentReference(startReference);

                    references.add(attachmentReference);

                    break;
                default:
                    // Ignore.
                    break;
            }
        } catch (Exception e) {
            throw new SolrIndexException(String.format("Failed to compute the list of entities to index for '%s'",
                serializer.serialize(startReference)), e);
        }

        return references;
    }

    /**
     * @param objectPropertyReference the object property reference.
     * @return indexable references in an object property.
     * @throws Exception if problems occur.
     */
    protected List<EntityReference> getReferencesFor(ObjectPropertyReference objectPropertyReference) throws Exception
    {
        List<EntityReference> result = new ArrayList<EntityReference>();

        // Avoid indexing passwords.
        BaseObjectReference objectReference = new BaseObjectReference(objectPropertyReference.getParent());
        DocumentReference classReference = objectReference.getXClassReference();

        // FIXME: The things we do to make checkstyle happy...
        if (!(getDocument(classReference).getXClass().get(objectPropertyReference.getName())
            instanceof PasswordClass)) {
            result.add(objectPropertyReference);
        }

        return result;
    }

    /**
     * @param objectReference the object reference.
     * @return indexable references in a object.
     * @throws Exception if problems occur.
     */
    protected List<EntityReference> getReferencesFor(BaseObjectReference objectReference) throws Exception
    {
        List<EntityReference> result = new ArrayList<EntityReference>();

        // Object itself
        result.add(objectReference);

        // Object properties
        DocumentReference documentReference = new DocumentReference(objectReference.getParent());
        XWikiDocument document = getDocument(documentReference);
        BaseObject object = document.getXObject(objectReference);
        if (object != null) {
            for (Object field : object.getFieldList()) {
                BaseProperty<EntityReference> objectProperty = (BaseProperty<EntityReference>) field;

                result.addAll(getReferences(objectProperty.getReference()));
            }
        }

        return result;
    }

    /**
     * @param documentReference the document reference.
     * @return indexable references in a document.
     * @throws Exception if problems occur.
     */
    protected List<EntityReference> getReferencesFor(DocumentReference documentReference) throws Exception
    {
        List<EntityReference> result = new ArrayList<EntityReference>();

        XWikiContext context = getXWikiContext();

        // FIXME: The GSoC implementation had a second part in the if. Remove this comment if it proves to be not
        // important in, say... version 5.1.
        // && !documentReference.getName().contains("WatchList")
        if (context.getWiki().exists(documentReference, context)) {

            // Document itself
            result.add(documentReference);

            // FIXME: Naive assumption that the original document does not have the locale set in the reference.
            // http://jira.xwiki.org/browse/XWIKI-8349 should make things clearer at some point, but for now we are
            // using what we have.
            // FIXME: Second assumption - Only original documents contain objects and attachments, because objects are
            // not translatable.
            // http://jira.xwiki.org/browse/XWIKI-69 is the long standing issue on which the second assumption relies.
            if (documentReference.getLocale() == null) {
                XWikiDocument document = getDocument(documentReference);

                // Document translations
                List<String> translatedLanguages = document.getTranslationList(context);
                for (String translatedLanguage : translatedLanguages) {
                    DocumentReference translatedDocumentReference =
                        new DocumentReference(documentReference, new Locale(translatedLanguage));
                    result.add(translatedDocumentReference);
                }

                // Attachments
                List<AttachmentReference> attachmentReferences =
                    documentAccessBridge.getAttachmentReferences(documentReference);
                // Directly add references since we`ve reached the bottom.
                result.addAll(attachmentReferences);

                // Objects
                for (Entry<DocumentReference, List<BaseObject>> entry : document.getXObjects().entrySet()) {
                    List<BaseObject> objects = entry.getValue();
                    for (BaseObject object : objects) {
                        if (object != null) {
                            result.addAll(getReferences(object.getReference()));
                        }
                    }
                }
            }
        }

        return result;
    }

    /**
     * @param spaceReference the space reference.
     * @return indexable references in a space.
     * @throws Exception if problems occur.
     */
    protected List<EntityReference> getReferencesFor(SpaceReference spaceReference) throws Exception
    {
        List<EntityReference> result = new ArrayList<EntityReference>();

        // Ignore the space reference because it is not indexable.

        XWikiContext context = getXWikiContext();
        String currentDatabase = context.getDatabase();

        // Make sure the list of documents in the space is retrieved from the space's wiki.
        context.setDatabase(spaceReference.getParent().getName());
        List<String> documentNames = null;
        try {
            documentNames = context.getWiki().getSpaceDocsName(spaceReference.getName(), context);
        } finally {
            // Reset the context database.
            context.setDatabase(currentDatabase);
        }

        for (String documentName : documentNames) {
            DocumentReference documentReference = new DocumentReference(documentName, spaceReference);

            result.addAll(getReferences(documentReference));
        }

        return result;
    }

    /**
     * @param wikiReference the wiki reference.
     * @return indexable references in a wiki.
     * @throws Exception if problems occur.
     */
    private List<EntityReference> getReferencesFor(WikiReference wikiReference) throws Exception
    {
        List<EntityReference> result = new ArrayList<EntityReference>();

        // Ignore the wiki reference because it is not indexable.

        XWikiContext context = getXWikiContext();
        String currentDatabase = context.getDatabase();

        // Make sure the list of spaces is from the requested wiki.
        context.setDatabase(wikiReference.getName());
        List<String> spaces = null;
        try {
            spaces = context.getWiki().getSpaces(context);
        } finally {
            // Reset the context database.
            context.setDatabase(currentDatabase);
        }

        // Visit each space
        for (String space : spaces) {
            SpaceReference spaceReference = new SpaceReference(space, wikiReference);

            result.addAll(getReferences(spaceReference));
        }

        return result;
    }

    /**
     * @return the XWikiContext
     */
    protected XWikiContext getXWikiContext()
    {
        ExecutionContext executionContext = this.execution.getContext();
        XWikiContext context = (XWikiContext) executionContext.getProperty(XWikiContext.EXECUTIONCONTEXT_KEY);
        // FIXME: Do we need this? Maybe when running an index Thread?
        // if (context == null) {
        // context = this.contextProvider.createStubContext();
        // executionContext.setProperty(XWikiContext.EXECUTIONCONTEXT_KEY, context);
        // }
        return context;
    }

    /**
     * Utility method.
     * 
     * @param documentReference reference to a document.
     * @return the {@link XWikiDocument} instance referenced.
     * @throws Exception if problems occur.
     */
    protected XWikiDocument getDocument(DocumentReference documentReference) throws Exception
    {
        XWikiContext context = getXWikiContext();
        XWikiDocument document = context.getWiki().getDocument(documentReference, context);

        return document;
    }
}
