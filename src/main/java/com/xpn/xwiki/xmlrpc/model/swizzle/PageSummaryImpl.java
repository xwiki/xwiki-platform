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

/**
 * 
 */
package com.xpn.xwiki.xmlrpc.model.swizzle;

import java.util.Map;

import com.xpn.xwiki.xmlrpc.model.PageSummary;

/**
 * @author hritcu
 *
 */
public class PageSummaryImpl implements PageSummary
{
    
    private org.codehaus.swizzle.confluence.PageSummary target;
    
    public PageSummaryImpl()
    {
        target = new org.codehaus.swizzle.confluence.PageSummary();
    }

    public PageSummaryImpl(Map map)
    {
        target = new org.codehaus.swizzle.confluence.PageSummary(map);
    }

    public PageSummaryImpl(org.codehaus.swizzle.confluence.PageSummary pageSummary)
    {
        target = pageSummary;
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
     * @see com.xpn.xwiki.xmlrpc.model.MapObject#toMap(java.lang.String)
     */
    public Map toMap()
    {
        return target.toMap();
    }

    public org.codehaus.swizzle.confluence.PageSummary getTarget()
    {
        return target;
    }
}
