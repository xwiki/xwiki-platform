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

package com.xpn.xwiki.plugin.applicationmanager.core.doc.objects.classes;

import java.util.List;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Default implementation of SuperDocument. This class manage an XWiki document containing provided
 * XWiki class. It add some specifics methods, getters and setters for this type of object and
 * fields. It also override {@link Document} (and then {@link XWikiDocument}) isNew concept
 * considering as new a document that does not contains an XWiki object of the provided XWiki class.
 * 
 * @version $Id: $
 * @see SuperDocument
 * @see SuperClass
 */
public class DefaultSuperDocument extends Document implements SuperDocument
{
    /**
     * Value in int for {@link Boolean#TRUE}.
     */
    private static final int BOOLEANFIELD_TRUE = 1;

    /**
     * Value in int for {@link Boolean#FALSE}.
     */
    private static final int BOOLEANFIELD_FALSE = 0;
    
    /**
     * Value in int for {@link Boolean} = null.
     */
    private static final int BOOLEANFIELD_MAYBE = 2;

    /**
     * The class manager for this document.
     */
    protected SuperClass sclass;

    /**
     * true if this is a new document of this class (this document can exist but does not contains
     * object of this class).
     */
    protected boolean isNew;

    /**
     * Create new instance of DefaultSuperDocument.
     * 
     * @param sclass the class manager for this document.
     * @param context the XWiki context.
     * @throws XWikiException error when calling {@link #reload(XWikiContext)}.
     */
    public DefaultSuperDocument(SuperClass sclass, XWikiContext context) throws XWikiException
    {
        this(sclass, new XWikiDocument(), context);
    }

    /**
     * Create instance of DefaultSuperDocument from XWikiDocument with <code>docFullName</code>
     * full name.
     * 
     * @param sclass the class manager for this document.
     * @param docFullName the full name of the XWikiDocument to manage.
     * @param context the Xwiki context.
     * @throws XWikiException error when calling {@link #reload(XWikiContext)}.
     */
    public DefaultSuperDocument(SuperClass sclass, String docFullName, XWikiContext context)
        throws XWikiException
    {
        this(sclass, context.getWiki().getDocument(docFullName, context), context);
    }

    /**
     * Create instance of DefaultSuperDocument from provided XWikiDocument.
     * 
     * @param sclass the class manager for this document.
     * @param xdoc the XWikiDocument to manage.
     * @param context the XWiki context.
     * @throws XWikiException error when calling {@link #reload(XWikiContext)}.
     */
    public DefaultSuperDocument(SuperClass sclass, XWikiDocument xdoc, XWikiContext context)
        throws XWikiException
    {
        super(xdoc, context);

        this.sclass = sclass;

        reload(context);
    }

    /**
     * @param docFullName modify the full name the the managed XWikiDocument.
     */
    public void setFullName(String docFullName)
    {
        getDoc().setFullName(docFullName, context);
    }

    /**
     * {@inheritDoc}
     * 
     * @see SuperDocument#reload(com.xpn.xwiki.XWikiContext)
     */
    public void reload(XWikiContext context) throws XWikiException
    {
        if (this.getObjectNumbers(this.sclass.getClassFullName()) == 0) {
            BaseObject object = getDoc().newObject(this.sclass.getClassFullName(), context);

            XWikiDocument docTemplate = this.sclass.getClassTemplateDocument(context);
            BaseObject templateObject = docTemplate.getObject(this.sclass.getClassFullName());

            if (templateObject != null) {
                object.merge(templateObject);
            }

            if (super.isNew()) {
                setParent(docTemplate.getParent());
                setContent(docTemplate.getContent());
            }

            this.isNew = true;
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see SuperDocument#mergeBaseObject(SuperDocument)
     */
    public void mergeBaseObject(SuperDocument sdoc)
    {
        if (getSuperClass() != sdoc.getSuperClass()) {
            return;
        }

        getDoc().getObject(this.sclass.getClassFullName()).merge(
            sdoc.getDocument().getObject(this.sclass.getClassFullName()));
    }

    /**
     * {@inheritDoc}
     * 
     * @see SuperDocument#getSuperClass()
     */
    public SuperClass getSuperClass()
    {
        return this.sclass;
    }

    /**
     * {@inheritDoc}
     * 
     * @see SuperDocument#isNew()
     */
    public boolean isNew()
    {
        return super.isNew() || this.isNew;
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.api.Document#saveDocument(String)
     */
    protected void saveDocument(String comment) throws XWikiException
    {
        super.saveDocument(comment);
        this.isNew = false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.api.Document#deleteDocument()
     */
    protected void deleteDocument() throws XWikiException
    {
        super.deleteDocument();
        this.isNew = true;
    }

    /**
     * Get the value of the field <code>fieldName</code> of the class "XWikiApplicationClass".
     * 
     * @param fieldName the name of the field from the class "XWikiApplicationClass" where to find
     *            the value.
     * @return the value in {@link String} of the field <code>fieldName</code> of the class
     *         "XWikiApplicationClass".
     * @see com.xpn.xwiki.doc.XWikiDocument#getStringValue(java.lang.String)
     */
    public String getStringValue(String fieldName)
    {
        return this.doc.getStringValue(this.sclass.getClassFullName(), fieldName);
    }

    /**
     * Modify the value of the field <code>fieldName</code> of the class "XWikiApplicationClass".
     * 
     * @param fieldName the name of the field from the class "XWikiApplicationClass" where to find
     *            the value.
     * @param value the new value of the field <code>fieldName</code> of the class
     *            "XWikiApplicationClass".
     * @see com.xpn.xwiki.doc.XWikiDocument#setStringValue(java.lang.String,java.lang.String,java.lang.String)
     */
    public void setStringValue(String fieldName, String value)
    {
        getDoc().setStringValue(this.sclass.getClassFullName(), fieldName, value);
    }

    /**
     * Get the value of the field <code>fieldName</code> of the class "XWikiApplicationClass".
     * 
     * @param fieldName the name of the field from the class "XWikiApplicationClass" where to find
     *            the value.
     * @return the value in {@link String} of the field <code>fieldName</code> of the class
     *         "XWikiApplicationClass".
     * @see com.xpn.xwiki.doc.XWikiDocument#getStringValue(java.lang.String)
     */
    public String getLargeStringValue(String fieldName)
    {
        return this.doc.getStringValue(this.sclass.getClassFullName(), fieldName);
    }

    /**
     * Modify the value of the field <code>fieldName</code> of the class "XWikiApplicationClass".
     * 
     * @param fieldName the name of the field from the class "XWikiApplicationClass" where to find
     *            the value.
     * @param value the new value of the field <code>fieldName</code> of the class
     *            "XWikiApplicationClass".
     * @see com.xpn.xwiki.doc.XWikiDocument#setLargeStringValue(java.lang.String,java.lang.String,java.lang.String)
     */
    public void setLargeStringValue(String fieldName, String value)
    {
        getDoc().setLargeStringValue(this.sclass.getClassFullName(), fieldName, value);
    }

    /**
     * Get the value of the field <code>fieldName</code> of the class "XWikiApplicationClass".
     * 
     * @param fieldName the name of the field from the class "XWikiApplicationClass" where to find
     *            the value.
     * @return the value in {@link List} of the field <code>fieldName</code> of the class
     *         "XWikiApplicationClass".
     * @see com.xpn.xwiki.doc.XWikiDocument#getListValue(java.lang.String)
     */
    public List getListValue(String fieldName)
    {
        return this.doc.getListValue(this.sclass.getClassFullName(), fieldName);
    }

    /**
     * Modify the value of the field <code>fieldName</code> of the class "XWikiApplicationClass".
     * 
     * @param fieldName the name of the field from the class "XWikiApplicationClass" where to find
     *            the value.
     * @param value the new value of the field <code>fieldName</code> of the class
     *            "XWikiApplicationClass".
     * @see com.xpn.xwiki.doc.XWikiDocument#setStringListValue(java.lang.String,java.lang.String,java.util.List)
     */
    public void setListValue(String fieldName, List value)
    {
        getDoc().setStringListValue(this.sclass.getClassFullName(), fieldName, value);
    }

    /**
     * Get the value of the field <code>fieldName</code> of the class "XWikiApplicationClass".
     * 
     * @param fieldName the name of the field from the class "XWikiApplicationClass" where to find
     *            the value.
     * @return the value in int of the field <code>fieldName</code> of the class
     *         "XWikiApplicationClass".
     * @see com.xpn.xwiki.doc.XWikiDocument#getListValue(java.lang.String)
     */
    public int getIntValue(String fieldName)
    {
        return this.doc.getIntValue(this.sclass.getClassFullName(), fieldName);
    }

    /**
     * Modify the value of the field <code>fieldName</code> of the class "XWikiApplicationClass".
     * 
     * @param fieldName the name of the field from the class "XWikiApplicationClass" where to find
     *            the value.
     * @param value the new value of the field <code>fieldName</code> of the class
     *            "XWikiApplicationClass".
     * @see com.xpn.xwiki.doc.XWikiDocument#setIntValue(String, String, int)
     */
    public void setIntValue(String fieldName, int value)
    {
        getDoc().setIntValue(this.sclass.getClassFullName(), fieldName, value);
    }

    /**
     * Get the value of the field <code>fieldName</code> of the class "XWikiApplicationClass".
     * 
     * @param fieldName the name of the field from the class "XWikiApplicationClass" where to find
     *            the value.
     * @return the value in {@link Boolean} of the field <code>fieldName</code> of the class
     *         "XWikiApplicationClass".
     * @see com.xpn.xwiki.doc.XWikiDocument#getListValue(java.lang.String)
     */
    public Boolean getBooleanValue(String fieldName)
    {
        int intValue = getIntValue(fieldName);

        return intValue == BOOLEANFIELD_TRUE ? Boolean.TRUE : (intValue == BOOLEANFIELD_FALSE
            ? Boolean.FALSE : null);
    }

    /**
     * Modify the value of the field <code>fieldName</code> of the class "XWikiApplicationClass".
     * 
     * @param fieldName the name of the field from the class "XWikiApplicationClass" where to find
     *            the value.
     * @param value the new value of the field <code>fieldName</code> of the class
     *            "XWikiApplicationClass".
     * @see com.xpn.xwiki.doc.XWikiDocument#setIntValue(String, String, int)
     */
    public void setBooleanValue(String fieldName, Boolean value)
    {
        setIntValue(fieldName, value == null ? BOOLEANFIELD_MAYBE : (value.booleanValue()
            ? BOOLEANFIELD_TRUE : BOOLEANFIELD_FALSE));
    }
}
