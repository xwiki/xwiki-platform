package com.xpn.xwiki.cache.impl;

import java.util.Date;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.opensymphony.oscache.base.Cache;
import com.opensymphony.oscache.base.EntryRefreshPolicy;
import com.opensymphony.oscache.base.NeedsRefreshException;
import com.opensymphony.oscache.general.GeneralCacheAdministrator;
import com.xpn.xwiki.cache.api.XWikiCache;
import com.xpn.xwiki.cache.api.XWikiCacheNeedsRefreshException;

/**
 * Copyright 2006, XpertNet SARL, and individual contributors as indicated by
 * the contributors.txt.
 * 
 * This is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 * 
 * This software is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this software; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA, or see the FSF
 * site: http://www.fsf.org.
 * 
 * Implements XWikiCache using oscache
 * 
 * @author ludovic
 * @author sdumitriu
 * @author markj
 * @author wr0ngway
 * 
 */
public class OSCacheCache implements XWikiCache
{
    private static final Log          log = LogFactory.getLog(OSCacheCache.class);

    private GeneralCacheAdministrator cache;
    private String                    name;
    private int                       capacity;

    public OSCacheCache(Properties props)
    {
        cache = new GeneralCacheAdministrator(props);
    }

    public OSCacheCache(Properties props, int capacity)
    {
        this(props);
        cache.setCacheCapacity(capacity);
        this.capacity = capacity;
    }

    /**
     * Provide package-private access to the underlying m_cache
     */
    Cache getCache()
    {
        return cache.getCache();
    }

    public void setCapacity(int capacity)
    {
        cache.setCacheCapacity(capacity);
        this.capacity = capacity;
    }

    public void flushEntry(String key)
    {
        cache.flushEntry(key);
    }

    public void putInCache(String key, Object obj)
    {
        cache.putInCache(key, obj);
        logCacheAdd(key, obj);
    }

    public void putInCache(String key, Object obj, EntryRefreshPolicy expiry)
    {
        cache.putInCache(key, obj, expiry);
        logCacheAdd(key, obj);
    }

    public Object getFromCache(String key) throws XWikiCacheNeedsRefreshException
    {
        try
        {
            return cache.getFromCache(key);
        }
        catch (NeedsRefreshException e)
        {
            throw new XWikiCacheNeedsRefreshException(e);
        }
    }

    public Object getFromCache(String key, int refeshPeriod) throws XWikiCacheNeedsRefreshException
    {
        try
        {
            return cache.getFromCache(key, refeshPeriod);
        }
        catch (NeedsRefreshException e)
        {
            throw new XWikiCacheNeedsRefreshException(e);
        }
    }

    public void cancelUpdate(String key)
    {
        cache.cancelUpdate(key);
    }

    public void flushAll()
    {
        cache.flushAll(new Date());
    }

    /**
     * Method called here is labeled "for test only" in OSCache JavaDoc, so
     * don't call this a lot!
     */
    public int getNumberEntries()
    {
        return cache.getCache().getNbEntries();
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getName()
    {
        return name;
    }

    public int getCapacity()
    {
        return capacity;
    }

    private void logCacheAdd(String sKey, Object obj)
    {
        if (log.isDebugEnabled())
        {
            StringBuffer sbEntry = new StringBuffer();
            sbEntry.append(getName());
            sbEntry.append(" adding object key=");
            sbEntry.append(sKey);
            sbEntry.append("; class=");
            if (obj == null)
            {
                sbEntry.append("null");
            }
            else
            {
                sbEntry.append(obj.getClass().toString());
                sbEntry.append("; toString=");
                sbEntry.append(obj.toString());
                log.debug(sbEntry.toString());
            }
        }
    }
}
