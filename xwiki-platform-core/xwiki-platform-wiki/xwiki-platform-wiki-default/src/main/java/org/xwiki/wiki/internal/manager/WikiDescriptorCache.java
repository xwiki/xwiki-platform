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
package org.xwiki.wiki.internal.manager;

import java.util.Collection;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheException;
import org.xwiki.cache.CacheManager;
import org.xwiki.cache.config.CacheConfiguration;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.wiki.internal.descriptor.DefaultWikiDescriptor;

/**
 * Component that handle caching for wiki descriptors.
 * 
 * @version $Id$
 * @since 5.3M2
 */
@Component(roles = WikiDescriptorCache.class)
@Singleton
public class WikiDescriptorCache implements Initializable
{
    @Inject
    private CacheManager cacheManager;

    private Cache<DefaultWikiDescriptor> wikiAliasCache;

    private Cache<DefaultWikiDescriptor> wikiIdCache;

    private Collection<String> wikiIds;

    @Override
    public void initialize() throws InitializationException
    {
        this.wikiAliasCache = createCache("wiki.descriptor.cache.wikiAlias");
        this.wikiIdCache = createCache("wiki.descriptor.cache.wikiId");
    }

    private Cache<DefaultWikiDescriptor> createCache(String cacheId) throws InitializationException
    {
        CacheConfiguration configuration = new CacheConfiguration(cacheId);

        try {
            return this.cacheManager.createNewCache(configuration);
        } catch (CacheException e) {
            throw new InitializationException(
                String.format("Failed to initialize wiki descriptor caches [%s]", configuration.getConfigurationId()),
                e);
        }
    }

    /**
     * Add a descriptor to the cache.
     *
     * @param descriptor descriptor to add
     */
    public void add(DefaultWikiDescriptor descriptor)
    {
        // Update the wiki name cache
        addFromId(descriptor.getId(), descriptor);

        // Update the wiki alias cache
        for (String alias : descriptor.getAliases()) {
            if (alias != null) {
                addFromAlias(alias, descriptor);
            }
        }
    }

    /**
     * Add a descriptor to the cache.
     *
     * @param wikiAlias Alias of the wiki to get
     * @param descriptor descriptor to add
     */
    public void addFromAlias(String wikiAlias, DefaultWikiDescriptor descriptor)
    {
        this.wikiAliasCache.set(wikiAlias, descriptor);
    }

    /**
     * Add a descriptor to the cache.
     * 
     * @param wikiId Id of the wiki to get
     * @param descriptor descriptor to add
     */
    public void addFromId(String wikiId, DefaultWikiDescriptor descriptor)
    {
        this.wikiIdCache.set(wikiId, descriptor);
    }

    /**
     * Remove a descriptor from the cache.
     * 
     * @param wikiId the wiki id to remove
     * @param aliases the wiki aliases to remove
     * @since 8.4.6
     * @since 9.9RC1
     */
    public void remove(String wikiId, List<String> aliases)
    {
        // Remove from the wiki name cache
        this.wikiIdCache.remove(wikiId);

        // Remove from the wiki alias cache
        for (String alias : aliases) {
            this.wikiAliasCache.remove(alias);
        }
    }

    /**
     * Get a descriptor from the cache.
     *
     * @param wikiId Id of the wiki to get
     * @return the descriptor related to the id or null if there is no corresponding descriptor in the cache
     */
    public DefaultWikiDescriptor getFromId(String wikiId)
    {
        return wikiIdCache.get(wikiId);
    }

    /**
     * Get a descriptor from the cache.
     *
     * @param wikiAlias Alias of the wiki to get
     * @return the descriptor related to the alias or null if there is no corresponding descriptor in the cache
     */
    public DefaultWikiDescriptor getFromAlias(String wikiAlias)
    {
        return wikiAliasCache.get(wikiAlias);
    }

    /**
     * @param wikiIds the full list of wikis identifiers
     * @since 6.2M1
     */
    public void setWikiIds(Collection<String> wikiIds)
    {
        this.wikiIds = wikiIds;
    }

    /**
     * @return the full list of wikis identifiers
     * @since 6.2M1
     */
    public Collection<String> getWikiIds()
    {
        return this.wikiIds;
    }
}
