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
package org.xwiki.rendering.internal.macro.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheEntry;
import org.xwiki.cache.event.CacheEntryEvent;
import org.xwiki.cache.event.AbstractCacheEntryListener;
import org.xwiki.job.JobException;
import org.xwiki.job.JobExecutor;

public class CacheMacroCacheEntryListener extends AbstractCacheEntryListener<CacheValue>
{
    private Logger logger = LoggerFactory.getLogger(CacheMacroCacheEntryListener.class);

    private JobExecutor jobExecutor;

    public CacheMacroCacheEntryListener(JobExecutor jobExecutor)
    {
        this.jobExecutor = jobExecutor;
    }

    @Override
    public void cacheEntryAdded(CacheEntryEvent<CacheValue> event)
    {
        if (logger.isDebugEnabled()) {
            this.logger.debug("Macro Cache entry [{}] added", event.getEntry());
        }
    }

    @Override
    public void cacheEntryModified(CacheEntryEvent<CacheValue> event)
    {
        if (logger.isDebugEnabled()) {
            this.logger.debug("Macro Cache entry [{}] modified", event.getEntry());
        }
    }

    @Override
    public void cacheEntryRemoved(CacheEntryEvent<CacheValue> event)
    {
        if (logger.isDebugEnabled()) {
            this.logger.debug("Macro Cache entry [{}] removed", event.getEntry());
        }

        // The cache has removed an entry but since caching is used to speed up rendering, it usually means that what
        // needs to be rendered takes time to render. Thus to avoid a long wait for the next person to hit the page
        // where the cache macro is used, we do 2 things:
        // - put back the removed entry in the cache for now so that users are served with the cached entry
        // - start rendering the content of the macro asynchronously and when it's done update the cache entry with
        //   the result.

        // Put back the entry in the cache
        CacheEntry<CacheValue> entry = event.getEntry();
        Cache<CacheValue> cache = event.getCache();
        cache.set(entry.getKey(), entry.getValue());

        // We pass the cache and entry so that we can update the cache with it when the recomputation is over.
        CacheMacroRecomputeRequest request =
            new CacheMacroRecomputeRequest(cache, entry.getKey(), entry.getValue().getStaticXDOM());
        try {
            // This executes asynchronously
            this.jobExecutor.execute(CacheMacroRecomputationJob.JOBTYPE, request);
        } catch (JobException e) {
            // The job failed to be loaded, we cannot do the recomputation and thus we remove the entry from the cache
            cache.remove(entry.getKey());
        }
    }
}
