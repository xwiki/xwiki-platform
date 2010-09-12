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
package org.xwiki.rendering.internal.configuration;

import java.util.Properties;

import org.jmock.Expectations;
import org.junit.Assert;
import org.junit.Test;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.test.AbstractMockingComponentTestCase;
import org.xwiki.test.annotation.MockingRequirement;

/**
 * Unit tests for {@link org.xwiki.rendering.internal.configuration.XWikiRenderingConfiguration}.
 * 
 * @version $Id$
 * @since 2.0M1
 */
public class XWikiRenderingConfigurationTest extends AbstractMockingComponentTestCase
{
    @MockingRequirement
    private XWikiRenderingConfiguration configuration;

    @Test
    public void testGetLinkLabelFormat() throws Exception
    {
        final ConfigurationSource source = getComponentManager().lookup(ConfigurationSource.class);
        getMockery().checking(new Expectations()
        {
            {
                allowing(source).getProperty("rendering.linkLabelFormat", "%p");
                will(returnValue("%p"));
            }
        });

        Assert.assertEquals("%p", this.configuration.getLinkLabelFormat());
    }

    @Test
    public void testGetMacroCategories() throws Exception
    {
        final ConfigurationSource source = getComponentManager().lookup(ConfigurationSource.class);
        getMockery().checking(new Expectations()
        {
            {
                allowing(source).getProperty("rendering.macroCategories", Properties.class);
                will(returnValue(new Properties()));
            }
        });

        Assert.assertNotNull(this.configuration.getMacroCategories());
        Assert.assertEquals(0, this.configuration.getMacroCategories().size());
    }

    @Test
    public void testGetImageWidthLimit() throws Exception
    {
        final ConfigurationSource source = getComponentManager().lookup(ConfigurationSource.class);
        getMockery().checking(new Expectations()
        {
            {
                allowing(source).getProperty("rendering.imageWidthLimit", -1);
                will(returnValue(100));
            }
        });

        Assert.assertEquals(100, this.configuration.getImageWidthLimit());
    }

    @Test
    public void testGetImageHeightLimit() throws Exception
    {
        final ConfigurationSource source = getComponentManager().lookup(ConfigurationSource.class);
        getMockery().checking(new Expectations()
        {
            {
                allowing(source).getProperty("rendering.imageHeightLimit", -1);
                will(returnValue(150));
            }
        });

        Assert.assertEquals(150, this.configuration.getImageHeightLimit());
    }

    @Test
    public void testIsIncludeImageDimensionsInImageURL() throws Exception
    {
        final ConfigurationSource source = getComponentManager().lookup(ConfigurationSource.class);
        getMockery().checking(new Expectations()
        {
            {
                allowing(source).getProperty("rendering.includeImageDimensionsInImageURL", true);
                will(returnValue(false));
            }
        });

        Assert.assertFalse(this.configuration.isIncludeImageDimensionsInImageURL());
    }
}
