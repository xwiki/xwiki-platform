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
package org.xwiki.configuration.internal;

import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Properties;

import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.configuration.ConversionException;

/**
 * Unit tests for {@link CommonsConfigurationSource}.
 * 
 * @version $Id$
 * @since 2.0M1
 */
public class CommonsConfigurationSourceTest
{
    private Configuration configuration;
    
    private CommonsConfigurationSource source;
    
    @Before
    public void testSetup()
    {
        this.source = new CommonsConfigurationSource();
        this.configuration = new BaseConfiguration();
        this.source.setConfiguration(this.configuration);
    }

    @Test
    public void testDefaultValue()
    {
        configuration.setProperty("string", "value");
        
        Assert.assertEquals("default", source.getProperty("unknown", "default"));
        Assert.assertEquals("value", source.getProperty("string", "default"));
    }

    @Test
    public void testStringProperty()
    {
        configuration.setProperty("string", "value");

        Assert.assertEquals("value", source.getProperty("string"));
        Assert.assertEquals("value", source.getProperty("string", String.class));

        Assert.assertNull(source.getProperty("unknown"));
        Assert.assertNull(source.getProperty("unknown", String.class));
    }

    @Test(expected = ConversionException.class)
    public void testStringPropertyWhenConversionError()
    {
        configuration.setProperty("string", "value");

        // Try to retrieve a String property as a Boolean
        source.getProperty("string", Boolean.class);
    }

    @Test(expected = ConversionException.class)
    public void testBooleanPropertyWhenConversionError()
    {
        configuration.setProperty("boolean", true);

        // Try to retrieve a Boolean property as a String
        source.getProperty("boolean", String.class);
    }
    
    @Test
    public void testBooleanProperty()
    {
        // Test boolean value
        configuration.setProperty("boolean", true);

        Assert.assertEquals(true, source.getProperty("boolean"));
        Assert.assertEquals(true, source.getProperty("boolean", Boolean.class));
    }
    
    @Test(expected = NoSuchElementException.class)
    public void testUnknownBooleanProperty()
    {
        source.getProperty("unknown", Boolean.class);
    }
    
    @Test
    public void testListProperty()
    {
        configuration.setProperty("list", "value1");
        configuration.addProperty("list", "value2");
        List<String> expected = Arrays.asList("value1", "value2");

        Assert.assertEquals(expected, source.getProperty("list"));
        Assert.assertEquals(expected, source.getProperty("list", List.class));

        Assert.assertTrue(source.getProperty("unknown", List.class).isEmpty());
    }

    @Test
    public void testPropertiesProperty()
    {
        configuration.setProperty("properties", "key1=value1");
        configuration.addProperty("properties", "key2=value2");
        List<String> expectedList = Arrays.asList("key1=value1", "key2=value2");
        Properties expectedProperties = new Properties();
        expectedProperties.put("key1", "value1");
        expectedProperties.put("key2", "value2");

        Assert.assertEquals(expectedList, source.getProperty("properties"));
        Assert.assertEquals(expectedProperties, source.getProperty("properties", Properties.class));

        Assert.assertTrue(source.getProperty("unknown", Properties.class).isEmpty());
    }
}
