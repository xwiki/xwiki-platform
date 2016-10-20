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

import java.util.Map;

import org.xwiki.job.event.status.JobStatus.State;

import com.xpn.xwiki.cache.api.XWikiCache;
import com.xpn.xwiki.cache.api.XWikiCacheNeedsRefreshException;
import com.xpn.xwiki.internal.WikiInitializerJob;

@Deprecated
public class XWikiInitializedWikiCacheStub implements XWikiCache
{
    private Map<String, WikiInitializerJob> initializedWikis;

    public XWikiInitializedWikiCacheStub(Map<String, WikiInitializerJob> initializedWikis)
    {
        this.initializedWikis = initializedWikis;
    }

    @Override
    public void cancelUpdate(String key)
    {

    }

    @Override
    public void flushAll()
    {
        // This map should never be acceded directly
    }

    @Override
    public void flushEntry(String key)
    {
        // This map should never be acceded directly
    }

    @Override
    public Object getFromCache(String key) throws XWikiCacheNeedsRefreshException
    {
        WikiInitializerJob job = initializedWikis.get(key);

        if (job == null || job.getStatus().getState() != State.FINISHED) {
            return Boolean.FALSE;
        }

        return Boolean.TRUE;
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
     // This map should never be acceded directly
    }

    @Override
    public void setCapacity(int capacity)
    {

    }
}
