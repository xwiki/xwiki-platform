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

import java.util.Locale;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.solr.common.SolrInputDocument;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.search.solr.internal.api.FieldUtils;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiAttachment;
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
    @Inject
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @Override
    public boolean setFieldsInternal(LengthSolrInputDocument solrDocument, EntityReference entityReference)
        throws Exception
    {
        AttachmentReference attachmentReference = new AttachmentReference(entityReference);

        XWikiDocument document = getDocument(attachmentReference.getDocumentReference());
        XWikiAttachment attachment = document.getAttachment(attachmentReference.getName());
        if (attachment == null) {
            return false;
        }

        XWikiContext xcontext = xcontextProvider.get();

        solrDocument.setField(FieldUtils.FILENAME, attachment.getFilename());
        solrDocument.setField(FieldUtils.FILENAME_SORT, attachment.getFilename());
        solrDocument.setField(FieldUtils.MIME_TYPE, attachment.getMimeType(xcontext));
        solrDocument.setField(FieldUtils.ATTACHMENT_DATE, attachment.getDate());
        // We need to add a dedicated sort field because the corresponding field is multiValued and thus cannot be used
        // for sorting (the reason it is multiValued is because it is 'reused' on document rows and documents can have
        // multiple attachments).
        solrDocument.setField(FieldUtils.ATTACHMENT_DATE_SORT, attachment.getDate());
        solrDocument.setField(FieldUtils.ATTACHMENT_SIZE, attachment.getFilesize());
        solrDocument.setField(FieldUtils.ATTACHMENT_SIZE_SORT, attachment.getFilesize());
        // We need to index the attachment version (revision) to be able to detect when the search index is out of date
        // (not in sync with the database).
        solrDocument.setField(FieldUtils.ATTACHMENT_VERSION, attachment.getVersion());

        // Index the full author reference for exact matching (faceting).
        String authorStringReference = entityReferenceSerializer.serialize(attachment.getAuthorReference());
        solrDocument.setField(FieldUtils.ATTACHMENT_AUTHOR, authorStringReference);
        try {
            // Index the author display name for free text search and results sorting.
            String authorDisplayName = xcontext.getWiki().getPlainUserName(attachment.getAuthorReference(), xcontext);
            solrDocument.setField(FieldUtils.ATTACHMENT_AUTHOR_DISPLAY, authorDisplayName);
            solrDocument.setField(FieldUtils.ATTACHMENT_AUTHOR_DISPLAY_SORT, authorDisplayName);
        } catch (Exception e) {
            this.logger.error("Failed to get author display name for attachment [{}]", attachment.getReference(), e);
        }

        setLocaleAndContentFields(attachment, solrDocument);

        return true;
    }

    /**
     * Set the locale to all the translations that the owning document has. This ensures that this entity is found for
     * all the translations of a document, not just the original document.
     * <p>
     * Also, index the content with each locale so that the right analyzer is used.
     * 
     * @param attachment the attachment.
     * @param solrDocument the Solr document where to add the fields.
     * @throws Exception if problems occur.
     */
    protected void setLocaleAndContentFields(XWikiAttachment attachment, SolrInputDocument solrDocument)
        throws Exception
    {
        String attachmentTextContent = getContentAsText(attachment);

        // Do the work for each locale.
        for (Locale documentLocale : getLocales(attachment.getDoc(), null)) {
            solrDocument.addField(FieldUtils.LOCALES, documentLocale.toString());

            solrDocument.setField(FieldUtils.getFieldName(FieldUtils.ATTACHMENT_CONTENT, documentLocale),
                attachmentTextContent);
        }

        // We can't rely on the schema's copyField here because we would trigger it for each language. Doing the copy to
        // the text_general field manually.
        solrDocument.setField(FieldUtils.getFieldName(FieldUtils.ATTACHMENT_CONTENT, null), attachmentTextContent);
    }
}
