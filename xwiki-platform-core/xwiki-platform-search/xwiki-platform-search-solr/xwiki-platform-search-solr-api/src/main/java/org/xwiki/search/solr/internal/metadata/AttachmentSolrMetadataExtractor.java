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
import java.util.Locale;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.common.SolrInputDocument;
import org.apache.tika.Tika;
import org.apache.tika.metadata.Metadata;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.search.solr.internal.api.FieldUtils;
import org.xwiki.search.solr.internal.api.SolrIndexerException;
import org.xwiki.search.solr.internal.reference.SolrReferenceResolver;

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
    /**
     * The Solr reference resolver.
     */
    @Inject
    @Named("attachment")
    protected SolrReferenceResolver resolver;

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
        solrDocument.setField(FieldUtils.MIME_TYPE, attachment.getMimeType(xcontext));
        solrDocument.setField(FieldUtils.ATTACHMENT_VERSION, attachment.getVersion());

        setLocaleAndContentFields(attachment, solrDocument);

        return true;
    }

    /**
     * Set the locale to all the translations that the owning document has. This ensures that this entity is found for
     * all the translations of a document, not just the original document.
     * <p/>
     * Also, index the content with each locale so that the right analyzer is used.
     * 
     * @param attachment the attachment.
     * @param solrDocument the Solr document where to add the fields.
     * @throws Exception if problems occur.
     */
    protected void setLocaleAndContentFields(XWikiAttachment attachment, SolrInputDocument solrDocument)
        throws Exception
    {
        Locale defaultDocumentLocale = getLocale(attachment.getDoc().getDocumentReference());

        String attachmentTextContent = getContentAsText(attachment);

        // Do the work for each locale.
        for (Locale documentLocale : getLocales(attachment.getDoc(), null)) {
            if (!documentLocale.equals(defaultDocumentLocale)) {
                // The original document's locale is already set by the call to the setDocumentFields method.
                solrDocument.addField(FieldUtils.LOCALES, documentLocale.toString());
            }

            solrDocument.setField(FieldUtils.getFieldName(FieldUtils.ATTACHMENT_CONTENT, documentLocale),
                attachmentTextContent);
        }

        // We can`t rely on the schema's copyField here because we would trigger it for each language. Doing the copy to
        // the text_general field manually.
        solrDocument.setField(FieldUtils.getFieldName(FieldUtils.ATTACHMENT_CONTENT, null), attachmentTextContent);
    }

    /**
     * Tries to extract text indexable content from a generic attachment.
     * 
     * @param attachment attachment.
     * @return the text representation of the attachment's content.
     * @throws SolrIndexerException if problems occur.
     */
    protected String getContentAsText(XWikiAttachment attachment) throws SolrIndexerException
    {
        try {
            Tika tika = new Tika();

            Metadata metadata = new Metadata();
            metadata.set(Metadata.RESOURCE_NAME_KEY, attachment.getFilename());

            InputStream in = attachment.getContentInputStream(this.xcontextProvider.get());

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
