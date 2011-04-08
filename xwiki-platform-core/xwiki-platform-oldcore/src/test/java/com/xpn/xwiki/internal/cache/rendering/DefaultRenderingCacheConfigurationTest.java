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
import org.junit.Test;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.MockConfigurationSource;

import com.xpn.xwiki.test.AbstractBridgedXWikiComponentTestCase;

/**
 * Unit test for {@link DefaultRenderingCacheConfiguration}.
 * 
 * @version $Id$
 * @since 2.4M1
 */
public class DefaultRenderingCacheConfigurationTest extends AbstractBridgedXWikiComponentTestCase
{
    private RenderingCacheConfiguration configuration;

    private DocumentReference documentReference;

    @Override
    protected void registerComponents() throws Exception
    {
        super.registerComponents();

        getComponentManager().registerComponent(MockConfigurationSource.getDescriptor("wiki"));
    }

    private MockConfigurationSource getWikiConfigurationSource() throws Exception
    {
        return (MockConfigurationSource) getComponentManager().lookup(ConfigurationSource.class, "wiki");
    }

    private MockConfigurationSource getXWikiPropertiesConfigurationSource() throws Exception
    {
        return (MockConfigurationSource) getComponentManager().lookup(ConfigurationSource.class, "xwikiproperties");
    }

    @Before
    public void setUp() throws Exception
    {
        super.setUp();

        this.configuration = getComponentManager().lookup(RenderingCacheConfiguration.class);
        this.documentReference = new DocumentReference("wiki", "space", "page");

        getContext().setDatabase("wiki");
    }

    @Test
    public void testIsCachedWithNoConfiguration() throws Exception
    {
        Assert.assertFalse(this.configuration.isCached(this.documentReference));
    }

    @Test
    public void testIsCachedWhenDisabled() throws Exception
    {
        MockConfigurationSource source = getXWikiPropertiesConfigurationSource();

        source.setProperty("core.renderingcache.enabled", false);

        Assert.assertFalse(this.configuration.isCached(this.documentReference));
    }

    @Test
    public void testIsCachedWhenDisabledWithNoConfiguration() throws Exception
    {
        MockConfigurationSource source = getXWikiPropertiesConfigurationSource();

        source.setProperty("core.renderingcache.enabled", true);

        Assert.assertFalse(this.configuration.isCached(this.documentReference));
    }

    @Test
    public void testIsCachedWithExactReference() throws Exception
    {
        MockConfigurationSource source = getXWikiPropertiesConfigurationSource();

        source.setProperty("core.renderingcache.enabled", true);
        source.setProperty("core.renderingcache.documents", Collections.singletonList("wiki:space.page"));

        Assert.assertTrue(this.configuration.isCached(this.documentReference));
    }

    @Test
    public void testIsCachedWithWrongReference() throws Exception
    {
        MockConfigurationSource source = getXWikiPropertiesConfigurationSource();

        source.setProperty("core.renderingcache.enabled", true);
        source.setProperty("core.renderingcache.documents", Collections.singletonList("wrongreference"));

        Assert.assertFalse(this.configuration.isCached(this.documentReference));
    }

    @Test
    public void testIsCachedWithOnePattern() throws Exception
    {
        MockConfigurationSource source = getXWikiPropertiesConfigurationSource();

        source.setProperty("core.renderingcache.enabled", true);
        source.setProperty("core.renderingcache.documents", Collections.singletonList("wiki:space.*"));

        Assert.assertTrue(this.configuration.isCached(this.documentReference));
    }

    @Test
    public void testIsCachedWithSeveralPattern() throws Exception
    {
        MockConfigurationSource source = getXWikiPropertiesConfigurationSource();

        source.setProperty("core.renderingcache.enabled", true);
        source.setProperty("core.renderingcache.documents", Arrays.asList("wrongreference", "wiki:space.*"));

        Assert.assertTrue(this.configuration.isCached(this.documentReference));
    }

    @Test
    public void testIsCachedWithSeveralPattern2() throws Exception
    {
        MockConfigurationSource source = getXWikiPropertiesConfigurationSource();

        source.setProperty("core.renderingcache.enabled", true);
        source.setProperty("core.renderingcache.documents", Arrays.asList("wiki:space.*", "wrongreference"));

        Assert.assertTrue(this.configuration.isCached(this.documentReference));
    }

    @Test
    public void testIsCachedWithWikiConfiguration() throws Exception
    {
        getXWikiPropertiesConfigurationSource().setProperty("core.renderingcache.enabled", true);

        MockConfigurationSource wikiSource = getWikiConfigurationSource();

        wikiSource.setProperty("core.renderingcache.enabled", true);
        wikiSource.setProperty("core.renderingcache.documents", Arrays.asList("wiki:space.*", "wrongreference"));

        Assert.assertTrue(this.configuration.isCached(this.documentReference));

        wikiSource.setProperty("core.renderingcache.documents", Arrays.asList("space.*", "wrongreference"));

        Assert.assertTrue(this.configuration.isCached(this.documentReference));
    }
}
