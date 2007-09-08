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

import java.util.Date;
import java.util.Map;

import org.codehaus.swizzle.confluence.MapConvertor;
import org.codehaus.swizzle.confluence.SwizzleConversionException;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.xmlrpc.model.Comment;

/**
 * @author hritcu
 *
 */
public class CommentImpl implements Comment
{
    
    private org.codehaus.swizzle.confluence.Comment target;
    
    public CommentImpl()
    {
        target = new org.codehaus.swizzle.confluence.Comment();
    }

    public CommentImpl(Map map)
    {
        target = new org.codehaus.swizzle.confluence.Comment(map);
    }
    
    public CommentImpl(Map map, MapConvertor convertor) throws XWikiException
    {
        Map typeMap = org.codehaus.swizzle.confluence.Comment.FIELD_TYPES;
        try {
            target = new org.codehaus.swizzle.confluence.Comment(convertor.revert(map, typeMap));
        } catch (SwizzleConversionException e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_XMLRPC, 0, e.getMessage(), e);
        }
    }
    
    public CommentImpl(org.codehaus.swizzle.confluence.Comment comment)
    {
        target = comment;
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.Comment#getContent()
     */
    public String getContent()
    {
        return target.getContent();
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.Comment#getCreated()
     */
    public Date getCreated()
    {
        return target.getCreated();
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.Comment#getCreator()
     */
    public String getCreator()
    {
        return target.getCreator();
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.Comment#getId()
     */
    public String getId()
    {
        return target.getId();
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.Comment#getPageId()
     */
    public String getPageId()
    {
        return target.getPageId();
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.Comment#getTitle()
     */
    public String getTitle()
    {
        return target.getTitle();
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.Comment#getUrl()
     */
    public String getUrl()
    {
        return target.getUrl();
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.Comment#setContent(java.lang.String)
     */
    public void setContent(String content)
    {
        target.setContent(content);
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.Comment#setCreated(java.util.Date)
     */
    public void setCreated(Date created)
    {
        target.setCreated(created);
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.Comment#setCreator(java.lang.String)
     */
    public void setCreator(String creator)
    {
        target.setCreator(creator);
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.Comment#setId(java.lang.String)
     */
    public void setId(String id)
    {
        target.setId(id);
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.Comment#setPageId(java.lang.String)
     */
    public void setPageId(String pageId)
    {
        target.setPageId(pageId);
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.Comment#setTitle(java.lang.String)
     */
    public void setTitle(String title)
    {
        target.setTitle(title);
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.Comment#setUrl(java.lang.String)
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

    public org.codehaus.swizzle.confluence.Comment getTarget()
    {
        return target;
    }
}
