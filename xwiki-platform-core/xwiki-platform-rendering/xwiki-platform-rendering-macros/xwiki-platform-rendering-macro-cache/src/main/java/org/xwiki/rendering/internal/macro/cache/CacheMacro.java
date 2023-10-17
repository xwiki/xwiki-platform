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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheException;
import org.xwiki.cache.CacheManager;
import org.xwiki.cache.config.LRUCacheConfiguration;
import org.xwiki.component.annotation.Component;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.macro.AbstractMacro;
import org.xwiki.rendering.macro.MacroContentParser;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.MacroPreparationException;
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

    /**
     * Map of all caches. There's one cache per timeToLive/maxEntry combination since currently we cannot set these
     * configuration values at the cache entry level but only for the whole cache.
     */
    private Map<CacheKey, Cache<List<Block>>> contentCacheMap = new ConcurrentHashMap<>();

    /**
     * Create and initialize the descriptor of the macro.
     */
    public CacheMacro()
    {
        super("Cache", DESCRIPTION, new DefaultContentDescriptor(CONTENT_DESCRIPTION, true, Block.LIST_BLOCK_TYPE),
            CacheMacroParameters.class);
        setDefaultCategories(Set.of(DEFAULT_CATEGORY_DEVELOPMENT));
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

        Cache<List<Block>> contentCache = getContentCache(parameters.getTimeToLive(), parameters.getMaxEntries());
        List<Block> result = contentCache.get(cacheKey);
        if (result == null) {
            // Run the parser for the syntax on the content
            // We run the current transformation on the cache macro content. We need to do this since we want to cache
            // the XDOM resulting from the execution of Macros because that's where lengthy processing happens.
            result = this.contentParser.parse(content, context, true, context.isInline()).getChildren();
            contentCache.set(cacheKey, result);
        }

        return result;
    }

    /**
     * Get a cache matching the passed time to live and max entries.
     * <p>
     * Note that whenever a new cache is created it currently means a new thread is used too (since the JBoss cache used
     * underneath uses a thread for evicting entries from the cache). We need to modify our xwiki-cache module to allow
     * setting time to live on cache items, see https://jira.xwiki.org/browse/XWIKI-5907
     * </p>
     *
     * @param lifespan the number of seconds to cache the content
     * @param maxEntries the maximum number of entries in the cache (Least Recently Used entries are ejected)
     * @return the matching cache (a new cache is created if no existing one is found)
     * @throws MacroExecutionException in case we fail to create the new cache
     */
    Cache<List<Block>> getContentCache(int lifespan, int maxEntries) throws MacroExecutionException
    {
        CacheKey cacheKey = new CacheKey(lifespan, maxEntries);
        Cache<List<Block>> contentCache = this.contentCacheMap.get(cacheKey);
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

            this.contentCacheMap.put(cacheKey, contentCache);
        }

        return contentCache;
    }

    @Override
    public void prepare(MacroBlock macroBlock) throws MacroPreparationException
    {
        this.contentParser.prepareContentWiki(macroBlock);
    }
}
