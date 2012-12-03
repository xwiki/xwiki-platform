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

import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.common.SolrInputDocument;
import org.apache.tika.Tika;
import org.apache.tika.metadata.Metadata;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.search.solr.internal.api.Fields;
import org.xwiki.search.solr.internal.api.SolrIndexException;

/**
 * Extract the metadata to be indexed from attachments.
 * 
 * @version $Id$
 * @since 4.3M2
 */
@Component
@Named("attachment")
public class AttachmentSolrMetadataExtractor extends AbstractSolrMetadataExtractor
{
    @Override
    public SolrInputDocument getSolrDocument(EntityReference entityReference) throws SolrIndexException,
        IllegalArgumentException
    {
        AttachmentReference attachmentReference = new AttachmentReference(entityReference);

        try {
            SolrInputDocument solrDocument = new SolrInputDocument();

            DocumentReference documentReference = attachmentReference.getDocumentReference();

            solrDocument.addField(Fields.ID, getId(attachmentReference));
            addDocumentFields(documentReference, solrDocument);
            solrDocument.addField(Fields.TYPE, attachmentReference.getType().name());
            solrDocument.addField(Fields.FILENAME, attachmentReference.getName());
            solrDocument.addField(Fields.ATTACHMENT_CONTENT, getContentAsText(attachmentReference));
            solrDocument.addField(Fields.MIME_TYPE, getMimeType(attachmentReference));

            return solrDocument;
        } catch (Exception e) {
            throw new SolrIndexException(String.format("Failed to get Solr document for '%s'",
                serializer.serialize(attachmentReference)), e);
        }
    }

    /**
     * Tries to extract text indexable content from a generic attachment.
     * 
     * @param attachment reference to the attachment.
     * @return the text representation of the attachment's content.
     * @throws SolrIndexException if problems occur.
     */
    protected String getContentAsText(AttachmentReference attachment) throws SolrIndexException
    {
        try {
            Tika tika = new Tika();

            Metadata metadata = new Metadata();
            metadata.set(Metadata.RESOURCE_NAME_KEY, attachment.getName());

            InputStream in = documentAccessBridge.getAttachmentContent(attachment);

            String result = StringUtils.lowerCase(tika.parseToString(in, metadata));

            return result;
        } catch (Exception e) {
            throw new SolrIndexException(String.format("Failed to retrieve attachment content for '%s'",
                serializer.serialize(attachment)), e);
        }
    }

    /**
     * @param reference to the attachment
     * @return the mimetype of the attachment's content.
     */
    protected String getMimeType(AttachmentReference reference)
    {
        String mimetype = getXWikiContext().getEngineContext().getMimeType(reference.getName().toLowerCase());
        if (mimetype != null) {
            return mimetype;
        } else {
            return "application/octet-stream";
        }
    }
}
