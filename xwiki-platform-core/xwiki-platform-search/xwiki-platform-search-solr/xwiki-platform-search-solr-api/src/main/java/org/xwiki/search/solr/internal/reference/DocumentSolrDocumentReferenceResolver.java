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
package org.xwiki.search.solr.internal.reference;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.search.solr.internal.api.SolrIndexException;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseObjectReference;

/**
 * Resolve document references.
 * 
 * @version $Id$
 * @since 5.1M2
 */
@Component
@Named("document")
@Singleton
public class DocumentSolrDocumentReferenceResolver extends AbstractSolrDocumentReferenceResolver
{
    /**
     * Used to resolve object references.
     */
    @Inject
    @Named("object")
    private SolrDocumentReferenceResolver objectResolver;

    /**
     * Used to resolve attachment references.
     */
    @Inject
    private SolrDocumentReferenceResolver attachmentResolver;

    @Override
    public List<EntityReference> getReferences(EntityReference reference) throws SolrIndexException
    {
        List<EntityReference> result = new ArrayList<EntityReference>();

        XWikiContext context = this.xcontextProvider.get();

        DocumentReference documentReference = new DocumentReference(reference);

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
                XWikiDocument document;
                try {
                    document = getDocument(documentReference);
                } catch (Exception e) {
                    throw new SolrIndexException(String.format("Failed to get document [%]", documentReference));
                }

                // Document translations
                List<String> translatedLanguages;
                try {
                    translatedLanguages = document.getTranslationList(context);
                } catch (XWikiException e) {

                    throw new SolrIndexException(String.format("Failed to get document [%s] translations",
                        documentReference));
                }

                for (String translatedLanguage : translatedLanguages) {
                    DocumentReference translatedDocumentReference =
                        new DocumentReference(documentReference, new Locale(translatedLanguage));
                    result.add(translatedDocumentReference);
                }

                // Attachments
                addAttachmentsReferences(document, result);

                // Objects
                addObjectsReferences(document, result);
            }
        }

        return result;
    }

    /**
     * @param document the document
     * @param result the list to add reference to
     */
    private void addAttachmentsReferences(XWikiDocument document, List<EntityReference> result)
    {
        List<XWikiAttachment> attachments = document.getAttachmentList();
        for (XWikiAttachment attachment : attachments) {
            AttachmentReference attachmentReference = attachment.getReference();

            try {
                result.addAll(this.attachmentResolver.getReferences(attachmentReference));
            } catch (Exception a) {
                this.logger.error("Failed to resolve references for attachment [" + attachmentReference + "]");
            }
        }
    }

    /**
     * @param document the document
     * @param result the list to add reference to
     */
    private void addObjectsReferences(XWikiDocument document, List<EntityReference> result)
    {
        for (Entry<DocumentReference, List<BaseObject>> entry : document.getXObjects().entrySet()) {
            List<BaseObject> objects = entry.getValue();
            for (BaseObject object : objects) {
                if (object != null) {
                    BaseObjectReference objectReference = object.getReference();

                    try {
                        result.addAll(this.objectResolver.getReferences(objectReference));
                    } catch (Exception a) {
                        this.logger.error("Failed to resolve references for object [" + objectReference + "]");
                    }
                }
            }
        }
    }
}
