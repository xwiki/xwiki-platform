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
package com.xpn.xwiki.internal.cache.rendering;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.model.internal.DefaultModelConfiguration;
import org.xwiki.model.internal.DefaultModelContext;
import org.xwiki.model.internal.reference.DefaultStringEntityReferenceSerializer;
import org.xwiki.model.internal.reference.DefaultSymbolScheme;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.internal.MockConfigurationSource;

import com.xpn.xwiki.internal.model.reference.CompactWikiStringEntityReferenceSerializer;
import com.xpn.xwiki.internal.model.reference.CurrentEntityReferenceProvider;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit test for {@link DefaultRenderingCacheConfiguration}.
 * 
 * @version $Id$
 */
@OldcoreTest
@ComponentList(value = {
    DefaultRenderingCacheConfiguration.class,
    DefaultModelContext.class,
    CompactWikiStringEntityReferenceSerializer.class,
    DefaultStringEntityReferenceSerializer.class,
    CurrentEntityReferenceProvider.class,
    DefaultModelConfiguration.class,
    DefaultSymbolScheme.class
})
class DefaultRenderingCacheConfigurationTest
{
    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    private RenderingCacheConfiguration configuration;

    private MockConfigurationSource xwikipropertiesConfiguration;

    private MockConfigurationSource wikiConfiguration;

    private DocumentReference documentReference = new DocumentReference("wiki", "space", "page");

    @BeforeEach
    void before() throws Exception
    {
        this.oldcore.getXWikiContext().setWikiId("wiki");

        this.oldcore.getMocker().registerComponent(MockConfigurationSource.getDescriptor("xwikiproperties"));
        this.oldcore.getMocker().registerComponent(MockConfigurationSource.getDescriptor("wiki"));

        this.xwikipropertiesConfiguration =
            this.oldcore.getMocker().getInstance(ConfigurationSource.class, "xwikiproperties");
        this.wikiConfiguration = this.oldcore.getMocker().getInstance(ConfigurationSource.class, "wiki");

        this.configuration = this.oldcore.getMocker().getInstance(RenderingCacheConfiguration.class);
    }

    @Test
    void isCachedWithNoConfiguration()
    {
        assertFalse(this.configuration.isCached(this.documentReference));
    }

    @Test
    void isCachedWhenDisabled()
    {
        this.xwikipropertiesConfiguration.setProperty("core.renderingcache.enabled", false);

        assertFalse(this.configuration.isCached(this.documentReference));
    }

    @Test
    void isCachedWhenDisabledWithNoConfiguration()
    {
        this.xwikipropertiesConfiguration.setProperty("core.renderingcache.enabled", true);

        assertFalse(this.configuration.isCached(this.documentReference));
    }

    @Test
    void isCachedWithExactReference()
    {
        this.xwikipropertiesConfiguration.setProperty("core.renderingcache.enabled", true);
        this.xwikipropertiesConfiguration.setProperty("core.renderingcache.documents",
            List.of("wiki:space.page"));

        assertTrue(this.configuration.isCached(this.documentReference));
    }

    @Test
    void isCachedWithWrongReference()
    {
        this.xwikipropertiesConfiguration.setProperty("core.renderingcache.enabled", true);
        this.xwikipropertiesConfiguration.setProperty("core.renderingcache.documents",
            List.of("wrongreference"));

        assertFalse(this.configuration.isCached(this.documentReference));
    }

    @Test
    void isCachedWithOnePattern()
    {
        this.xwikipropertiesConfiguration.setProperty("core.renderingcache.enabled", true);
        this.xwikipropertiesConfiguration.setProperty("core.renderingcache.documents",
            List.of("wiki:space.*"));

        assertTrue(this.configuration.isCached(this.documentReference));
    }

    @Test
    void isCachedWithSeveralPattern()
    {
        this.xwikipropertiesConfiguration.setProperty("core.renderingcache.enabled", true);
        this.xwikipropertiesConfiguration.setProperty("core.renderingcache.documents",
            List.of("wrongreference", "wiki:space.*"));

        assertTrue(this.configuration.isCached(this.documentReference));
    }

    @Test
    void isCachedWithSeveralPattern2()
    {
        this.xwikipropertiesConfiguration.setProperty("core.renderingcache.enabled", true);
        this.xwikipropertiesConfiguration.setProperty("core.renderingcache.documents",
            List.of("wiki:space.*", "wrongreference"));

        assertTrue(this.configuration.isCached(this.documentReference));
    }

    @Test
    void isCachedWithWikiConfiguration()
    {
        this.xwikipropertiesConfiguration.setProperty("core.renderingcache.enabled", true);

        this.wikiConfiguration.setProperty("core.renderingcache.enabled", true);
        this.wikiConfiguration.setProperty("core.renderingcache.documents",
            List.of("wiki:space.*", "wrongreference"));

        assertTrue(this.configuration.isCached(this.documentReference));

        this.wikiConfiguration.setProperty("core.renderingcache.documents", List.of("space.*", "wrongreference"));

        assertTrue(this.configuration.isCached(this.documentReference));
    }
}
