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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.xwiki.configuration.ConfigurationSaveException;
import org.xwiki.environment.Environment;
import org.xwiki.properties.ConverterManager;
import org.xwiki.test.TestEnvironment;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Validate {@link PermanentConfigurationSource}.
 * 
 * @version $Id$
 */
@ComponentTest
@ComponentList(TestEnvironment.class)
class PermanentConfigurationSourceTest
{
    @Inject
    private Environment environment;

    @InjectMockComponents
    private PermanentConfigurationSource configuration;

    @MockComponent
    private ConverterManager converter;

    private void assertEqualProperties(Map<String, Object> expect) throws FileNotFoundException, IOException
    {
        Properties properties = new Properties();
        try (InputStream stream =
            new FileInputStream(new File(environment.getPermanentDirectory(), "configuration.properties"))) {
            properties.load(stream);
        }

        assertEquals(expect, properties);
    }

    @Test
    void initialize() throws FileNotFoundException, IOException
    {
        File file = new File(this.environment.getPermanentDirectory(), "configuration.properties");

        // Make sure the file was created
        assertTrue(file.exists());

        assertEqualProperties(Map.of());
    }

    @Test
    void setProperty() throws ConfigurationSaveException, FileNotFoundException, IOException
    {
        this.configuration.setProperty("prop1", "value1");

        assertEqualProperties(Map.of("prop1", "value1"));

        this.configuration.setProperty("prop2", "value2");

        assertEqualProperties(Map.of("prop1", "value1", "prop2", "value2"));

        this.configuration.setProperty("prop2", null);

        assertEqualProperties(Map.of("prop1", "value1"));

        when(this.converter.convert(String.class, this)).thenReturn("OK");

        this.configuration.setProperty("converted", this);

        assertEqualProperties(Map.of("prop1", "value1", "converted", "OK"));
    }

    @Test
    void setProperties() throws ConfigurationSaveException, FileNotFoundException, IOException
    {
        when(this.converter.convert(String.class, this)).thenReturn("OK");

        this.configuration.setProperties(Map.of("prop1", "value1", "converted", this));

        assertEqualProperties(Map.of("prop1", "value1", "converted", "OK"));

        this.configuration.setProperties(Map.of("prop2", "value2"));

        assertEqualProperties(Map.of("prop2", "value2"));
    }
}
