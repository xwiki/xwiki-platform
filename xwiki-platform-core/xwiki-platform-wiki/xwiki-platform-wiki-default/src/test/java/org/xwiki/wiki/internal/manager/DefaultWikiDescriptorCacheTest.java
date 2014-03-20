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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheFactory;
import org.xwiki.cache.config.CacheConfiguration;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.wiki.descriptor.WikiDescriptor;
import org.xwiki.wiki.internal.descriptor.DefaultWikiDescriptor;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link org.xwiki.wiki.internal.manager.DefaultWikiDescriptorCache}.
 *
 * @version $Id$
 * @since 6.0M1
 */
public class DefaultWikiDescriptorCacheTest
{
    @Rule
    public MockitoComponentMockingRule<DefaultWikiDescriptorCache> mocker =
            new MockitoComponentMockingRule(DefaultWikiDescriptorCache.class);

    private Cache<WikiDescriptor> wikiAliasCache;

    private Cache<WikiDescriptor> wikiIdCache;

    private CacheFactory cacheFactory;

    @Before
    public void setUp() throws Exception
    {
        wikiAliasCache = mock(Cache.class);
        wikiIdCache = mock(Cache.class);
        cacheFactory = this.mocker.getInstance(CacheFactory.class);
        when(cacheFactory.<WikiDescriptor>newCache(any(CacheConfiguration.class))).thenReturn(wikiAliasCache,
                wikiIdCache);
    }

    @Test
    public void add() throws Exception
    {
        DefaultWikiDescriptor descriptor = new DefaultWikiDescriptor("wikiid", "wikialias");
        descriptor.addAlias("alias2");

        this.mocker.getComponentUnderTest().add(descriptor);

        verify(wikiIdCache).set("wikiid", descriptor);
        verify(wikiAliasCache).set("wikialias", descriptor);
        verify(wikiAliasCache).set("alias2", descriptor);
    }

    @Test
    public void remove() throws Exception
    {
        DefaultWikiDescriptor descriptor = new DefaultWikiDescriptor("wikiid", "wikialias");
        descriptor.addAlias("alias2");

        this.mocker.getComponentUnderTest().remove(descriptor);

        verify(wikiIdCache).remove("wikiid");
        verify(wikiAliasCache).remove("wikialias");
        verify(wikiAliasCache).remove("alias2");
    }

}
