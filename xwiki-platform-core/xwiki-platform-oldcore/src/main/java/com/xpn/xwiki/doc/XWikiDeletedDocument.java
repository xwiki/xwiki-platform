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
 *
 */
package com.xpn.xwiki.doc;

import java.util.Date;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.util.AbstractSimpleClass;

/**
 * Archive of deleted document, stored in {@link com.xpn.xwiki.store.XWikiRecycleBinStoreInterface} Immutable, because
 * we don't need modify deleted document.
 * 
 * @version $Id$
 * @since 1.2M1
 */
public class XWikiDeletedDocument extends AbstractSimpleClass
{
    /**
     * Synthetic id.
     */
    private long id;

    /**
     * @see XWikiDocument#getFullName()
     */
    private String fullName;

    /**
     * @see XWikiDocument#getLanguage()
     */
    private String language;

    /**
     * date of delete action.
     */
    private Date date;

    /**
     * @see XWikiDeletedDocument#getDeleter()
     */
    private String deleter;

    /**
     * @see XWikiDocument#toXML(XWikiContext)
     */
    private String xml;

    /**
     * Default constructor. Used only in hibernate.
     */
    protected XWikiDeletedDocument()
    {
    }

    /**
     * @param doc - deleted document
     * @param deleter - user which delete document
     * @param deleteDate - date of delete action
     * @param context - used for environment
     * @throws XWikiException if any error
     */
    public XWikiDeletedDocument(XWikiDocument doc, String deleter, Date deleteDate, XWikiContext context)
        throws XWikiException
    {
        this.fullName = doc.getFullName();
        this.language = doc.getLanguage();
        this.deleter = deleter;
        this.date = deleteDate;
        setDocument(doc, context);
    }

    /**
     * @return the synthetic id of this deleted document. unique only for document.
     */
    public long getId()
    {
        return this.id;
    }

    /**
     * @param id - the synthetic id to set. used only in hibernate.
     */
    protected void setId(long id)
    {
        this.id = id;
    }

    /**
     * @return {@link XWikiDocument#getFullName()}
     */
    public String getFullName()
    {
        return this.fullName;
    }

    /**
     * @param docFullName - {@link XWikiDocument#getFullName()} to set
     */
    protected void setFullName(String docFullName)
    {
        this.fullName = docFullName;
    }

    /**
     * @return {@link XWikiDocument#getLanguage()}
     */
    public String getLanguage()
    {
        return this.language;
    }

    /**
     * @param language - {@link XWikiDocument#getLanguage()} to set
     */
    protected void setLanguage(String language)
    {
        this.language = language;
    }

    /**
     * @return the date of delete action
     */
    public Date getDate()
    {
        return this.date;
    }

    /**
     * @param date - the date of delete action to set
     */
    protected void setDate(Date date)
    {
        this.date = date;
    }

    /**
     * @return the user which has removed the document
     */
    public String getDeleter()
    {
        return this.deleter;
    }

    /**
     * @param deleter - the user which has removed the document to set
     */
    protected void setDeleter(String deleter)
    {
        this.deleter = deleter;
    }

    /** @return xml serialization of {@link XWikiDocument} */
    public String getXml()
    {
        return this.xml;
    }

    /** @param xml - xml serialization of {@link XWikiDocument} */
    protected void setXml(String xml)
    {
        this.xml = xml;
    }

    /**
     * export {@link XWikiDocument} to {@link XWikiDeletedDocument}.
     * 
     * @param doc - deleted document
     * @param context - used in {@link XWikiDocument#toXML(XWikiContext)}
     * @throws XWikiException in error in {@link XWikiDocument#toXML(XWikiContext)}
     */
    protected void setDocument(XWikiDocument doc, XWikiContext context) throws XWikiException
    {
        setXml(doc.toFullXML(context));
    }

    /**
     * @return restored document
     * @param doc - restore to this document, if not null
     * @param context - may be useful in future
     * @throws XWikiException if error in {@link XWikiDocument#fromXML(String)}
     */
    public XWikiDocument restoreDocument(XWikiDocument doc, XWikiContext context) throws XWikiException
    {
        XWikiDocument result = doc;
        if (result == null) {
            result = new XWikiDocument();
        }
        result.fromXML(getXml(), true);
        return result;
    }
}
