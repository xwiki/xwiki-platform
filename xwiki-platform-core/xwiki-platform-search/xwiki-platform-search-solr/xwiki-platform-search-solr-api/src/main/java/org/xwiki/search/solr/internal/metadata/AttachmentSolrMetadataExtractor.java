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
package org.xwiki.search.solr.internal.metadata;

import java.io.InputStream;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.common.SolrInputDocument;
import org.apache.tika.Tika;
import org.apache.tika.metadata.Metadata;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.search.solr.internal.api.Fields;
import org.xwiki.search.solr.internal.api.SolrIndexerException;
import org.xwiki.search.solr.internal.reference.SolrReferenceResolver;

import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Extract the metadata to be indexed from attachments.
 * 
 * @version $Id$
 * @since 4.3M2
 */
@Component
@Named("attachment")
@Singleton
public class AttachmentSolrMetadataExtractor extends AbstractSolrMetadataExtractor
{
    /**
     * The Solr reference resolver.
     */
    @Inject
    @Named("attachment")
    protected SolrReferenceResolver resolver;

    /**
     * DocumentAccessBridge component.
     */
    @Inject
    protected DocumentAccessBridge documentAccessBridge;

    @Override
    public void addFieldsInternal(LengthSolrInputDocument solrDocument, EntityReference entityReference)
        throws Exception
    {
        AttachmentReference attachmentReference = new AttachmentReference(entityReference);

        DocumentReference documentReference = attachmentReference.getDocumentReference();

        solrDocument.addField(Fields.FILENAME, attachmentReference.getName());
        solrDocument.addField(Fields.MIME_TYPE, getMimeType(attachmentReference));

        addLocaleAndContentFields(documentReference, solrDocument, attachmentReference);
    }

    /**
     * Set the locale to all the translations that the owning document has. This ensures that this entity is found for
     * all the translations of a document, not just the original document.
     * <p/>
     * Also, index the content with each locale so that the right analyzer is used.
     * 
     * @param documentReference the original document's reference.
     * @param solrDocument the Solr document where to add the fields.
     * @param attachmentReference the attachment's reference.
     * @throws Exception if problems occur.
     */
    protected void addLocaleAndContentFields(DocumentReference documentReference, SolrInputDocument solrDocument,
        AttachmentReference attachmentReference) throws Exception
    {
        XWikiDocument originalDocument = getDocument(documentReference);

        // Get all the locales in which the document is available.
        List<Locale> documentLocales = originalDocument.getTranslationLocales(this.xcontextProvider.get());
        // Make sure that the original document's locale is there as well.
        Locale originalDocumentLocale = getLocale(documentReference);
        if (!documentLocales.contains(originalDocumentLocale)) {
            documentLocales.add(originalDocumentLocale);
        }

        String attachmentTextContent = getContentAsText(attachmentReference);
        // Do the work for each locale.
        for (Locale documentLocale : documentLocales) {
            if (!documentLocale.equals(originalDocumentLocale)) {
                // The original document's locale is already set by the call to the addDocumentFields method.
                solrDocument.addField(Fields.LOCALE, documentLocale);
            }

            solrDocument.addField(
                String.format(Fields.MULTILIGNUAL_FORMAT, Fields.ATTACHMENT_CONTENT, documentLocale.toString()),
                attachmentTextContent);
        }

        // We can`t rely on the schema's copyField here because we would trigger it for each language. Doing the copy to
        // the text_general field manually.
        solrDocument.addField(
            String.format(Fields.MULTILIGNUAL_FORMAT, Fields.ATTACHMENT_CONTENT, Fields.MULTILINGUAL),
            attachmentTextContent);
    }

    /**
     * Tries to extract text indexable content from a generic attachment.
     * 
     * @param attachment reference to the attachment.
     * @return the text representation of the attachment's content.
     * @throws SolrIndexerException if problems occur.
     */
    protected String getContentAsText(AttachmentReference attachment) throws SolrIndexerException
    {
        try {
            Tika tika = new Tika();

            Metadata metadata = new Metadata();
            metadata.set(Metadata.RESOURCE_NAME_KEY, attachment.getName());

            InputStream in = this.documentAccessBridge.getAttachmentContent(attachment);

            try {
                String result = StringUtils.lowerCase(tika.parseToString(in, metadata));

                return result;
            } finally {
                in.close();
            }
        } catch (Exception e) {
            throw new SolrIndexerException(String.format("Failed to retrieve attachment content for '%s'", attachment),
                e);
        }
    }

    /**
     * @param reference to the attachment
     * @return the mimetype of the attachment's content.
     */
    protected String getMimeType(AttachmentReference reference)
    {
        String mimetype = this.xcontextProvider.get().getEngineContext().getMimeType(reference.getName().toLowerCase());
        if (mimetype != null) {
            return mimetype;
        } else {
            return "application/octet-stream";
        }
    }
}
