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

import java.awt.Color;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.configuration.ConversionException;
import org.xwiki.properties.ConverterManager;
import org.xwiki.test.annotation.AllComponents;
import org.xwiki.test.junit5.mockito.ComponentTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link CommonsConfigurationSource}.
 * 
 * @version $Id$
 * @since 2.0M1
 */
@ComponentTest
@AllComponents
class CommonsConfigurationSourceTest
{
    private Configuration configuration;

    private CommonsConfigurationSource source;

    @BeforeEach
    void setUp(ComponentManager componentManager) throws Exception
    {
        this.source = new CommonsConfigurationSource();
        ConverterManager converterManager = componentManager.getInstance(ConverterManager.class);
        ReflectionUtils.setFieldValue(this.source, "converterManager", converterManager);
        this.configuration = new BaseConfiguration();
        this.source.setConfiguration(this.configuration);
    }

    @Test
    void defaultValue()
    {
        this.configuration.setProperty("string", "value");

        assertEquals("default", this.source.getProperty("unknown", "default"));
        assertEquals("value", this.source.getProperty("string", "default"));
        assertEquals(null, this.source.getProperty("unknown", (String) null));
    }

    @Test
    void stringProperty()
    {
        this.configuration.setProperty("string", "value");

        assertEquals("value", this.source.getProperty("string"));
        assertEquals("value", this.source.getProperty("string", String.class));

        assertNull(this.source.getProperty("unknown"));
        assertNull(this.source.getProperty("unknown", String.class));
    }

    @Test
    void stringPropertyWhenSeveral()
    {
        this.configuration.addProperty("string", "value1");
        this.configuration.addProperty("string", "value2");

        assertEquals(List.of("value1", "value2"), this.source.getProperty("string"));
        assertEquals("value1", this.source.getProperty("string", String.class));
    }

    @Test
    void stringPropertyWhenConversionError()
    {
        this.configuration.setProperty("string", "value");

        // Try to retrieve a String property as a Boolean
        Exception exception = assertThrows(ConversionException.class,
            () -> this.source.getProperty("string", Boolean.class));
        assertEquals("Key [string] is not compatible with type [java.lang.Boolean]", exception.getMessage());
    }

    @Test
    void booleanPropertyWhenConversionError()
    {
        this.configuration.setProperty("property", "");

        // Try to retrieve a String property as a Color
        Exception exception = assertThrows(ConversionException.class,
            () -> this.source.getProperty("property", Color.class));
        assertEquals("Key [property] is not compatible with type [java.awt.Color]", exception.getMessage());
    }

    @Test
    void booleanProperty()
    {
        // Test boolean value
        this.configuration.setProperty("boolean", true);

        assertEquals(true, this.source.getProperty("boolean"));
        assertEquals(true, this.source.getProperty("boolean", Boolean.class));
        assertEquals(true, this.source.getProperty("unknown", true));
        assertEquals(false, this.source.getProperty("unknown", false));
    }

    @Test
    void unknownBooleanProperty()
    {
        assertNull(this.source.getProperty("unknown", Boolean.class));
    }

    @Test
    void listProperty()
    {
        this.configuration.setProperty("list", "value1");
        this.configuration.addProperty("list", "value2");
        List<String> expected = Arrays.asList("value1", "value2");

        assertEquals(expected, this.source.getProperty("list"));
        assertEquals(expected, this.source.getProperty("list", List.class));

        assertTrue(this.source.getProperty("unknown", List.class).isEmpty());
        assertEquals(Arrays.asList("toto"), this.source.getProperty("unknown", Arrays.asList("toto")));
    }

    @Test
    void testListPropertyWhenArrayList()
    {
        this.configuration.setProperty("list", "value");
        List<String> expected = Arrays.asList("value");

        assertEquals(expected, this.source.getProperty("list", Arrays.asList("default")));
    }

    @Test
    void propertiesProperty()
    {
        this.configuration.setProperty("properties", "key1=value1");
        this.configuration.addProperty("properties", "key2=value2");
        List<String> expectedList = Arrays.asList("key1=value1", "key2=value2");
        Properties expectedProperties = new Properties();
        expectedProperties.put("key1", "value1");
        expectedProperties.put("key2", "value2");

        assertEquals(expectedList, this.source.getProperty("properties"));
        assertEquals(expectedProperties, this.source.getProperty("properties", Properties.class));

        assertTrue(this.source.getProperty("unknown", Properties.class).isEmpty());
    }

    @Test
    void isEmpty()
    {
        assertTrue(this.configuration.isEmpty());

        this.configuration.addProperty("properties", "key2=value2");

        assertFalse(this.configuration.isEmpty());
    }
}
