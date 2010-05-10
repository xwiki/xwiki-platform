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
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Holds all data but the content of an attachment to be indexed. The content is retrieved at indexing time, which
 * should save us some memory especially when rebuilding an index for a big wiki.
 * 
 * @version $Id$
 */
public class AttachmentData extends IndexData
{
    private static final Log LOG = LogFactory.getLog(AttachmentData.class);

    private int size;

    private String filename;

    public AttachmentData(final XWikiDocument document, final XWikiAttachment attachment, final XWikiContext context)
    {
        super(attachment.getDoc(), context);

        setModificationDate(attachment.getDate());
        setAuthor(attachment.getAuthor());
        setSize(attachment.getFilesize());
        setFilename(attachment.getFilename());
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.plugin.lucene.IndexData#addDataToLuceneDocument(org.apache.lucene.document.Document,
     *      com.xpn.xwiki.doc.XWikiDocument, com.xpn.xwiki.XWikiContext)
     */
    public void addDataToLuceneDocument(Document luceneDoc, XWikiDocument doc, XWikiContext context)
    {
        super.addDataToLuceneDocument(luceneDoc, doc, context);

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
        return size;
    }

    /**
     * @see IndexData#getType()
     */
    public String getType()
    {
        return LucenePlugin.DOCTYPE_ATTACHMENT;
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
     * @see IndexData#getId()
     */
    public String getId()
    {
        return new StringBuffer(super.getId()).append(".file.").append(this.filename).toString();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Return a string containing the result of {@link IndexData#getFullText} plus the full text content of this
     * attachment, as far as it could be extracted.
     * 
     * @see com.xpn.xwiki.plugin.lucene.IndexData#getFullText(java.lang.StringBuilder, com.xpn.xwiki.doc.XWikiDocument,
     *      com.xpn.xwiki.XWikiContext)
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

            LOG.debug("Start parsing attachement [" + this.filename + "] in document [" + doc.getPrefixedFullName()
                + "]");

            Parser parser = new AutoDetectParser();
            BodyContentHandler contenthandler = new BodyContentHandler();
            Metadata metadata = new Metadata();
            metadata.set(Metadata.RESOURCE_NAME_KEY, this.filename);
            ParseContext parseContext = new ParseContext();
            parseContext.set(Parser.class, parser);

            parser.parse(att.getContentInputStream(context), contenthandler, metadata, parseContext);

            String title = metadata.get(Metadata.TITLE);

            LOG.debug("* Type: [" + metadata.get(Metadata.CONTENT_TYPE) + "]");
            LOG.debug("* Title: [" + title + "]");
            LOG.debug("* Author: [" + metadata.get(Metadata.AUTHOR) + "]");

            return this.filename + (title != null ? " " + metadata.get(Metadata.TITLE) : "") + " "
                + contenthandler.toString();
        } catch (Throwable e) {
            LOG.warn("error getting content of attachment [" + this.filename + "] for document ["
                + doc.getPrefixedFullName() + "]", e);
        }

        return contentText;
    }
}
