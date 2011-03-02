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
package org.xwiki.velocity.internal;

import java.util.Collections;
import java.util.Properties;

import org.apache.velocity.tools.generic.ListTool;
import org.apache.velocity.util.introspection.SecureUberspector;
import org.jmock.Expectations;
import org.junit.Assert;
import org.junit.Test;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.test.AbstractMockingComponentTestCase;
import org.xwiki.test.annotation.MockingRequirement;
import org.xwiki.velocity.introspection.ChainingUberspector;
import org.xwiki.velocity.introspection.DeprecatedCheckUberspector;

/**
 * Unit tests for {@link DefaultVelocityConfiguration}.
 *
 * @version $Id$
 * @since 2.4RC1
 */
public class DefaultVelocityConfigurationTest extends AbstractMockingComponentTestCase
{
    @MockingRequirement
    private DefaultVelocityConfiguration configuration;

    /**
     * @see org.xwiki.test.AbstractMockingComponentTestCase#configure()
     */
    public void configure() throws Exception
    {
        final ConfigurationSource source = getComponentManager().lookup(ConfigurationSource.class);
        getMockery().checking(new Expectations() {{
            allowing(source).getProperty("velocity.tools", Properties.class);
            will(returnValue(Collections.emptyMap()));
            allowing(source).getProperty("velocity.properties", Properties.class);
            will(returnValue(Collections.emptyMap()));
        }});
    }

    @Test
    public void testDefaultToolsPresent() throws Exception
    {
        // Verify for example that the List tool is present.
        Assert.assertEquals(ListTool.class.getName(), this.configuration.getTools().get("listtool"));
    }

    @Test
    public void testDefaultPropertiesPresent() throws Exception
    {
        // Verify that the secure uberspector is set by default
        Assert.assertEquals(ChainingUberspector.class.getName(),
            this.configuration.getProperties().getProperty("runtime.introspector.uberspect"));
        Assert.assertEquals(SecureUberspector.class.getName() + ","  + DeprecatedCheckUberspector.class.getName(),
            this.configuration.getProperties().getProperty("runtime.introspector.uberspect.chainClasses"));

        // Verify that null values are allowed by default
        Assert.assertEquals(Boolean.TRUE.toString(),
            this.configuration.getProperties().getProperty("directive.set.null.allowed"));

        // Verify that Macros are isolated by default
        Assert.assertEquals(Boolean.TRUE.toString(),
            this.configuration.getProperties().getProperty("velocimacro.permissions.allow.inline.local.scope"));
    }
}
