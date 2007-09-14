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
 * Default implementation of SuperDocument.
 */
public class DefaultSuperDocument extends Document implements SuperDocument
{
    protected SuperClass sclass;

    protected boolean isNew = false;

    public DefaultSuperDocument(SuperClass sclass, XWikiContext context) throws XWikiException
    {
        this(sclass, new XWikiDocument(), context);
    }

    public DefaultSuperDocument(SuperClass sclass, String docFullName, XWikiContext context)
        throws XWikiException
    {
        this(sclass, context.getWiki().getDocument(docFullName, context), context);
    }

    public DefaultSuperDocument(SuperClass sclass, XWikiDocument xdoc, XWikiContext context)
        throws XWikiException
    {
        super(xdoc, context);
        
        this.sclass = sclass;

        reload(context);
    }

    public void setFullName(String docFullName)
    {
        getDoc().setFullName(docFullName, context);
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see com.xpn.xwiki.util.SuperDocument#reload(com.xpn.xwiki.api.Document,
     *      com.xpn.xwiki.XWikiContext)
     */
    public void reload(XWikiContext context) throws XWikiException
    {
        if (this.doc.getObject(this.sclass.getClassFullName()) == null) {
            createNewObject(this.sclass.getClassFullName());
            BaseObject object = this.doc.getObject(this.sclass.getClassFullName());

            XWikiDocument docTemplate = this.sclass.getClassTemplateDocument(context);
            BaseObject templateObject = docTemplate.getObject(this.sclass.getClassFullName());
            
            if (templateObject != null)
                object.merge(templateObject);

            if (this.doc.isNew()) {
                setParent(this.sclass.getClassFullName());
                setContent(docTemplate.getContent());
            }
            
            this.isNew = true;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.xpn.xwiki.util.SuperDocument#merge(com.xpn.xwiki.util.SuperDocument)
     */
    public void mergeBaseObject(SuperDocument sdoc)
    {
        if (getSuperClass() != sdoc.getSuperClass())
            return ;
        
        getDoc().getObject(this.sclass.getClassFullName()).merge(
            sdoc.getDocument().getObject(this.sclass.getClassFullName()));
    }
   
    /*
     * (non-Javadoc)
     * 
     * @see com.xpn.xwiki.util.SuperDocument#getSuperClass()
     */
    public SuperClass getSuperClass()
    {
        return this.sclass;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.xpn.xwiki.util.SuperDocument#isNew()
     */
    public boolean isNew()
    {
        return this.isNew;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.xpn.xwiki.util.SuperDocument#save(com.xpn.xwiki.XWikiContext)
     */
    public void save() throws XWikiException
    {
        super.save();
        this.isNew = false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.xpn.xwiki.util.SuperDocument#delete(com.xpn.xwiki.XWikiContext)
     */
    public void delete(XWikiContext context) throws XWikiException
    {
        super.delete();
        this.isNew = true;
    }

    
    /**
     * @see com.xpn.xwiki.doc.XWikiDocument#getStringValue(java.lang.String)
     */
    public String getStringValue(String fieldName)
    {       
        return this.doc.getStringValue(this.sclass.getClassFullName(), fieldName);
    }

    /**
     * @see com.xpn.xwiki.doc.XWikiDocument#setStringValue(java.lang.String,java.lang.String,java.lang.String)
     */
    public void setStringValue(String fieldName, String value)
    {
        getDoc().setStringValue(this.sclass.getClassFullName(), fieldName, value);
    }

    /**
     * @see com.xpn.xwiki.doc.XWikiDocument#getStringValue(java.lang.String)
     */
    public String getLargeStringValue(String fieldName)
    {       
        return this.doc.getStringValue(this.sclass.getClassFullName(), fieldName);
    }
    
    /**
     * @see com.xpn.xwiki.doc.XWikiDocument#setLargeStringValue(java.lang.String,java.lang.String,java.lang.String)
     */
    public void setLargeStringValue(String fieldName, String value)
    {
        getDoc().setLargeStringValue(this.sclass.getClassFullName(), fieldName, value);
    }
    
    /**
     * @see com.xpn.xwiki.doc.XWikiDocument#getListValue(java.lang.String)
     */
    public List getListValue(String fieldName)
    {
        return this.doc.getListValue(this.sclass.getClassFullName(), fieldName);
    }

    /**
     * @see com.xpn.xwiki.doc.XWikiDocument#setStringListValue(java.lang.String,java.lang.String,java.util.List)
     */
    public void setListValue(String fieldName, List value)
    {
        getDoc().setStringListValue(this.sclass.getClassFullName(), fieldName, value);
    }
}
