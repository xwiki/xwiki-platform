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
package org.xwiki.rendering.internal.transformation.icon;

import java.util.Properties;

import org.jmock.Expectations;
import org.junit.Assert;
import org.junit.Test;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.test.AbstractMockingComponentTestCase;
import org.xwiki.test.annotation.MockingRequirement;

/**
 * Unit tests for {@link XWikiIconTransformationConfiguration}.
 *
 * @version $Id$
 * @since 2.6RC1
 */
public class XWikiIconTransformationConfigurationTest extends AbstractMockingComponentTestCase
{
    @MockingRequirement
    private XWikiIconTransformationConfiguration configuration;

    private ConfigurationSource source;

    @Override
    public void setUp() throws Exception
    {
        super.setUp();
        this.source = getComponentManager().getInstance(ConfigurationSource.class);
    }

    @Test
    public void testGetMappings() throws Exception
    {
        getMockery().checking(new Expectations()
        {
            {
                oneOf(source).getProperty("rendering.transformation.icon.mappings", Properties.class);
                Properties props = new Properties();
                props.setProperty("::", "test");
                will(returnValue(props));
            }
        });

        Properties mappings = this.configuration.getMappings();
        Assert.assertNotNull(mappings);
        // Make sure we have our mapping coming from the configuration source + the default mappings
        Assert.assertTrue(mappings.size() > 1);
        Assert.assertEquals("test", mappings.getProperty("::"));
    }
}
