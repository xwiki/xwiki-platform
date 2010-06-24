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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.xwiki.rendering.syntax.Syntax;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * @version $Id$
 */
public abstract class IndexData
{
    private static final Log LOG = LogFactory.getLog(IndexData.class);

    private String documentTitle;

    private String documentName;

    private String documentSpace;

    private String documentFullName;

    private String fullName;

    private String author;

    private Date creationDate;

    private String creator;

    private String language;

    private Date modificationDate;

    /**
     * name of the wiki this doc belongs to
     */
    private String wiki;

    public IndexData(final XWikiDocument doc, final XWikiContext context)
    {
        setDocumentName(doc.getName());
        setDocumentTitle(doc.getRenderedTitle(Syntax.PLAIN_1_0, context));
        setDocumentSpace(doc.getSpace());
        setDocumentFullName(doc.getFullName());
        setWiki(doc.getWikiName() == null ? context.getDatabase() : doc.getWikiName());
        setFullName(new StringBuffer(this.wiki).append(":").append(this.documentSpace).append(".").append(
            this.documentName).toString());
        setLanguage(doc.getLanguage());
    }

    /**
     * Adds this documents data to a lucene Document instance for indexing.
     * <p>
     * <strong>Short introduction to Lucene field types </strong>
     * </p>
     * <p>
     * Which type of Lucene field is used determines what Lucene does with data and how we can use it for searching and
     * showing search results:
     * </p>
     * <ul>
     * <li>Keyword fields don't get tokenized, but are searchable and stored in the index. This is perfect for fields
     * you want to search in programmatically (like ids and such), and date fields. Since all user-entered queries are
     * tokenized, letting the user search these fields makes almost no sense, except of queries for date fields, where
     * tokenization is useless.</li>
     * <li>the stored text fields are used for short texts which should be searchable by the user, and stored in the
     * index for reconstruction. Perfect for document names, titles, abstracts.</li>
     * <li>the unstored field takes the biggest part of the content - the full text. It is tokenized and indexed, but
     * not stored in the index. This makes sense, since when the user wants to see the full content, he clicks the link
     * to vie the full version of a document, which is then delivered by xwiki.</li>
     * </ul>
     * 
     * @param luceneDoc if not null, this controls which translated version of the content will be indexed. If null, the
     *            content in the default language will be used.
     */
    public void addDataToLuceneDocument(org.apache.lucene.document.Document luceneDoc, XWikiDocument doc,
        XWikiContext context)
    {
        // Keyword fields: stored and indexed, but not tokenized
        // Note: ID field must be UN_TOKENIZED to enable case sensitive IDs
        luceneDoc.add(new Field(IndexFields.DOCUMENT_ID, getId(), Field.Store.YES, Field.Index.NOT_ANALYZED));
        luceneDoc.add(new Field(IndexFields.DOCUMENT_LANGUAGE, this.language, Field.Store.YES, Field.Index.ANALYZED));
        if (!StringUtils.isEmpty(this.wiki)) {
            luceneDoc.add(new Field(IndexFields.DOCUMENT_WIKI, this.wiki, Field.Store.YES, Field.Index.ANALYZED));
        }
        if (getType() != null) {
            luceneDoc.add(new Field(IndexFields.DOCUMENT_TYPE, getType(), Field.Store.YES, Field.Index.ANALYZED));
        }
        if (this.modificationDate != null) {
            luceneDoc.add(new Field(IndexFields.DOCUMENT_DATE, IndexFields.dateToString(this.modificationDate),
                Field.Store.YES, Field.Index.NOT_ANALYZED));
        }
        if (this.creationDate != null) {
            luceneDoc.add(new Field(IndexFields.DOCUMENT_CREATIONDATE, IndexFields.dateToString(this.creationDate),
                Field.Store.YES, Field.Index.NOT_ANALYZED));
        }

        // stored Text fields: tokenized and indexed
        if (this.documentTitle != null) {
            luceneDoc.add(new Field(IndexFields.DOCUMENT_TITLE, this.documentTitle, Field.Store.YES,
                Field.Index.ANALYZED));
        }
        luceneDoc.add(new Field(IndexFields.DOCUMENT_NAME, this.documentName, Field.Store.YES, Field.Index.ANALYZED));
        luceneDoc.add(new Field(IndexFields.DOCUMENT_WEB, this.documentSpace, Field.Store.YES, Field.Index.ANALYZED));
        luceneDoc.add(new Field(IndexFields.DOCUMENT_SPACE, this.documentSpace, Field.Store.YES, Field.Index.ANALYZED));
        luceneDoc.add(new Field(IndexFields.DOCUMENT_FULLNAME, this.documentFullName, Field.Store.YES,
            Field.Index.ANALYZED));
        if (this.author != null) {
            luceneDoc.add(new Field(IndexFields.DOCUMENT_AUTHOR, this.author, Field.Store.YES, Field.Index.ANALYZED));
        }
        if (this.creator != null) {
            luceneDoc.add(new Field(IndexFields.DOCUMENT_CREATOR, this.creator, Field.Store.YES, Field.Index.ANALYZED));
        }

        // UnStored fields: tokenized and indexed, but no reconstruction of
        // original content will be possible from the search result
        try {
            final String ft = getFullText(doc, context);
            if (ft != null) {
                luceneDoc.add(new Field(IndexFields.FULLTEXT, ft, Field.Store.NO, Field.Index.ANALYZED));
            }
        } catch (Exception e) {
            LOG.error("Error extracting fulltext for document [" + this + "]", e);
        }
    }

    /**
     * Builds a Lucene query matching only the document this instance represents. This is used for removing old versions
     * of a document from the index before adding a new one.
     * 
     * @return a query matching the field DOCUMENT_ID to the value of #getId()
     */
    public Query buildQuery()
    {
        return new TermQuery(new Term(IndexFields.DOCUMENT_ID, getId()));
    }

    /**
     * @return string unique to this document across all languages and virtual wikis
     */
    public String getId()
    {
        StringBuffer retval = new StringBuffer();

        if (!StringUtils.isEmpty(this.wiki)) {
            retval.append(this.wiki).append(":");
        }
        retval.append(this.documentSpace).append(".");
        retval.append(this.documentName).append(".");
        retval.append(this.language);

        return retval.toString();
    }

    /**
     * @return String of documentName, documentWeb, author and creator
     */
    public String getFullText(XWikiDocument doc, XWikiContext context)
    {
        StringBuilder sb = new StringBuilder();

        getFullText(sb, doc, context);

        return sb.toString();
    }

    protected void getFullText(StringBuilder sb, XWikiDocument doc, XWikiContext context)
    {
        sb.append(this.documentName).append(" ").append(this.documentSpace);

        if (!StringUtils.isEmpty(this.author)) {
            sb.append(" ").append(this.author);
        }

        if (!StringUtils.isEmpty(this.creator)) {
            sb.append(" ").append(this.creator);
        }
    }

    public abstract String getType();

    public String toString()
    {
        return getId();
    }

    /**
     * @param author The author to set.
     */
    public void setAuthor(String author)
    {
        this.author = author;
    }

    /**
     * @param documentTitle the document title
     */
    public void setDocumentTitle(String documentTitle)
    {
        this.documentTitle = documentTitle;
    }

    /**
     * @param documentName The documentName to set.
     */
    public void setDocumentName(String documentName)
    {
        this.documentName = documentName;
    }

    /**
     * @param documentWeb The documentWeb to set.
     * @deprecated use {@link #setDocumentSpace(String)} instead
     */
    @Deprecated
    public void setDocumentWeb(String documentWeb)
    {
        setDocumentSpace(documentWeb);
    }

    public void setDocumentSpace(String documentSpace)
    {
        this.documentSpace = documentSpace;
    }

    /**
     * @param documentFullName The documentFullName to set.
     */
    public void setDocumentFullName(String documentFullName)
    {
        this.documentFullName = documentFullName;
    }

    /**
     * @param modificationDate The modificationDate to set.
     */
    public void setModificationDate(Date modificationDate)
    {
        this.modificationDate = modificationDate;
    }

    public String getDocumentTitle()
    {
        return this.documentTitle;
    }

    public String getDocumentName()
    {
        return this.documentName;
    }

    /**
     * @deprecated use {@link #getDocumentSpace()} instead
     */
    @Deprecated
    public String getDocumentWeb()
    {
        return getDocumentSpace();
    }

    public String getDocumentSpace()
    {
        return this.documentSpace;
    }

    public String getDocumentFullName()
    {
        return this.documentFullName;
    }

    public String getWiki()
    {
        return this.wiki;
    }

    public void setWiki(String wiki)
    {
        this.wiki = wiki;
    }

    public Date getCreationDate()
    {
        return this.creationDate;
    }

    public void setCreationDate(Date creationDate)
    {
        this.creationDate = creationDate;
    }

    public String getCreator()
    {
        return this.creator;
    }

    public void setCreator(String creator)
    {
        this.creator = creator;
    }

    public String getFullName()
    {
        return this.fullName;
    }

    public void setFullName(String fullName)
    {
        this.fullName = fullName;
    }

    public String getLanguage()
    {
        return this.language;
    }

    public void setLanguage(String lang)
    {
        if (!StringUtils.isEmpty(lang)) {
            this.language = lang;
        } else {
            this.language = "default";
        }
    }
}
