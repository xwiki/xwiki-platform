/*
 * Copyright 2005-2007, XpertNet SARL, and individual contributors as
 * indicated by the contributors.txt.
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

import org.apache.log4j.Logger;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * @version $Id: $
 */
public abstract class IndexData
{
    private static final Logger LOG = Logger.getLogger(IndexData.class);

    private String documentName;

    private String documentWeb;

    private String fullName;

    private String author;

    private Date creationDate;

    private String creator;

    private String language;

    private Date modificationDate;

    /**
     * name of the virtual wiki this doc belongs to
     */
    private String wiki;

    public IndexData(final XWikiDocument doc, final XWikiContext context)
    {
        setDocumentName(doc.getName());
        setDocumentWeb(doc.getSpace());
        setWiki(context.getDatabase());
        setFullName(new StringBuffer(wiki).append(":").append(documentWeb).append(".")
            .append(documentName).toString());
        setLanguage(doc.getLanguage());
    }

    /**
     * Adds this documents data to a lucene Document instance for indexing. <p> <strong>Short
     * introduction to Lucene field types </strong> </p> <p> Which type of Lucene field is used
     * determines what Lucene does with data and how we can use it for searching and showing search
     * results: </p> <ul> <li>Keyword fields don't get tokenized, but are searchable and stored in
     * the index. This is perfect for fields you want to search in programmatically (like ids and
     * such), and date fields. Since all user-entered queries are tokenized, letting the user search
     * these fields makes almost no sense, except of queries for date fields, where tokenization is
     * useless.</li> <li>the stored text fields are used for short texts which should be searchable
     * by the user, and stored in the index for reconstruction. Perfect for document names, titles,
     * abstracts.</li> <li>the unstored field takes the biggest part of the content - the full text.
     * It is tokenized and indexed, but not stored in the index. This makes sense, since when the
     * user wants to see the full content, he clicks the link to vie the full version of a document,
     * which is then delivered by xwiki.</li> </ul>
     *
     * @param luceneDoc if not null, this controls which translated version of the content will be
     * indexed. If null, the content in the default language will be used.
     */
    public void addDataToLuceneDocument(org.apache.lucene.document.Document luceneDoc,
        XWikiDocument doc,
        XWikiContext context)
    {
        // Keyword fields: stored and indexed, but not tokenized
        // Note: ID field must be UN_TOKENIZED to enable case sensitive IDs
        luceneDoc.add(
            new Field(IndexFields.DOCUMENT_ID, getId(), Field.Store.YES, Field.Index.UN_TOKENIZED));
        luceneDoc.add(new Field(IndexFields.DOCUMENT_LANGUAGE, this.language, Field.Store.YES,
            Field.Index.TOKENIZED));
        if (wiki != null && wiki.length() > 0) {
            luceneDoc.add(
                new Field(IndexFields.DOCUMENT_WIKI, wiki, Field.Store.YES, Field.Index.TOKENIZED));
        }
        if (getType() != null) {
            luceneDoc.add(new Field(IndexFields.DOCUMENT_TYPE, getType(),
                Field.Store.YES, Field.Index.TOKENIZED));
        }
        if (modificationDate != null) {
            luceneDoc.add(new Field(IndexFields.DOCUMENT_DATE, IndexFields
                .dateToString(modificationDate), Field.Store.YES, Field.Index.NO));
        }
        if (creationDate != null) {
            luceneDoc.add(new Field(IndexFields.DOCUMENT_CREATIONDATE, IndexFields
                .dateToString(creationDate), Field.Store.YES, Field.Index.NO));
        }

        // stored Text fields: tokenized and indexed
        luceneDoc.add(new Field(IndexFields.DOCUMENT_NAME, documentName, Field.Store.YES,
            Field.Index.TOKENIZED));
        luceneDoc.add(new Field(IndexFields.DOCUMENT_WEB, documentWeb, Field.Store.YES,
            Field.Index.TOKENIZED));
        if (author != null) {
            luceneDoc.add(
                new Field(IndexFields.DOCUMENT_AUTHOR, author, Field.Store.YES,
                    Field.Index.TOKENIZED));
        }
        if (creator != null) {
            luceneDoc.add(new Field(IndexFields.DOCUMENT_CREATOR, creator,
                Field.Store.YES, Field.Index.TOKENIZED));
        }

        // UnStored fields: tokenized and indexed, but no reconstruction of
        // original content will be possible from the search result
        try {
            final String ft = getFullText(doc, context);
            if (ft != null) {
                luceneDoc
                    .add(
                        new Field(IndexFields.FULLTEXT, ft, Field.Store.NO, Field.Index.TOKENIZED));
            }
        } catch (Exception e) {
            LOG.error("error extracting fulltext for document " + this, e);
        }
    }

    /**
     * Builds a Lucene query matching only the document this instance represents. This is used for
     * removing old versions of a document from the index before adding a new one.
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
        if (wiki != null && wiki.length() > 0) {
            retval.append(wiki).append(":");
        }
        retval.append(documentWeb).append(".");
        retval.append(documentName).append(".");
        retval.append(language);
        return retval.toString();
    }

    /**
     * @return String of documentName, documentWeb, author and creator
     */
    public String getFullText(XWikiDocument doc, XWikiContext context)
    {
        StringBuffer sb = new StringBuffer(documentName).append(" ").append(documentWeb).append(" ")
            .append(author).append(creator);
        return sb.toString();
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
     * @param documentName The documentName to set.
     */
    public void setDocumentName(String documentName)
    {
        this.documentName = documentName;
    }

    /**
     * @param documentWeb The documentWeb to set.
     */
    public void setDocumentWeb(String documentWeb)
    {
        this.documentWeb = documentWeb;
    }

    /**
     * @param modificationDate The modificationDate to set.
     */
    public void setModificationDate(Date modificationDate)
    {
        this.modificationDate = modificationDate;
    }

    public String getDocumentName()
    {
        return documentName;
    }

    public String getDocumentWeb()
    {
        return documentWeb;
    }

    public String getWiki()
    {
        return wiki;
    }

    public void setWiki(String wiki)
    {
        this.wiki = wiki;
    }

    public Date getCreationDate()
    {
        return creationDate;
    }

    public void setCreationDate(Date creationDate)
    {
        this.creationDate = creationDate;
    }

    public String getCreator()
    {
        return creator;
    }

    public void setCreator(String creator)
    {
        this.creator = creator;
    }

    /**
     * @return
     */
    public String getFullName()
    {
        return fullName;
    }

    public void setFullName(String fullName)
    {
        this.fullName = fullName;
    }

    public String getLanguage()
    {
        return language;
    }

    public void setLanguage(String lang)
    {
        if (lang != null && lang.length() > 0) {
            this.language = lang;
        } else {
            this.language = "default";
        }
    }
}
