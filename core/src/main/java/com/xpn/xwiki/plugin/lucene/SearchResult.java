/*
 * 
 * ===================================================================
 *
 * Copyright (c) 2005 Jens Krämer, All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details, published at
 * http://www.gnu.org/copyleft/gpl.html or in gpl.txt in the
 * root folder of this distribution.
 *
 * Created on 25.01.2005
 *
 */

package com.xpn.xwiki.plugin.lucene;
import java.util.Date;

import org.apache.log4j.Logger;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;

/**
 * Result of a search. The Plugin will return a collection of these for display
 * on the search page.
 * @author <a href="mailto:jk@jkraemer.net">Jens Krämer </a>
 */
public class SearchResult {
    private float               score;
    private String              name;
    private String              wiki;
    private String              web;
    private String              url;
    private String              filename;
    private String              type;
    private String              author;
    private String              language;
    private Date                date;
    private Date                creationDate;
    private String              creator;
    private static final Logger LOG = Logger.getLogger (SearchResult.class);

    /**
     * @todo add fallback for unknown index field names (read values into a map
     *       accessible from search results page) This would be useful for
     *       integration of external indexes where the field names dont match
     *       ours.
     * @param doc
     * @param score
     * @todo TODO: to be more flexible make a factory to construct different
     *       kinds of searchresults, esp. for external indexes and custom
     *       implementations of searchresults
     */
    public SearchResult (org.apache.lucene.document.Document doc, float score, com.xpn.xwiki.api.XWiki xwiki)
    {
        this.score = score;
        name = doc.get (IndexFields.DOCUMENT_NAME);
        web = doc.get (IndexFields.DOCUMENT_WEB);
        wiki = doc.get (IndexFields.DOCUMENT_WIKI);
        type = doc.get (IndexFields.DOCUMENT_TYPE);
        author = doc.get (IndexFields.DOCUMENT_AUTHOR);
        creator = doc.get (IndexFields.DOCUMENT_CREATOR);
        language = doc.get (IndexFields.DOCUMENT_LANGUAGE);
        date = IndexFields.stringToDate (doc.get (IndexFields.DOCUMENT_DATE));
        creationDate = IndexFields.stringToDate (doc.get (IndexFields.DOCUMENT_CREATIONDATE));
        if (LucenePlugin.DOCTYPE_ATTACHMENT.equals (type))
        {
            filename = doc.get (IndexFields.FILENAME);
            Document document;
            final String fullDocName = new StringBuffer (wiki).append (":").append (web).append (".")
                    .append (name).toString ();
            try
            {
                document = xwiki.getDocument (fullDocName);
                url = document.getAttachmentURL (filename, "download");
            } catch (XWikiException e)
            {
                LOG.error ("error retrieving url for attachment " + filename + " of document " + fullDocName);
                e.printStackTrace ();
            }
        }
    }

    /**
     * @return Returns the name of the user who last modified the document.
     */
    public String getAuthor ()
    {
        return author;
    }

    /**
     * @return Returns the date of last modification.
     */
    public Date getDate ()
    {
        return date;
    }

    /**
     * @return Returns the filename, only used for Attachments (see
     *         {@link #getType()})
     */
    public String getFilename ()
    {
        return filename;
    }

    /**
     * @return Returns the name of the document.
     */
    public String getName ()
    {
        return name;
    }

    /**
     * @return Returns the score of this search result as computed by lucene. Is
     *         a float between zero and 1.
     */
    public float getScore ()
    {
        return score;
    }

    /**
     * @return Returns the type of the document, atm this can be either
     *         <code>wikipage</code> or <code>attachment</code>.
     */
    public String getType ()
    {
        return type;
    }

    /**
     * @return Returns the url to access the document.
     */
    public String getUrl ()
    {
        return url;
    }

    /**
     * @return Returns the web the document belongs to.
     */
    public String getWeb ()
    {
        return web;
    }

    /**
     * @return the language of the Document, i.e. <code>de</code> or
     *         <code>en</code>,<code>default</code> if no language was set
     *         at indexing time.
     */
    public String getLanguage ()
    {
        return language;
    }

    /**
     * @return creationDate of this document
     */
    public Date getCreationDate ()
    {
        return creationDate;
    }

    /**
     * @return Username of the creator of the document
     */
    public String getCreator ()
    {
        return creator;
    }

    public void setUrl (String url)
    {
        this.url = url;
    }

    public String getWiki ()
    {
        return wiki;
    }

    /**
     * @return true when this result points to wiki content (attachment or a
     *         wiki page)
     */
    public boolean isWikiContent ()
    {
        return (LucenePlugin.DOCTYPE_WIKIPAGE.equals (type) || LucenePlugin.DOCTYPE_ATTACHMENT.equals (type));
    }
}
