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

import org.junit.jupiter.api.Test;
import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheManager;
import org.xwiki.cache.config.CacheConfiguration;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.wiki.descriptor.WikiDescriptor;
import org.xwiki.wiki.internal.descriptor.DefaultWikiDescriptor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link org.xwiki.wiki.internal.manager.DefaultWikiDescriptorCache}.
 *
 * @version $Id$
 */
@ComponentTest
class WikiDescriptorCacheTest
{
    @InjectMockComponents
    private WikiDescriptorCache wikiDescriptorCache;

    @MockComponent
    private CacheManager cacheManager;

    private Cache<WikiDescriptor> wikiAliasCache;

    private Cache<WikiDescriptor> wikiIdCache;

    @BeforeComponent
    void beforeComponent() throws Exception
    {
        this.wikiAliasCache = mock(Cache.class);
        this.wikiIdCache = mock(Cache.class);
        when(this.cacheManager.<WikiDescriptor>createNewCache(any(CacheConfiguration.class)))
            .thenReturn(this.wikiAliasCache, this.wikiIdCache);
    }

    @Test
    void add()
    {
        DefaultWikiDescriptor descriptor = new DefaultWikiDescriptor("wikiid", "wikialias");
        descriptor.addAlias("alias2");

        this.wikiDescriptorCache.add(descriptor);

        verify(this.wikiIdCache).set("wikiid", descriptor);
        verify(this.wikiAliasCache).set("wikialias", descriptor);
        verify(this.wikiAliasCache).set("alias2", descriptor);
    }

    @Test
    void remove()
    {
        DefaultWikiDescriptor descriptor = new DefaultWikiDescriptor("wikiid", "wikialias");
        descriptor.addAlias("alias2");

        this.wikiDescriptorCache.remove(descriptor.getId(), descriptor.getAliases());

        verify(this.wikiIdCache).remove("wikiid");
        verify(this.wikiAliasCache).remove("wikialias");
        verify(this.wikiAliasCache).remove("alias2");
    }
}
