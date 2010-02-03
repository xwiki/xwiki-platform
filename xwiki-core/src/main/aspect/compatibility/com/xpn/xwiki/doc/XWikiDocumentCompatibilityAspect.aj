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
package com.xpn.xwiki.doc;

import java.util.ArrayList;
import java.util.List;

import org.xwiki.rendering.syntax.Syntax;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.DocumentSection;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Add a backward compatibility layer to the {@link com.xpn.xwiki.doc.XWikiDocument} class.
 *
 * @version $Id$
 */
public aspect XWikiDocumentCompatibilityAspect
{   
    /**
     * @deprecated use setStringListValue or setDBStringListProperty
     */
    @Deprecated
    public void XWikiDocument.setListValue(String className, String fieldName, List value)
    {
        BaseObject bobject = getObject(className);
        if (bobject == null) {
            bobject = new BaseObject();
            addObject(className, bobject);
        }
        bobject.setName(getFullName());
        bobject.setClassName(className);
        bobject.setListValue(fieldName, value);
        setContentDirty(true);
    }
    
    /**
     * This method to split section according to title.
     * 
     * @return the sections in the current document
     * @throws XWikiException
     * @deprecated use {@link #getSections()} instead, since 1.6M1
     */
    @Deprecated
    public List<DocumentSection> XWikiDocument.getSplitSectionsAccordingToTitle() throws XWikiException
    {
        return getSections();
    }
    
    /**
     * @deprecated use {@link #getUniqueLinkedPages(XWikiContext)}
     */
    @Deprecated
    public List<String> XWikiDocument.getLinkedPages(XWikiContext context)
    {
        return new ArrayList<String>(getUniqueLinkedPages(context));
    }
    
    /**
     * @deprecated use {@link #getUniqueWikiLinkedPages(XWikiContext)} instead
     */
    @Deprecated
    public List<XWikiLink> XWikiDocument.getLinks(XWikiContext context) throws XWikiException
    {
        return getWikiLinkedPages(context);
    }

    /**
     * @deprecated use {@link #getUniqueWikiLinkedPages(XWikiContext)} instead
     */
    @Deprecated
    public List<XWikiLink> XWikiDocument.getWikiLinkedPages(XWikiContext context) throws XWikiException
    {
        return new ArrayList<XWikiLink>(getUniqueWikiLinkedPages(context));
    }
    
    /**
     * @deprecated use {@link #getBackLinkedPages(XWikiContext)} instead
     */
    @Deprecated
    public List<String> XWikiDocument.getBacklinks(XWikiContext context) throws XWikiException
    {
        return getBackLinkedPages(context);
    }
    
    /**
     * @param text the text to render
     * @param context the XWiki Context object
     * @return the given text rendered in the context of this document
     * @deprecated since 1.6M1 use {@link #getRenderedContent(String, String, com.xpn.xwiki.XWikiContext)}
     */
    @Deprecated
    public String XWikiDocument.getRenderedContent(String text, XWikiContext context)
    {
        return getRenderedContent(text, Syntax.XWIKI_1_0.toIdString(), context);
    }
}
