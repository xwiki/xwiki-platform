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
package com.xpn.xwiki.render;

import java.util.Date;

public class XWikiRenderingCache
{
    private String key;

    private String content;

    private int cacheDuration;

    private Date date;

    public XWikiRenderingCache(String key, String content, int cacheDuration, Date date)
    {
        setKey(key);
        setContent(content);
        setCacheDuration(cacheDuration);
        setDate(date);
    }

    public String getKey()
    {
        return this.key;
    }

    public void setKey(String key)
    {
        this.key = key;
    }

    public String getContent()
    {
        return this.content;
    }

    public void setContent(String content)
    {
        this.content = content;
    }

    public int getCacheDuration()
    {
        return this.cacheDuration;
    }

    public void setCacheDuration(int cacheDuration)
    {
        this.cacheDuration = cacheDuration;
    }

    public Date getDate()
    {
        return this.date;
    }

    public void setDate(Date date)
    {
        this.date = date;
    }

    public boolean isValid()
    {
        Date cdate = new Date();
        return ((cdate.getTime() - getDate().getTime()) < this.cacheDuration * 1000);
    }
}
