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

import java.util.Map;

import org.codehaus.swizzle.confluence.MapConvertor;
import org.codehaus.swizzle.confluence.SwizzleConversionException;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.xmlrpc.model.Space;

/**
 * @author hritcu
 *
 */
public class SpaceImpl implements Space
{
    
    private org.codehaus.swizzle.confluence.Space target;
    
    public SpaceImpl()
    {
        target = new org.codehaus.swizzle.confluence.Space();
    }

    public SpaceImpl(Map map)
    {
        target = new org.codehaus.swizzle.confluence.Space(map);
    }

    public SpaceImpl(Map map, MapConvertor convertor) throws XWikiException
    {
        Map typeMap = org.codehaus.swizzle.confluence.Space.FIELD_TYPES;
        try {
            target = new org.codehaus.swizzle.confluence.Space(convertor.revert(map, typeMap));
        } catch (SwizzleConversionException e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_XMLRPC, 0, e.getMessage(), e);
        }
    }
    
    public SpaceImpl(org.codehaus.swizzle.confluence.Space space)
    {
        target = space;
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.Space#getDescription()
     */
    public String getDescription()
    {
        return target.getDescription();
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.Space#getHomepage()
     */
    public String getHomePage()
    {
        return target.getHomepage();
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.Space#getKey()
     */
    public String getKey()
    {
        return target.getKey();
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.Space#getName()
     */
    public String getName()
    {
        return target.getName();
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.Space#getType()
     */
    public String getType()
    {
        return target.getType();
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.Space#getUrl()
     */
    public String getUrl()
    {
        return target.getUrl();
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.Space#setDescription(java.lang.String)
     */
    public void setDescription(String description)
    {
        target.setDescription(description);
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.Space#setHomepage(java.lang.String)
     */
    public void setHomePage(String homepage)
    {
        target.setHomepage(homepage);
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.Space#setKey(java.lang.String)
     */
    public void setKey(String key)
    {
        target.setKey(key);
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.Space#setName(java.lang.String)
     */
    public void setName(String name)
    {
        target.setName(name);
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.Space#setType(java.lang.String)
     */
    public void setType(String type)
    {
        target.setType(type);
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.Space#setUrl(java.lang.String)
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

    public org.codehaus.swizzle.confluence.Space getTarget()
    {
        return target;
    }
}
