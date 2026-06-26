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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.test.annotation.AllComponents;
import org.xwiki.test.junit5.mockito.ComponentTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link CompositeConfigurationSource}.
 * 
 * @version $Id$
 * @since 2.0M1
 */
@ComponentTest
@AllComponents
class CompositeConfigurationSourceTest
{
    private MapConfigurationSource config1 = new MapConfigurationSource();

    private MapConfigurationSource config2 = new MapConfigurationSource();

    private CompositeConfigurationSource composite;

    @BeforeEach
    void beforeEach()
    {
        this.composite = new CompositeConfigurationSource();
        this.composite.addConfigurationSource(this.config1);
        this.composite.addConfigurationSource(this.config2);
    }

    @Test
    void testContainsKey()
    {
        this.config1.setProperty("key1", "value1");
        this.config1.setProperty("key3", "value3");
        this.config2.setProperty("key2", "value2");
        this.config2.setProperty("key3", "value3");

        assertTrue(this.composite.containsKey("key1"));
        assertTrue(this.composite.containsKey("key2"));
        assertTrue(this.composite.containsKey("key3"));
        assertFalse(this.composite.containsKey("unknown"));
    }

    @Test
    void testGetProperty()
    {
        this.config1.setProperty("key1", "value1");
        this.config1.setProperty("key3", "value3");
        this.config2.setProperty("key2", "value2");
        this.config2.setProperty("key3", "value3");

        assertEquals("value1", this.composite.getProperty("key1"));
        assertEquals("value2", this.composite.getProperty("key2"));
        assertEquals("value3", this.composite.getProperty("key3"));
        assertNull(this.composite.getProperty("unknown"));
    }

    @Test
    void testGetPropertyWithClass()
    {
        this.config1.setProperty("key1", "value1");
        this.config1.setProperty("key3", "value3");
        this.config2.setProperty("key2", "value2");
        this.config2.setProperty("key3", "value3");

        assertEquals("value1", this.composite.getProperty("key1", String.class));
        assertEquals("value2", this.composite.getProperty("key2", String.class));
        assertEquals("value3", this.composite.getProperty("key3", String.class));
        assertNull(this.composite.getProperty("unknown", String.class));
    }

    @Test
    void testGetPropertyWithDefaultValue()
    {
        this.config1.setProperty("key1", "value1");
        this.config1.setProperty("key3", "value3");
        this.config2.setProperty("key2", "value2");
        this.config2.setProperty("key3", "value3");

        assertEquals("value1", this.composite.getProperty("key1", "default"));
        assertEquals("value2", this.composite.getProperty("key2", "default"));
        assertEquals("value3", this.composite.getProperty("key3", "default"));
        assertEquals("default", this.composite.getProperty("unknown", "default"));
    }

    @Test
    void testGetKeys()
    {
        this.config1.setProperty("key1", "value1");
        this.config2.setProperty("key2", "value2");

        List<String> expected = Arrays.asList("key1", "key2");
        assertEquals(expected, this.composite.getKeys());
    }

    @Test
    void testIsEmpty()
    {
        assertTrue(this.composite.isEmpty());

        this.config2.setProperty("key", "value");
        assertFalse(this.composite.isEmpty());
    }

    @Test
    void testGetPropertiesAndListsWhenEmpty()
    {
        assertTrue(this.composite.getProperty("unknown", Properties.class).isEmpty());
        assertTrue(this.composite.getProperty("unknown", List.class).isEmpty());
    }

    @Test
    void testTypeConversionsWhenDefaultValuesAreNotUsed()
    {
        this.config1.setProperty("key1", "true");

        // Default value is not used since the property exists and is converted to boolean automatically
        assertTrue(this.composite.getProperty("key1", false));
    }
}
