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

package com.xpn.xwiki.xmlrpc;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.xmlrpc.Convert.ConversionException;

import java.util.Date;
import java.util.Map;
import java.util.HashMap;

public class PageHistorySummary
{
    private String id;

    private int version;

    private Date modified;

    private String modifier;

    public PageHistorySummary(String id, int version, Date modified, String modifier)
    {
        setId(id);
        setVersion(version);
        setModified(modified);
        setModifier(modifier);
    }

    public PageHistorySummary(XWikiDocument document)
    {
        setId(document.getFullName() + ":" + document.getVersion());
        setVersion(Page.constructVersion(document.getRCSVersion()));
        setModified(document.getDate());
        setModifier(document.getAuthor());
    }
    
    public PageHistorySummary(Map map) throws ConversionException
    {
    	setId((String)map.get("id"));
        if (map.containsKey("version")) {
            setVersion(Convert.str2int((String)map.get("version")));
        }
        if (map.containsKey("modified")) {
            setModified(Convert.str2date((String)map.get("modified")));
        }
    	setModifier((String)map.get("modifier"));
    }

    public Map toMap()
    {
        Map map = new HashMap();
        map.put("id", getId());
        map.put("version", Convert.int2str(getVersion()));
        map.put("modified", Convert.date2str(getModified()));
        map.put("modifier", getModifier());
        return map;
    }

    public String getId()
    {
        return id;
    }

    public void setId(String id)
    {
        this.id = id;
    }

    public int getVersion()
    {
        return version;
    }

    public void setVersion(int version)
    {
        this.version = version;
    }

    public Date getModified()
    {
        return modified;
    }

    public void setModified(Date modified)
    {
        this.modified = modified;
    }

    public String getModifier()
    {
        return modifier;
    }

    public void setModifier(String modifier)
    {
        this.modifier = modifier;
    }
}
