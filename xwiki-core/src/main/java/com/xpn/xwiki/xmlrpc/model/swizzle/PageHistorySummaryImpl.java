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

import java.util.Date;
import java.util.Map;

import com.xpn.xwiki.xmlrpc.model.PageHistorySummary;

/**
 * @author hritcu
 *
 */
public class PageHistorySummaryImpl implements PageHistorySummary
{
    
    private org.codehaus.swizzle.confluence.PageHistorySummary target;
    
    public PageHistorySummaryImpl()
    {
        target = new org.codehaus.swizzle.confluence.PageHistorySummary();
    }

    public PageHistorySummaryImpl(Map map)
    {
        target = new org.codehaus.swizzle.confluence.PageHistorySummary(map);
    }

    public PageHistorySummaryImpl(org.codehaus.swizzle.confluence.PageHistorySummary phs)
    {
        target = phs;
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.PageHistorySummary#getId()
     */
    public String getId()
    {
        return target.getId();
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.PageHistorySummary#getModified()
     */
    public Date getModified()
    {
        return target.getModified();
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.PageHistorySummary#getModifier()
     */
    public String getModifier()
    {
        return target.getModifier();
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.PageHistorySummary#getVersion()
     */
    public int getVersion()
    {
        return target.getVersion();
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.PageHistorySummary#setId(java.lang.String)
     */
    public void setId(String id)
    {
        target.setId(id);
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.PageHistorySummary#setModified(java.util.Date)
     */
    public void setModified(Date modified)
    {
        target.setModified(modified);
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.PageHistorySummary#setModifier(java.lang.String)
     */
    public void setModifier(String modifier)
    {
        target.setModifier(modifier);
    }

    /**
     * @see com.xpn.xwiki.xmlrpc.model.PageHistorySummary#setVersion(int)
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
}
