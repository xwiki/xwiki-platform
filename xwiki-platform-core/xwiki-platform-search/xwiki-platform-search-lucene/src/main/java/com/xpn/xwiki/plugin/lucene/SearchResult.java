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

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;

/**
 * Result of a search. The Plugin will return a collection of these for display on the search page.
 * 
 * @version $Id$
 */
public class SearchResult
{
    private String id;

    private float score;

    private String title;

    private String name;

    private String wiki;

    private String space;

    private String fullName;

    private String url;

    private String filename;

    private String[] objects;

    private String type;

    private String author;

    private String language;

    private Date date;

    private Date creationDate;

    private String creator;

    private boolean hidden;

    private static final Logger LOGGER = LoggerFactory.getLogger(SearchResult.class);

    /**
     * @todo add fallback for unknown index field names (read values into a map accessible from search results page)
     *       This would be useful for integration of external indexes where the field names dont match ours.
     * @todo TODO: to be more flexible make a factory to construct different kinds of searchresults, esp. for external
     *       indexes and custom implementations of searchresults
     */
    public SearchResult(org.apache.lucene.document.Document doc, float score, com.xpn.xwiki.api.XWiki xwiki)
    {
        this.score = score;
        this.id = doc.get(IndexFields.DOCUMENT_ID);
        this.title = doc.get(IndexFields.DOCUMENT_TITLE);
        this.name = doc.get(IndexFields.DOCUMENT_NAME);
        this.space = doc.get(IndexFields.DOCUMENT_SPACE);
        this.wiki = doc.get(IndexFields.DOCUMENT_WIKI);
        this.fullName = doc.get(IndexFields.DOCUMENT_FULLNAME);
        this.type = doc.get(IndexFields.DOCUMENT_TYPE);
        this.author = doc.get(IndexFields.DOCUMENT_AUTHOR);
        this.creator = doc.get(IndexFields.DOCUMENT_CREATOR);
        this.language = doc.get(IndexFields.DOCUMENT_LANGUAGE);
        this.date = IndexFields.stringToDate(doc.get(IndexFields.DOCUMENT_DATE));
        this.creationDate = IndexFields.stringToDate(doc.get(IndexFields.DOCUMENT_CREATIONDATE));
        this.hidden = IndexFields.stringToBoolean(doc.get(IndexFields.DOCUMENT_HIDDEN));
        if (LucenePlugin.DOCTYPE_ATTACHMENT.equals(this.type)) {
            this.filename = doc.get(IndexFields.FILENAME);
            Document document;
            final String fullDocName =
                new StringBuffer(this.wiki).append(":").append(this.space).append(".").append(this.name).toString();
            try {
                document = xwiki.getDocument(fullDocName);
                if (document != null) {
                    this.url = document.getAttachmentURL(this.filename, "download");
                }
            } catch (XWikiException e) {
                LOGGER.error("error retrieving url for attachment [{}] of document [{}]",
                    new Object[] {this.filename, fullDocName, e});
            }
        } else {
            this.objects = doc.getValues("object");
        }
    }

    /**
     * @return the document id as indexed
     */
    public String getId()
    {
        return this.id;
    }

    /**
     * @return Returns the name of the user who last modified the document.
     */
    public String getAuthor()
    {
        return this.author;
    }

    /**
     * @return Returns the date of last modification.
     */
    public Date getDate()
    {
        return this.date;
    }

    /**
     * @return Returns the filename, only used for Attachments (see {@link #getType()})
     */
    public String getFilename()
    {
        return this.filename;
    }

    /**
     * @return the title of the document.
     */
    public String getTitle()
    {
        return this.title;
    }

    /**
     * @return Returns the name of the document.
     */
    public String getName()
    {
        return this.name;
    }

    /**
     * @return Returns the score of this search result as computed by lucene. Is a float between zero and 1.
     */
    public float getScore()
    {
        return this.score;
    }

    /**
     * @return Returns the type of the document, atm this can be either <code>wikipage</code> or <code>attachment</code>
     *         .
     */
    public String getType()
    {
        return this.type;
    }

    /**
     * @return Returns the url to access the document.
     */
    public String getUrl()
    {
        return this.url;
    }

    /**
     * @return Returns the space the document belongs to.
     * @deprecated Use {@link #getSpace} instead.
     */
    @Deprecated
    public String getWeb()
    {
        return this.space;
    }

    /**
     * @return Returns the space the document belongs to.
     */
    public String getSpace()
    {
        return this.space;
    }

    /**
     * @return the language of the Document, i.e. <code>de</code> or <code>en</code>,<code>default</code> if no language
     *         was set at indexing time.
     */
    public String getLanguage()
    {
        return this.language;
    }

    /**
     * @return creationDate of this document
     */
    public Date getCreationDate()
    {
        return this.creationDate;
    }

    /**
     * @return Username of the creator of the document
     */
    public String getCreator()
    {
        return this.creator;
    }

    public void setUrl(String url)
    {
        this.url = url;
    }

    public String getWiki()
    {
        return this.wiki;
    }

    public String getFullName()
    {
        return this.fullName;
    }

    public String[] getObjects()
    {
        return this.objects;
    }

    /**
     * @return true when this result points to wiki content (attachment, wiki page or object)
     */
    public boolean isWikiContent()
    {
        return (LucenePlugin.DOCTYPE_WIKIPAGE.equals(this.type) || LucenePlugin.DOCTYPE_ATTACHMENT.equals(this.type));
    }

    /**
     * @return true if the result is marked as "hidden", false otherwise.
     */
    public boolean isHidden()
    {
        return hidden;
    }

}
