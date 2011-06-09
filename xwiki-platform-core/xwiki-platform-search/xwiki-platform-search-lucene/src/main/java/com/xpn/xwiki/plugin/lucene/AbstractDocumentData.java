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
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.Term;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.rendering.syntax.Syntax;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.web.Utils;

/**
 * @version $Id$
 * @since 1.23
 */
public abstract class AbstractDocumentData extends AbstractIndexData
{
    private static final Log LOG = LogFactory.getLog(AbstractDocumentData.class);

    private String version;

    private String documentTitle;

    private String author;

    private String creator;

    private String language;

    private Date creationDate;

    private Date modificationDate;

    public AbstractDocumentData(String type, XWikiDocument doc, XWikiContext context, boolean deleted)
    {
        super(type, doc.getDocumentReference(), deleted);

        setVersion(doc.getVersion());
        setDocumentTitle(doc.getRenderedTitle(Syntax.PLAIN_1_0, context));
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
    public void addDataToLuceneDocument(Document luceneDoc, XWikiContext context) throws XWikiException
    {
        /*
         * XXX Is it not possible to obtain the right translation directly?
         */
        XWikiDocument doc = context.getWiki().getDocument(getDocumentReference(), context);

        if (getLanguage() != null && !getLanguage().equals("")) {
            doc = doc.getTranslatedDocument(getLanguage(), context);
        }

        addDocumentDataToLuceneDocument(luceneDoc, doc, context);
    }

    public void addDocumentDataToLuceneDocument(Document luceneDoc, XWikiDocument doc, XWikiContext context)
    {
        // Keyword fields: stored and indexed, but not tokenized
        // Note: ID field must be UN_TOKENIZED to enable case sensitive IDs
        luceneDoc.add(new Field(IndexFields.DOCUMENT_ID, getId(), Field.Store.YES, Field.Index.NOT_ANALYZED));
        luceneDoc.add(new Field(IndexFields.DOCUMENT_LANGUAGE, getLanguage(), Field.Store.YES, Field.Index.ANALYZED));
        luceneDoc.add(new Field(IndexFields.DOCUMENT_WIKI, getWiki(), Field.Store.YES, Field.Index.ANALYZED));
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
        luceneDoc.add(new Field(IndexFields.DOCUMENT_NAME, getDocumentName(), Field.Store.YES, Field.Index.ANALYZED));
        luceneDoc.add(new Field(IndexFields.DOCUMENT_WEB, getDocumentSpace(), Field.Store.YES, Field.Index.ANALYZED));
        luceneDoc.add(new Field(IndexFields.DOCUMENT_SPACE, getDocumentSpace(), Field.Store.YES, Field.Index.ANALYZED));
        luceneDoc.add(new Field(IndexFields.DOCUMENT_FULLNAME, getDocumentFullName(), Field.Store.YES,
            Field.Index.ANALYZED));

        luceneDoc.add(new Field(IndexFields.DOCUMENT_VERSION, getVersion(), Field.Store.YES, Field.Index.ANALYZED));

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
     * @return string unique to this document across all languages and virtual wikis
     */
    public String getId()
    {
        StringBuilder retval = new StringBuilder();

        retval.append(getFullName());
        retval.append(".");
        retval.append(getLanguage());

        return retval.toString();
    }

    public Term getTerm()
    {
        return new Term(IndexFields.DOCUMENT_ID, getId());
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
        sb.append(getDocumentName()).append(" ").append(getDocumentSpace());

        if (!StringUtils.isEmpty(this.author)) {
            sb.append(" ").append(this.author);
        }

        if (!StringUtils.isEmpty(this.creator)) {
            sb.append(" ").append(this.creator);
        }
    }

    /**
     * @param author The author to set.
     */
    public void setAuthor(String author)
    {
        this.author = author;
    }

    /**
     * @param version the version of the document
     */
    public void setVersion(String version)
    {
        this.version = version;
    }

    /**
     * @param documentTitle the document title
     */
    public void setDocumentTitle(String documentTitle)
    {
        this.documentTitle = documentTitle;
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

    public DocumentReference getDocumentReference()
    {
        return (DocumentReference) getEntityReference();
    }

    public String getDocumentName()
    {
        return getEntityName(EntityType.DOCUMENT);
    }

    public String getDocumentSpace()
    {
        return getEntityName(EntityType.SPACE);
    }

    public String getWiki()
    {
        return getEntityName(EntityType.WIKI);
    }

    public String getDocumentFullName()
    {
        return (String) Utils.getComponent(EntityReferenceSerializer.class, "local").serialize(getEntityReference());
    }

    public String getVersion()
    {
        return this.version;
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
        return (String) Utils.getComponent(EntityReferenceSerializer.class).serialize(getEntityReference());
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

    // Object

    public String toString()
    {
        return getId();
    }
}
