/*
 * Copyright 2006, XpertNet SARL, and individual contributors as indicated
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
 * @author ludovic
 * @author sdumitriu
 */

package com.xpn.xwiki.cache.impl;

import java.util.Date;
import java.util.Properties;

import com.opensymphony.oscache.base.Cache;
import com.opensymphony.oscache.base.EntryRefreshPolicy;
import com.opensymphony.oscache.base.NeedsRefreshException;
import com.opensymphony.oscache.base.Config;
import com.opensymphony.oscache.plugins.diskpersistence.DiskPersistenceListener;
import com.xpn.xwiki.cache.api.XWikiCache;
import com.xpn.xwiki.cache.api.XWikiCacheNeedsRefreshException;

public class OSCacheCache implements XWikiCache {
    private Cache cache;

    public OSCacheCache() {
       cache = new Cache(true, false);
    }

    public OSCacheCache(int capacity) {
       cache = new Cache(true, false, false, null, capacity);
    }

    public OSCacheCache(int capacity, boolean diskCache, String path) {
       if (diskCache==false)
        cache = new Cache(true, false, false, null, capacity);
       else {
           cache = new Cache(true, true, false, null, capacity);
           DiskPersistenceListener dpl = new DiskPersistenceListener();
           Properties p = new Properties();
           p.put("cache.path", path);
           dpl.configure(new Config(p));
           cache.setPersistenceListener(dpl);
       }
    }

    public void setCapacity(int capacity) {
     cache.setCapacity(capacity);
    }

    public void flushEntry(String key) {
     cache.flushEntry(key);
    }

    public void putInCache(String key, Object obj) {
     cache.putInCache(key, obj);
    }

    public void putInCache(String key, Object obj, EntryRefreshPolicy expiry) {
     cache.putInCache(key, obj, expiry);
    }

    public Object getFromCache(String key) throws XWikiCacheNeedsRefreshException {
        try {
         return cache.getFromCache(key);
        } catch (NeedsRefreshException e) {
            throw new XWikiCacheNeedsRefreshException(e);
        }
    }

    public Object getFromCache(String key, int refeshPeriod) throws XWikiCacheNeedsRefreshException {
        try {
         return cache.getFromCache(key, refeshPeriod);
        } catch (NeedsRefreshException e) {
            throw new XWikiCacheNeedsRefreshException(e);
        }
    }

    public void cancelUpdate(String key) {
        cache.cancelUpdate(key);
    }

    public void flushAll() {
        cache.flushAll(new Date());
    }
}
