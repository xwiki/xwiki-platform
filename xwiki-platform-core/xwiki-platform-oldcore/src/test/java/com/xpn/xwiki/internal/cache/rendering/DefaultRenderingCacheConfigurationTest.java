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

import java.util.Arrays;
import java.util.Collections;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
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
import com.xpn.xwiki.test.MockitoOldcoreRule;

/**
 * Unit test for {@link DefaultRenderingCacheConfiguration}.
 * 
 * @version $Id$
 */
@ComponentList(value = {
    DefaultRenderingCacheConfiguration.class,
    DefaultModelContext.class,
    CompactWikiStringEntityReferenceSerializer.class,
    DefaultStringEntityReferenceSerializer.class,
    CurrentEntityReferenceProvider.class,
    DefaultModelConfiguration.class,
    DefaultSymbolScheme.class
})
public class DefaultRenderingCacheConfigurationTest
{
    @Rule
    public MockitoOldcoreRule oldcore = new MockitoOldcoreRule();

    private RenderingCacheConfiguration configuration;

    private MockConfigurationSource xwikipropertiesConfiguration;

    private MockConfigurationSource wikiConfiguration;

    private DocumentReference documentReference = new DocumentReference("wiki", "space", "page");

    @Before
    public void before() throws Exception
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
    public void testIsCachedWithNoConfiguration() throws Exception
    {
        Assert.assertFalse(this.configuration.isCached(this.documentReference));
    }

    @Test
    public void testIsCachedWhenDisabled() throws Exception
    {
        this.xwikipropertiesConfiguration.setProperty("core.renderingcache.enabled", false);

        Assert.assertFalse(this.configuration.isCached(this.documentReference));
    }

    @Test
    public void testIsCachedWhenDisabledWithNoConfiguration() throws Exception
    {
        this.xwikipropertiesConfiguration.setProperty("core.renderingcache.enabled", true);

        Assert.assertFalse(this.configuration.isCached(this.documentReference));
    }

    @Test
    public void testIsCachedWithExactReference() throws Exception
    {
        this.xwikipropertiesConfiguration.setProperty("core.renderingcache.enabled", true);
        this.xwikipropertiesConfiguration.setProperty("core.renderingcache.documents",
            Collections.singletonList("wiki:space.page"));

        Assert.assertTrue(this.configuration.isCached(this.documentReference));
    }

    @Test
    public void testIsCachedWithWrongReference() throws Exception
    {
        this.xwikipropertiesConfiguration.setProperty("core.renderingcache.enabled", true);
        this.xwikipropertiesConfiguration.setProperty("core.renderingcache.documents",
            Collections.singletonList("wrongreference"));

        Assert.assertFalse(this.configuration.isCached(this.documentReference));
    }

    @Test
    public void testIsCachedWithOnePattern() throws Exception
    {
        this.xwikipropertiesConfiguration.setProperty("core.renderingcache.enabled", true);
        this.xwikipropertiesConfiguration.setProperty("core.renderingcache.documents",
            Collections.singletonList("wiki:space.*"));

        Assert.assertTrue(this.configuration.isCached(this.documentReference));
    }

    @Test
    public void testIsCachedWithSeveralPattern() throws Exception
    {
        this.xwikipropertiesConfiguration.setProperty("core.renderingcache.enabled", true);
        this.xwikipropertiesConfiguration.setProperty("core.renderingcache.documents",
            Arrays.asList("wrongreference", "wiki:space.*"));

        Assert.assertTrue(this.configuration.isCached(this.documentReference));
    }

    @Test
    public void testIsCachedWithSeveralPattern2() throws Exception
    {
        this.xwikipropertiesConfiguration.setProperty("core.renderingcache.enabled", true);
        this.xwikipropertiesConfiguration.setProperty("core.renderingcache.documents",
            Arrays.asList("wiki:space.*", "wrongreference"));

        Assert.assertTrue(this.configuration.isCached(this.documentReference));
    }

    @Test
    public void testIsCachedWithWikiConfiguration() throws Exception
    {
        this.xwikipropertiesConfiguration.setProperty("core.renderingcache.enabled", true);

        this.wikiConfiguration.setProperty("core.renderingcache.enabled", true);
        this.wikiConfiguration.setProperty("core.renderingcache.documents",
            Arrays.asList("wiki:space.*", "wrongreference"));

        Assert.assertTrue(this.configuration.isCached(this.documentReference));

        this.wikiConfiguration.setProperty("core.renderingcache.documents", Arrays.asList("space.*", "wrongreference"));

        Assert.assertTrue(this.configuration.isCached(this.documentReference));
    }
}
