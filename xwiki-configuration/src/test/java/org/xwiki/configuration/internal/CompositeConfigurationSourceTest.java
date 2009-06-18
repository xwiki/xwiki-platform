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
import java.util.Properties;

import junit.framework.Assert;

import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link CompositeConfigurationSource}.
 * 
 * @version $Id$
 * @since 2.0M1
 */

public class CompositeConfigurationSourceTest
{
    private CompositeConfigurationSource composite;
    
    private Configuration config1;
    
    private Configuration config2;
    
    @Before
    public void testSetup()
    {
        this.composite = new CompositeConfigurationSource();

        CommonsConfigurationSource source1 = new CommonsConfigurationSource();
        this.config1 = new BaseConfiguration();
        source1.setConfiguration(this.config1);
        this.composite.addConfigurationSource(source1);
        
        CommonsConfigurationSource source2 = new CommonsConfigurationSource();
        this.config2 = new BaseConfiguration();
        source2.setConfiguration(this.config2);
        this.composite.addConfigurationSource(source2);
    }

    @Test
    public void testContainsKey()
    {
        config1.setProperty("key1", "value1");
        config1.setProperty("key3", "value3");
        config2.setProperty("key2", "value2");
        config2.setProperty("key3", "value3");
        
        Assert.assertTrue(composite.containsKey("key1"));
        Assert.assertTrue(composite.containsKey("key2"));
        Assert.assertTrue(composite.containsKey("key3"));
        Assert.assertFalse(composite.containsKey("unknown"));
    }
    
    @Test
    public void testGetProperty()
    {
        config1.setProperty("key1", "value1");
        config1.setProperty("key3", "value3");
        config2.setProperty("key2", "value2");
        config2.setProperty("key3", "value3");

        Assert.assertEquals("value1", composite.getProperty("key1"));
        Assert.assertEquals("value2", composite.getProperty("key2"));
        Assert.assertEquals("value3", composite.getProperty("key3"));
        Assert.assertNull(composite.getProperty("unknown"));
    }
    
    @Test
    public void testGetPropertyWithClass()
    {
        config1.setProperty("key1", "value1");
        config1.setProperty("key3", "value3");
        config2.setProperty("key2", "value2");
        config2.setProperty("key3", "value3");

        Assert.assertEquals("value1", composite.getProperty("key1", String.class));
        Assert.assertEquals("value2", composite.getProperty("key2", String.class));
        Assert.assertEquals("value3", composite.getProperty("key3", String.class));
        Assert.assertNull(composite.getProperty("unknown", String.class));
    }
    
    @Test
    public void testGetPropertyWithDefaultValue()
    {
        config1.setProperty("key1", "value1");
        config1.setProperty("key3", "value3");
        config2.setProperty("key2", "value2");
        config2.setProperty("key3", "value3");
        
        Assert.assertEquals("value1", composite.getProperty("key1", "default"));
        Assert.assertEquals("value2", composite.getProperty("key2", "default"));
        Assert.assertEquals("value3", composite.getProperty("key3", "default"));
        Assert.assertEquals("default", composite.getProperty("unknown", "default"));
    }
    
    @Test
    public void testGetKeys()
    {
        config1.setProperty("key1", "value1");
        config1.setProperty("key3", "value3");
        config2.setProperty("key2", "value2");
        config2.setProperty("key3", "value3");

        List<String> expected = Arrays.asList("key1", "key3", "key2");
        Assert.assertEquals(expected, composite.getKeys());
    }
    
    @Test
    public void testIsEmpty()
    {
        Assert.assertTrue(composite.isEmpty());

        config2.setProperty("key", "value");
        Assert.assertFalse(composite.isEmpty());
    }
    
    @Test
    public void testGetPropertiesAndListsWhenEmpty()
    {
        Assert.assertTrue(composite.getProperty("unknown", Properties.class).isEmpty());
        Assert.assertTrue(composite.getProperty("unknown", List.class).isEmpty());
    }
}
