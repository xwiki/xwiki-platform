/*
 * Copyright 2006-2007, XpertNet SARL, and individual contributors as indicated
 * by the contributors.txt.
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
package com.xpn.xwiki.xmlrpc.model.swizzle;

import java.util.Date;
import java.util.Map;

import org.codehaus.swizzle.confluence.MapConvertor;
import org.codehaus.swizzle.confluence.SwizzleConversionException;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.xmlrpc.model.Page;

/**
 * @author hritcu
 *
 */
public class PageImpl implements Page
{

    private org.codehaus.swizzle.confluence.Page target;
    
    public PageImpl()
    {
        target = new org.codehaus.swizzle.confluence.Page();
    }

    public PageImpl(Map map)
    {
        target = new org.codehaus.swizzle.confluence.Page(map);
    }
    
    public PageImpl(Map map, MapConvertor convertor) throws XWikiException
    {
        Map typeMap = org.codehaus.swizzle.confluence.Page.FIELD_TYPES;
        try {
            target = new org.codehaus.swizzle.confluence.Page(convertor.revert(map, typeMap));
        } catch (SwizzleConversionException e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_XMLRPC, 0, e.getMessage(), e);
        }
    }
    
    public PageImpl(org.codehaus.swizzle.confluence.Page page)
    {
        target = page;
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.PageSummary#getId()
     */
    public String getId()
    {
        return target.getId();
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.PageSummary#getLocks()
     */
    public int getLocks()
    {
        return target.getLocks();
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.PageSummary#getParentId()
     */
    public String getParentId()
    {
        return target.getParentId();
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.PageSummary#getSpace()
     */
    public String getSpace()
    {
        return target.getSpace();
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.PageSummary#getTitle()
     */
    public String getTitle()
    {
        return target.getTitle();
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.PageSummary#getUrl()
     */
    public String getUrl()
    {
        return target.getUrl();
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.PageSummary#setId(java.lang.String)
     */
    public void setId(String id)
    {
        target.setId(id);
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.PageSummary#setLocks(int)
     */
    public void setLocks(int locks)
    {
        target.setLocks(locks);
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.PageSummary#setParentId(java.lang.String)
     */
    public void setParentId(String parentId)
    {
        target.setParentId(parentId);
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.PageSummary#setSpace(java.lang.String)
     */
    public void setSpace(String space)
    {
        target.setSpace(space);
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.PageSummary#setTitle(java.lang.String)
     */
    public void setTitle(String title)
    {
        target.setTitle(title);
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.PageSummary#setUrl(java.lang.String)
     */
    public void setUrl(String url)
    {
        target.setUrl(url);
    }
    
    /**
     * @see com.xpn.xwiki.xmlrpc.model.Page#getContent()
     */
    public String getContent()
    {
        return target.getContent();
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.Page#getContentStatus()
     */
    public String getContentStatus()
    {
        return target.getContentStatus();
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.Page#getCreated()
     */
    public Date getCreated()
    {
        return target.getCreated();
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.Page#getCreator()
     */
    public String getCreator()
    {
        return target.getCreator();
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.Page#getModified()
     */
    public Date getModified()
    {
        return target.getModified();
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.Page#getModifier()
     */
    public String getModifier()
    {
        return target.getModifier();
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.Page#getVersion()
     */
    public int getVersion()
    {
        return target.getVersion();
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.Page#isCurrent()
     */
    public boolean isCurrent()
    {
        return target.isCurrent();
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.Page#isHomePage()
     */
    public boolean isHomePage()
    {
        return target.isHomePage();
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.Page#setContent(java.lang.String)
     */
    public void setContent(String content)
    {
        target.setContent(content);
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.Page#setContentStatus(java.lang.String)
     */
    public void setContentStatus(String contentStatus)
    {
        target.setContentStatus(contentStatus);
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.Page#setCreated(java.util.Date)
     */
    public void setCreated(Date created)
    {
        target.setCreated(created);
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.Page#setCreator(java.lang.String)
     */
    public void setCreator(String creator)
    {
        target.setCreator(creator);
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.Page#setCurrent(boolean)
     */
    public void setCurrent(boolean current)
    {
        target.setCurrent(current);
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.Page#setHomePage(boolean)
     */
    public void setHomePage(boolean homePage)
    {
        target.setHomePage(homePage);
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.Page#setModified(java.util.Date)
     */
    public void setModified(Date modified)
    {
        target.setModified(modified);
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.Page#setModifier(java.lang.String)
     */
    public void setModifier(String modifier)
    {
        target.setModifier(modifier);
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.Page#setVersion(int)
     */
    public void setVersion(int version)
    {
        target.setVersion(version);
    }
    
    /**
     * @see com.xpn.xwiki.xmlrpc.model.MapObject#toMap(java.lang.String)
     */
    public Map toMap()
    {
        return target.toMap();
    }

    public org.codehaus.swizzle.confluence.Page getTarget()
    {
        return target;
    }
}
