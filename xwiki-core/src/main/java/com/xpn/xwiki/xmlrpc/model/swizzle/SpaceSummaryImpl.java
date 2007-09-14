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
package com.xpn.xwiki.xmlrpc.model.swizzle;

import java.util.Map;

import com.xpn.xwiki.xmlrpc.model.SpaceSummary;

/**
 * @author hritcu
 *
 */
public class SpaceSummaryImpl implements SpaceSummary
{
    private org.codehaus.swizzle.confluence.SpaceSummary target;
    
    public SpaceSummaryImpl()
    {
        target = new org.codehaus.swizzle.confluence.SpaceSummary();
    }

    public SpaceSummaryImpl(Map map)
    {
        target = new org.codehaus.swizzle.confluence.SpaceSummary(map);
    }
    
    public SpaceSummaryImpl(org.codehaus.swizzle.confluence.SpaceSummary spaceSummary)
    {
        target = spaceSummary;
    }
    
    /**
     * @see com.xpn.xwiki.xmlrpc.model.SpaceSummary#getKey()
     */
    public String getKey()
    {
        return target.getKey();
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.SpaceSummary#getName()
     */
    public String getName()
    {
        return target.getName();
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.SpaceSummary#getType()
     */
    public String getType()
    {
        return target.getType();
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.SpaceSummary#getUrl()
     */
    public String getUrl()
    {
        return target.getUrl();
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.SpaceSummary#setKey(java.lang.String)
     */
    public void setKey(String key)
    {
        target.setKey(key);
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.SpaceSummary#setName(java.lang.String)
     */
    public void setName(String name)
    {
        target.setName(name);
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.SpaceSummary#setType(java.lang.String)
     */
    public void setType(String type)
    {
        target.setType(type);
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.SpaceSummary#setUrl(java.lang.String)
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
