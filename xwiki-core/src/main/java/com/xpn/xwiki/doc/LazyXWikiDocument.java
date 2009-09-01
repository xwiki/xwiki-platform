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
import java.util.Vector;

import org.xwiki.context.Execution;
import org.xwiki.rendering.block.XDOM;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.web.Utils;

/**
 * Read oly lazy loading document.
 * 
 * @version $Id$
 * @since 2.0M4
 */
public class LazyXWikiDocument extends XWikiDocument
{
    /**
     * The real document.
     */
    private XWikiDocument document;

    /**
     * Load and return the document in the database.
     * 
     * @return the real document
     */
    private XWikiDocument getDocument()
    {
        if (this.document == null) {
            // get context
            XWikiContext context =
                (XWikiContext) Utils.getComponent(Execution.class).getContext().getProperty("xwikicontext");

            XWikiDocument doc = new XWikiDocument();
            doc.setDatabase(getDatabase());
            doc.setSpace(getSpace());
            doc.setName(getName());
            doc.setLanguage(getLanguage());

            try {
                if (this.version == null) {
                    this.document = context.getWiki().getDocument(doc, context);
                } else {
                    this.document = context.getWiki().getDocument(doc, getVersion(), context);
                }
            } catch (XWikiException e) {
                throw new RuntimeException("Failed to get document [" + this + "]");
            }
        }

        return this.document;
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.doc.XWikiDocument#getLanguage()
     */
    @Override
    public String getLanguage()
    {
        if (this.language == null) {
            return getDocument().getLanguage();
        } else {
            return this.language;
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.doc.XWikiDocument#getVersion()
     */
    @Override
    public String getVersion()
    {
        if (this.version == null) {
            return getDocument().getVersion();
        } else {
            return this.version.toString();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.doc.XWikiDocument#getContent()
     */
    @Override
    public String getContent()
    {
        return getDocument().getContent();
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.doc.XWikiDocument#getObjects(java.lang.String)
     */
    @Override
    public Vector<BaseObject> getObjects(String classname)
    {
        return getDocument().getObjects(classname);
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.doc.XWikiDocument#getAuthor()
     */
    @Override
    public String getAuthor()
    {
        return getDocument().getAuthor();
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.doc.XWikiDocument#getContentAuthor()
     */
    @Override
    public String getContentAuthor()
    {
        return getDocument().getContentAuthor();
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.doc.XWikiDocument#getDate()
     */
    @Override
    public Date getDate()
    {
        return getDocument().getDate();
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.doc.XWikiDocument#getCreationDate()
     */
    @Override
    public Date getCreationDate()
    {
        return getDocument().getCreationDate();
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.doc.XWikiDocument#getContentUpdateDate()
     */
    @Override
    public Date getContentUpdateDate()
    {
        return getDocument().getContentUpdateDate();
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.doc.XWikiDocument#getTitle()
     */
    @Override
    public String getTitle()
    {
        return getDocument().getTitle();
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.doc.XWikiDocument#getDefaultLanguage()
     */
    @Override
    public String getDefaultLanguage()
    {
        return getDocument().getDefaultLanguage();
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.doc.XWikiDocument#getTranslation()
     */
    @Override
    public int getTranslation()
    {
        return getDocument().getTranslation();
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.doc.XWikiDocument#getCustomClass()
     */
    @Override
    public String getCustomClass()
    {
        return getDocument().getCustomClass();
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.doc.XWikiDocument#getParent()
     */
    @Override
    public String getParent()
    {
        return getDocument().getParent();
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.doc.XWikiDocument#getElements()
     */
    @Override
    public int getElements()
    {
        return getDocument().getElements();
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.doc.XWikiDocument#getDefaultTemplate()
     */
    @Override
    public String getDefaultTemplate()
    {
        return getDocument().getDefaultTemplate();
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.doc.XWikiDocument#getValidationScript()
     */
    @Override
    public String getValidationScript()
    {
        return getDocument().getValidationScript();
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.doc.XWikiDocument#getComment()
     */
    @Override
    public String getComment()
    {
        return getDocument().getComment();
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.doc.XWikiDocument#getMinorEdit1()
     */
    @Override
    protected Boolean getMinorEdit1()
    {
        return getDocument().getMinorEdit1();
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.doc.XWikiDocument#getSyntaxId()
     */
    public String getSyntaxId()
    {
        return getDocument().getSyntaxId();
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.doc.XWikiDocument#isHidden()
     */
    @Override
    public Boolean isHidden()
    {
        return getDocument().isHidden();
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.doc.XWikiDocument#getDocumentArchive()
     */
    @Override
    public XWikiDocumentArchive getDocumentArchive()
    {
        return getDocument().getDocumentArchive();
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.doc.XWikiDocument#getDocumentArchive(com.xpn.xwiki.XWikiContext)
     */
    @Override
    public XWikiDocumentArchive getDocumentArchive(XWikiContext context) throws XWikiException
    {
        return getDocument().getDocumentArchive(context);
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.doc.XWikiDocument#getStore()
     */
    @Override
    public XWikiStoreInterface getStore()
    {
        return getDocument().getStore();
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.doc.XWikiDocument#getStore(com.xpn.xwiki.XWikiContext)
     */
    @Override
    public XWikiStoreInterface getStore(XWikiContext context)
    {
        return getDocument().getStore(context);
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.doc.XWikiDocument#getXDOM()
     */
    @Override
    public XDOM getXDOM()
    {
        return getDocument().getXDOM();
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.doc.XWikiDocument#getTags(com.xpn.xwiki.XWikiContext)
     */
    @Override
    public String getTags(XWikiContext context)
    {
        return getDocument().getTags(context);
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.doc.XWikiDocument#isFromCache()
     */
    @Override
    public boolean isFromCache()
    {
        return getDocument().isFromCache();
    }
}
