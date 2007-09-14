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

/**
 * 
 */
package com.xpn.xwiki.xmlrpc.model.swizzle;

import java.util.Map;

import com.xpn.xwiki.xmlrpc.model.SearchResult;

/**
 * @author hritcu
 *
 */
public class SearchResultImpl implements SearchResult
{

    private org.codehaus.swizzle.confluence.SearchResult target;
    
    public SearchResultImpl()
    {
        target = new org.codehaus.swizzle.confluence.SearchResult();
    }

    public SearchResultImpl(Map map)
    {
        target = new org.codehaus.swizzle.confluence.SearchResult(map);
    }
    
    public SearchResultImpl(org.codehaus.swizzle.confluence.SearchResult searchResult)
    {
        target = searchResult;
    }
    
    /**
     * @see com.xpn.xwiki.xmlrpc.model.SearchResult#getExcerpt()
     */
    public String getExcerpt()
    {
        return target.getExcerpt();
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.SearchResult#getId()
     */
    public String getId()
    {
        return target.getId();
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.SearchResult#getTitle()
     */
    public String getTitle()
    {
        return target.getTitle();
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.SearchResult#getType()
     */
    public String getType()
    {
        return target.getType();
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.SearchResult#getUrl()
     */
    public String getUrl()
    {
        return target.getUrl();
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.SearchResult#setExcerpt(java.lang.String)
     */
    public void setExcerpt(String excerpt)
    {
        target.setExcerpt(excerpt);
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.SearchResult#setId(java.lang.String)
     */
    public void setId(String id)
    {
        target.setId(id);
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.SearchResult#setTitle(java.lang.String)
     */
    public void setTitle(String title)
    {
        target.setTitle(title);
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.SearchResult#setType(java.lang.String)
     */
    public void setType(String type)
    {
        target.setType(type);
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.SearchResult#setUrl(java.lang.String)
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
}
