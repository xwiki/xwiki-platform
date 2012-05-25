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
package com.xpn.xwiki.plugin.lucene;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Fieldable;
import org.apache.tika.Tika;
import org.apache.tika.metadata.Metadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Holds all data but the content of an attachment to be indexed. The content is retrieved at indexing time, which
 * should save us some memory especially when rebuilding an index for a big wiki.
 * 
 * @version $Id$
 */
public class AttachmentData extends AbstractDocumentData
{
    /** Logging helper object. */
    private static final Logger LOGGER = LoggerFactory.getLogger(AttachmentData.class);

    /** The importance of attachments in general, compared to other documents. */
    private static final float ATTACHMENT_GLOBAL_BOOST = 0.25f;

    /** The importance of the attachment filename. */
    private static final float FILENAME_BOOST = 3f;

    /** How much to weight down fields from the owner document that are not that relevant for attachments. */
    private static final float IRRELEVANT_DOCUMENT_FIELD_BOOST = 0.1f;
	
	/** The importance of the attachment mimetype. */
	private static final float MIMETYPE_BOOST = 3f;

    /** Which fields are relevant for attachments as well and should be kept at their original importance. */
    private static final List<String> RELEVANT_DOCUMENT_FIELDS = new ArrayList<String>();
    static {
        RELEVANT_DOCUMENT_FIELDS.add(IndexFields.DOCUMENT_ID);
        RELEVANT_DOCUMENT_FIELDS.add(IndexFields.DOCUMENT_TYPE);
        RELEVANT_DOCUMENT_FIELDS.add(IndexFields.DOCUMENT_AUTHOR);
        RELEVANT_DOCUMENT_FIELDS.add(IndexFields.FULLTEXT);
    }

    private int size;

    private String filename;
	private String mimetype;

    public AttachmentData(XWikiAttachment attachment, XWikiContext context, boolean deleted)
    {
        super(LucenePlugin.DOCTYPE_ATTACHMENT, attachment.getDoc(), context, deleted);

        setModificationDate(attachment.getDate());
        setAuthor(attachment.getAuthor());
        setSize(attachment.getFilesize());
        setFilename(attachment.getFilename());
		setMimeType(attachment.getMimeType(context));
    }

    public AttachmentData(XWikiDocument document, String filename, XWikiContext context, boolean deleted)
    {
        super(LucenePlugin.DOCTYPE_ATTACHMENT, document, context, deleted);

        setFilename(filename);
    }

    @Override
    public void addDataToLuceneDocument(Document luceneDoc, XWikiContext context) throws XWikiException
    {
        super.addDataToLuceneDocument(luceneDoc, context);

        // Lower the importance of the fields inherited from the document
        List<Fieldable> existingFields = luceneDoc.getFields();
        for (Fieldable f : existingFields) {
            if (!RELEVANT_DOCUMENT_FIELDS.contains(f.name())) {
                f.setBoost(f.getBoost() * IRRELEVANT_DOCUMENT_FIELD_BOOST);
            }
        }

        if (this.filename != null) {
            addFieldToDocument(IndexFields.FILENAME, this.filename, Field.Store.YES, Field.Index.ANALYZED,
                FILENAME_BOOST, luceneDoc);
			addFieldToDocument(IndexFields.MIMETYPE, this.mimetype, Field.Store.YES, Field.Index.ANALYZED, 
			    MIMETYPE_BOOST, luceneDoc);
        }
        // Decrease the global score of attachments
        luceneDoc.setBoost(ATTACHMENT_GLOBAL_BOOST);
    }

    /**
     * @param size The size to set.
     */
    public void setSize(int size)
    {
        this.size = size;
    }

    /**
     * @return The size to set.
     */
    public int getSize()
    {
        return this.size;
    }

    /**
     * @return Returns the filename.
     */
    public String getFilename()
    {
        return this.filename;
    }

    /**
     * @param filename The filename to set.
     */
    public void setFilename(String filename)
    {
        this.filename = filename;
    }
	
	/**
     * @return Returns the mimetype.
     */
	
	public String getMimeType()
    {
    	return this.mimetype;
    }
    
	/**
     * @param mimetype The mimetype to set.
     */
    public void setMimeType(String mimetype)
    {
    	this.mimetype = mimetype;
    }

    /**
     * overridden to append the filename
     * 
     * @see AbstractIndexData#getId()
     */
    @Override
    public String getId()
    {
        return new StringBuffer(super.getId()).append(".file.").append(this.filename).toString();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Return a string containing the result of {@link AbstractIndexData#getFullText} plus the full text content of this
     * attachment, as far as it could be extracted.
     * 
     * @see com.xpn.xwiki.plugin.lucene.AbstractIndexData#getFullText(java.lang.StringBuilder,
     *      com.xpn.xwiki.doc.XWikiDocument, com.xpn.xwiki.XWikiContext)
     */
    @Override
    protected void getFullText(StringBuilder sb, XWikiDocument doc, XWikiContext context)
    {
        super.getFullText(sb, doc, context);

        String contentText = getContentAsText(doc, context);

        if (contentText != null) {
            if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append(contentText);
        }
    }

    private String getContentAsText(XWikiDocument doc, XWikiContext context)
    {
        String contentText = null;

        try {
            XWikiAttachment att = doc.getAttachment(this.filename);

            LOGGER.debug("Start parsing attachement [{}] in document [{}]", this.filename, doc.getDocumentReference());

            Tika tika = new Tika();

            Metadata metadata = new Metadata();
            metadata.set(Metadata.RESOURCE_NAME_KEY, this.filename);

            contentText = StringUtils.lowerCase(tika.parseToString(att.getContentInputStream(context), metadata));
        } catch (Throwable ex) {
            LOGGER.warn("error getting content of attachment [{}] for document [{}]",
                new Object[] {this.filename, doc.getDocumentReference(), ex});
        }

        return contentText;
    }
}
