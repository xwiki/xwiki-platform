/*
 * 
 * ===================================================================
 *
 * Copyright (c) 2005 Jens Krämer, All rights reserved.
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
 *
 * Created on 25.01.2005
 *
 */
package net.jkraemer.xwiki.plugins.lucene;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.TermQuery;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Holds all data but the content of an attachment to be indexed. The content is
 * retrieved at indexing time, which should save us some memory especially when
 * rebuilding an index for a big wiki.
 * @author <a href="mailto:jk@jkraemer.net">Jens Krämer </a>
 */
public class AttachmentData extends IndexData
{
    /**
     * Mapping from common file name endings to mime types. This is uses as a
     * fallback when text extraction by using the mime type delivered by xwiki
     * doesn't work.
     */
    static final Map            MIMETYPES = new HashMap ();
    static
    {
        MIMETYPES.put ("pdf", "application/pdf");
        MIMETYPES.put ("doc", "application/msword");
        MIMETYPES.put ("sxw", "application/vnd.sun.xml.writer");
        MIMETYPES.put ("xml", "text/xml");
        MIMETYPES.put ("txt", "text/plain");
    }

    private static final Logger LOG       = Logger.getLogger (AttachmentData.class);
    private int                 size;
    private String              filename;

    /**
     * @param attachment
     * @param context
     */
    public AttachmentData (final XWikiDocument document, final XWikiAttachment attachment,
                           final XWikiContext context)
    {
        super (attachment.getDoc (), context);
        setModificationDate (attachment.getDate ());
        setAuthor (attachment.getAuthor ());
        setSize (attachment.getFilesize ());
        setFilename (attachment.getFilename ());
    }

    /**
     * @see net.jkraemer.xwiki.plugins.lucene.IndexData#addDataToLuceneDocument(org.apache.lucene.document.Document)
     */
    public void addDataToLuceneDocument (Document luceneDoc, XWikiDocument doc, XWikiContext context)
    {
        super.addDataToLuceneDocument (luceneDoc, doc, context);
        if (filename != null) luceneDoc.add (Field.Keyword (IndexFields.FILENAME, filename));
    }

    /**
     * @param size
     *            The size to set.
     */
    public void setSize (int size)
    {
        this.size = size;
    }

    /**
     * @see net.jkraemer.xwiki.plugins.lucene.IndexData#getType()
     */
    public String getType ()
    {
        return LucenePlugin.DOCTYPE_ATTACHMENT;
    }

    /**
     * @return Returns the filename.
     */
    public String getFilename ()
    {
        return filename;
    }

    /**
     * @param filename
     *            The filename to set.
     */
    public void setFilename (String filename)
    {
        this.filename = filename;
    }

    /**
     * overridden to append the filename
     * @see net.jkraemer.xwiki.plugins.lucene.IndexData#getId()
     */
    public String getId ()
    {
        return new StringBuffer (super.getId ()).append (".").append (filename).toString ();
    }

    /**
     * @return a string containing the result of
     *         {@link IndexData#getFullText(XWikiDocument, XWikiContext)}plus
     *         the full text content of this attachment, as far as it could be
     *         extracted.
     */
    public String getFullText (XWikiDocument doc, XWikiContext context)
    {
        StringBuffer retval = new StringBuffer (super.getFullText (doc, context));

        String contentText = null;
        contentText = getContentAsText (doc, context);

        if (contentText != null)
        {
            retval.append (" ").append (contentText).toString ();
        }
        return retval.toString ();
    }

    /**
     * @param doc
     * @param context
     * @param contentText
     * @return
     */
    private String getContentAsText (XWikiDocument doc, XWikiContext context)
    {
        String contentText = null;
        try
        {
            XWikiAttachment att = doc.getAttachment (filename);
            if (LOG.isDebugEnabled ()) LOG.debug ("have attachment for filename " + filename + ": " + att);
            byte[] content = att.getContent (context);
            if (filename != null)
            {
                String[] nameParts = filename.split ("\\.");
                if (nameParts.length > 1)
                {
                    contentText = TextExtractor.getText (content, (String) MIMETYPES
                            .get (nameParts[nameParts.length - 1].toLowerCase ()));
                }
            }
        } catch (Exception e)
        {
            LOG.error ("error getting content of attachment", e);
            e.printStackTrace ();
        }
        return contentText;
    }

}
