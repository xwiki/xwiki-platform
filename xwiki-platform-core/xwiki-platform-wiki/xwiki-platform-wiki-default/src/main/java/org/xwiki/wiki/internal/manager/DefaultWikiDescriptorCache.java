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
 * Default implementation of {@link WikiDescriptorCache}.
 * 
 * @version $Id$
 * @since 5.3M2
 */
@Component
@Singleton
public class DefaultWikiDescriptorCache implements WikiDescriptorCache, Initializable
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
            throw new InitializationException(String.format("Failed to initialize wiki descriptor caches [%s]",
                configuration.getConfigurationId()), e);
        }
    }

    @Override
    public void add(DefaultWikiDescriptor descriptor)
    {
        // Update the wiki name cache
        this.wikiIdCache.set(descriptor.getId(), descriptor);

        // Update the wiki alias cache
        for (String alias : descriptor.getAliases()) {
            this.wikiAliasCache.set(alias, descriptor);
        }
    }

    @Override
    public void remove(DefaultWikiDescriptor descriptor)
    {
        // Remove from the wiki name cache
        this.wikiIdCache.remove(descriptor.getId());

        // Remove from the wiki alias cache
        for (String alias : descriptor.getAliases()) {
            this.wikiAliasCache.remove(alias);
        }
    }

    @Override
    public DefaultWikiDescriptor getFromId(String wikiId)
    {
        return wikiIdCache.get(wikiId);
    }

    @Override
    public DefaultWikiDescriptor getFromAlias(String wikiAlias)
    {
        return wikiAliasCache.get(wikiAlias);
    }

    @Override
    public void setWikiIds(Collection<String> wikiIds)
    {
        this.wikiIds = wikiIds;
    }

    @Override
    public Collection<String> getWikiIds()
    {
        return this.wikiIds;
    }
}
