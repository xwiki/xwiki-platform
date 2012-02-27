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
 */
package com.xpn.xwiki.cache.api.internal;

import org.xwiki.cache.Cache;

import com.xpn.xwiki.cache.api.XWikiCache;
import com.xpn.xwiki.cache.api.XWikiCacheNeedsRefreshException;

@Deprecated
public class XWikiCacheStub implements XWikiCache
{
    Cache cache;

    public XWikiCacheStub(Cache cache)
    {
        this.cache = cache;
    }

    @Override
    public void cancelUpdate(String key)
    {

    }

    @Override
    public void flushAll()
    {
        this.cache.removeAll();
    }

    @Override
    public void flushEntry(String key)
    {
        this.cache.remove(key);
    }

    @Override
    public Object getFromCache(String key) throws XWikiCacheNeedsRefreshException
    {
        Object value = this.cache.get(key);

        if (value == null) {
            throw new XWikiCacheNeedsRefreshException();
        }

        return value;
    }

    @Override
    public Object getFromCache(String key, int refeshPeriod) throws XWikiCacheNeedsRefreshException
    {
        return getFromCache(key);
    }

    @Override
    public int getNumberEntries()
    {
        return 0;
    }

    @Override
    public void putInCache(String key, Object obj)
    {
        this.cache.set(key, obj);
    }

    @Override
    public void setCapacity(int capacity)
    {

    }
}
