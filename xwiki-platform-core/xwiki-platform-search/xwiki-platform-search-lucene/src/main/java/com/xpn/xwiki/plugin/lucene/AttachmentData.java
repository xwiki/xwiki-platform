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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.tika.Tika;
import org.apache.tika.metadata.Metadata;

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
    private static final Log LOG = LogFactory.getLog(AttachmentData.class);

    private int size;

    private String filename;

    public AttachmentData(XWikiAttachment attachment, XWikiContext context, boolean deleted)
    {
        super(LucenePlugin.DOCTYPE_ATTACHMENT, attachment.getDoc(), context, deleted);

        setModificationDate(attachment.getDate());
        setAuthor(attachment.getAuthor());
        setSize(attachment.getFilesize());
        setFilename(attachment.getFilename());
    }

    public AttachmentData(XWikiDocument document, String filename, XWikiContext context, boolean deleted)
    {
        super(LucenePlugin.DOCTYPE_ATTACHMENT, document, context, deleted);

        setFilename(filename);
    }

    public void addDataToLuceneDocument(Document luceneDoc, XWikiContext context) throws XWikiException
    {
        super.addDataToLuceneDocument(luceneDoc, context);

        if (this.filename != null) {
            luceneDoc.add(new Field(IndexFields.FILENAME, filename, Field.Store.YES, Field.Index.ANALYZED));
        }
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
     * overridden to append the filename
     * 
     * @see AbstractIndexData#getId()
     */
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
            sb.append(getContentAsText(doc, context));
        }
    }

    private String getContentAsText(XWikiDocument doc, XWikiContext context)
    {
        String contentText = null;

        try {
            XWikiAttachment att = doc.getAttachment(this.filename);

            LOG.debug("Start parsing attachement [" + this.filename + "] in document [" + doc.getDocumentReference()
                + "]");

            Tika tika = new Tika();

            Metadata metadata = new Metadata();
            metadata.set(Metadata.RESOURCE_NAME_KEY, this.filename);

            contentText = this.filename + " " + tika.parseToString(att.getContentInputStream(context), metadata);
        } catch (Throwable e) {
            LOG.warn(
                "error getting content of attachment [" + this.filename + "] for document ["
                    + doc.getDocumentReference() + "]", e);
        }

        return contentText;
    }
}
