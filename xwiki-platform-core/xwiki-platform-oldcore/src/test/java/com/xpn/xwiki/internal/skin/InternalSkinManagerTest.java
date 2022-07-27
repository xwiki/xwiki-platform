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
package com.xpn.xwiki.internal.skin;

import java.net.URL;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheManager;
import org.xwiki.cache.config.LRUCacheConfiguration;
import org.xwiki.classloader.ClassLoaderManager;
import org.xwiki.environment.Environment;
import org.xwiki.skin.Skin;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test of {@link InternalSkinManager}.
 *
 * @version $Id$
 * @since 13.8RC1
 */
@ComponentTest
class InternalSkinManagerTest
{
    @InjectMockComponents
    private InternalSkinManager internalSkinManager;

    @MockComponent
    private WikiSkinUtils wikiSkinUtils;

    @MockComponent
    private Environment environment;

    @MockComponent
    private CacheManager cacheManager;

    @MockComponent
    private WikiDescriptorManager wikiDescriptorManager;

    @MockComponent
    private ClassLoaderManager classLoaderManager;

    @Mock
    private Cache<Skin> cache;

    @Mock
    private Skin skin;

    @BeforeEach
    void setUp() throws Exception
    {
        when(this.cacheManager.<Skin>createNewCache(any(LRUCacheConfiguration.class))).thenReturn(this.cache);
        this.internalSkinManager.initialize();
    }

    @Test
    void getSkinEmptyString()
    {
        assertNull(this.internalSkinManager.getSkin(""));
    }

    @Test
    void getSkinCached()
    {
        when(this.cache.get("test")).thenReturn(this.skin);

        Skin returnedSkin = this.internalSkinManager.getSkin("test");

        assertSame(this.skin, returnedSkin);
        verify(this.cache, never()).set(eq("test"), any());
    }

    @Test
    void getSkinCreateSkinWiki()
    {
        when(this.wikiSkinUtils.isWikiSkin("test")).thenReturn(true);

        Skin returnedSkin = this.internalSkinManager.getSkin("test");

        assertEquals("test", returnedSkin.getId());
        assertEquals(WikiSkin.class, returnedSkin.getClass());
        verify(this.cache).set(eq("test"), any(WikiSkin.class));
    }

    @Test
    void getSkinCreateEnvironmentWiki() throws Exception
    {
        when(this.wikiSkinUtils.isWikiSkin("test")).thenReturn(false);

        when(this.environment.getResource("/skins/test/skin.properties")).thenReturn(new URL("file:/"));

        Skin returnedSkin = this.internalSkinManager.getSkin("test");

        assertEquals("test", returnedSkin.getId());
        assertEquals(EnvironmentSkin.class, returnedSkin.getClass());
        verify(this.cache).set(eq("test"), any(EnvironmentSkin.class));
    }

    @Test
    void getSkinCreateClassLoaderWiki() throws Exception
    {
        when(this.wikiSkinUtils.isWikiSkin("test")).thenReturn(false);
        when(this.environment.getResource("/skins/test/skin.properties")).thenReturn(null);
        when(this.wikiDescriptorManager.getCurrentWikiId()).thenReturn("mywiki");

        Skin returnedSkin = this.internalSkinManager.getSkin("test");

        assertEquals("test", returnedSkin.getId());
        assertEquals(ClassLoaderSkin.class, returnedSkin.getClass());
        verify(this.cache).set(eq("test"), any(ClassLoaderSkin.class));
        verify(this.classLoaderManager).getURLClassLoader("wiki:mywiki", false);
    }
}
