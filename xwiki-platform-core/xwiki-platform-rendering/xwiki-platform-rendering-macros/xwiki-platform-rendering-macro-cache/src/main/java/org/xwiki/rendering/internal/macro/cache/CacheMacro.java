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

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheException;
import org.xwiki.cache.CacheManager;
import org.xwiki.cache.config.LRUCacheConfiguration;
import org.xwiki.component.annotation.Component;
import org.xwiki.job.Job;
import org.xwiki.job.JobExecutor;
import org.xwiki.logging.LogLevel;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.macro.AbstractMacro;
import org.xwiki.rendering.macro.MacroContentParser;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.cache.CacheMacroParameters;
import org.xwiki.rendering.macro.descriptor.DefaultContentDescriptor;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.rendering.transformation.MacroTransformationContext;

/**
 * Provides Caching for the content of the macro.
 * 
 * @version $Id$
 * @since 3.0M1
 */
@Component
@Named("cache")
@Singleton
public class CacheMacro extends AbstractMacro<CacheMacroParameters>
{
    /**
     * The description of the macro.
     */
    private static final String DESCRIPTION = "Caches content.";

    /**
     * The description of the macro content.
     */
    private static final String CONTENT_DESCRIPTION = "the content to cache.";

    @Inject
    private Logger logger;

    /**
     * Used to create the macro content cache.
     */
    @Inject
    private CacheManager cacheManager;

    /**
     * The parser used to parse the content (when not cached).
     */
    @Inject
    private MacroContentParser contentParser;

    /**
     * Renders the optional id parameter as plain text to use the result as a cache key.
     */
    @Inject
    @Named("plain/1.0")
    private BlockRenderer plainTextBlockRenderer;

    @Inject
    private JobExecutor jobExecutor;

    /**
     * Map of all caches. There's one cache per timeToLive/maxEntries combination since currently we cannot set these
     * configuration values at the cache entry level but only for the whole cache.
     */
    private Map<CacheKey, Cache<CacheValue>> contentCacheMap = new ConcurrentHashMap<>();

    /**
     * Create and initialize the descriptor of the macro.
     */
    public CacheMacro()
    {
        super("Cache", DESCRIPTION, new DefaultContentDescriptor(CONTENT_DESCRIPTION), CacheMacroParameters.class);
        setDefaultCategory(DEFAULT_CATEGORY_DEVELOPMENT);
    }

    @Override
    public boolean supportsInlineMode()
    {
        return true;
    }

    @Override
    public List<Block> execute(CacheMacroParameters parameters, String content, MacroTransformationContext context)
        throws MacroExecutionException
    {
        // Idea for improvement: use context.getId() (which contains the document name) as part of the cache key to
        // make it even more unique (when the cache macro parameter id is not specified).
        String cacheKey;
        if (parameters.getId() != null) {
            // Consider that the id contains wiki syntax and parse it with the same wiki parser than the current
            // transformation is using and render the result as plain text.
            WikiPrinter printer = new DefaultWikiPrinter();
            this.plainTextBlockRenderer.render(this.contentParser.parse(parameters.getId(), context, true, false),
                printer);
            cacheKey = printer.toString();
        } else {
            cacheKey = content;
        }

        Cache<CacheValue> contentCache = getContentCache(parameters.getTimeToLive(), parameters.getMaxEntries());
        CacheValue value = contentCache.get(cacheKey);
        if (value == null) {
            this.logger.debug("Cached content for cache [{}] is going to be computed", cacheKey);
            // Parse the macro content without executing the Macro transformation, since we want to save the static
            // Blocks separately. We do this so that we can apply the Macro Transformation again when the cache entry
            // expires and thus always have up to date cached content to return.
            XDOM staticXDOM = this.contentParser.parse(content, context, false, context.isInline());

            // Execute the recomputation job to execute the macro transformation on the static XDOM
            CacheMacroRecomputeRequest request = new CacheMacroRecomputeRequest(contentCache, cacheKey, staticXDOM);
            Job job;
            try {
                // This executes asynchronously and thus we wait before returning. The reason we call the job here
                // is to make sure we always return the same rendered content whether the cache macro content has not
                // been put in the cache yet or oce it's in the cache and it's expired and it's been recomputed.
                // It also allows us to cleanly isolate from the current execution context (since it runs in another
                // thread).
                job = this.jobExecutor.execute(CacheMacroRecomputationJob.JOBTYPE, request);
                job.join();
            } catch (Exception e) {
                throw new MacroExecutionException("Failed to create the job to render the Cache Macro content", e);
            }

            // If the job failed then it would have removed the entry from the cache
            value = contentCache.get(cacheKey);
            if (value == null) {
                // The job failed, throw an error and get the excetion from the job
                throw new MacroExecutionException("Failed to render the Cache Macro content",
                    job.getStatus().getLog().getLogs(LogLevel.ERROR).get(0).getThrowable());
            }
        } else {
            this.logger.debug("Cached content for cache [{}] found, no recomputation done", cacheKey);
        }

        return value.getTransformedXDOM().getChildren();
    }

    /**
     * Get a cache matching the passed time to live and max entries.
     * <p>
     * Note in the future, we need to modify our xwiki-cache module to allow setting time to live on cache items,
     * see http://jira.xwiki.org/jira/browse/XWIKI-5907
     * </p>
     *
     * @param lifespan the number of seconds to cache the content
     * @param maxEntries the maximum number of entries in the cache (Least Recently Used entries are ejected)
     * @return the matching cache (a new cache is created if no existing one is found)
     * @throws MacroExecutionException in case we fail to create the new cache
     */
    Cache<CacheValue> getContentCache(int lifespan, int maxEntries) throws MacroExecutionException
    {
        CacheKey cacheKey = new CacheKey(lifespan, maxEntries);
        Cache<CacheValue> contentCache = this.contentCacheMap.get(cacheKey);
        if (contentCache == null) {
            // Create Cache
            LRUCacheConfiguration configuration =
                new LRUCacheConfiguration(String.format("cacheMacro.%s", cacheKey.toString()), maxEntries);
            configuration.getLRUEvictionConfiguration().setLifespan(lifespan);

            try {
                contentCache = this.cacheManager.createNewLocalCache(configuration);
            } catch (CacheException e) {
                throw new MacroExecutionException("Failed to create content cache", e);
            }

            contentCache.addCacheEntryListener(new CacheMacroCacheEntryListener(this.jobExecutor));
            this.contentCacheMap.put(cacheKey, contentCache);
        }

        return contentCache;
    }
}
