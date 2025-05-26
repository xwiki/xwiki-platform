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
import java.util.Properties;

import jakarta.inject.Named;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.environment.Environment;
import org.xwiki.properties.ConverterManager;
import org.xwiki.test.LogLevel;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link XWikiPropertiesConfigurationSource}.
 *
 * @version $Id$
 * @since 3.0M1
 */
@ComponentTest
@ComponentList(XWikiPropertiesConfigurationSource.class)
class XWikiPropertiesConfigurationSourceTest
{
    @MockComponent
    private Environment environment;

    @MockComponent
    @Named("system")
    private ConfigurationSource configurationSource;

    @MockComponent
    private ConverterManager converterManager;

    @InjectComponentManager
    private MockitoComponentManager componentManager;

    @RegisterExtension
    LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.DEBUG);

    private ConfigurationSource getConfiguration() throws ComponentLookupException
    {
        // We cannot use @InjectMockComponents because some test need to customize the input of the initialization
        return this.componentManager.getInstance(ConfigurationSource.class, "xwikiproperties");
    }

    @Test
    void testInitializeWhenNoPropertiesFile() throws ComponentLookupException
    {
        // Verifies that we can get a property from the source (i.e. that it's correctly initialized)
        getConfiguration().getProperty("key");

        ILoggingEvent logEvent = this.logCapture.getLogEvent(0);
        assertSame(Level.DEBUG, logEvent.getLevel());
        assertEquals("No configuration file [{}] found. Using default configuration values.", logEvent.getMessage());
        assertEquals("/WEB-INF/xwiki.properties", logEvent.getArgumentArray()[0]);
    }

    @Test
    void testListParsing() throws ComponentLookupException
    {
        when(this.environment.getResource("/WEB-INF/xwiki.properties"))
            .thenReturn(getClass().getResource("/xwiki.properties"));

        ConfigurationSource configuration = getConfiguration();

        assertEquals(Arrays.asList("value1", "value2"), configuration.getProperty("listProperty"));

        Properties properties = configuration.getProperty("propertiesProperty", Properties.class);
        assertEquals("value1", properties.get("prop1"));

        this.logCapture.ignoreAllMessages();
    }
}
