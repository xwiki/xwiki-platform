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
package com.xpn.xwiki.internal.skin;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.skin.SkinActionURLParametersProvider;
import org.xwiki.test.LogLevel;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.mockito.MockitoComponentManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link SkinActionURLParameters}.
 *
 * @version $Id$
 */
@ComponentTest
class SkinActionURLParametersTest
{
    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @InjectComponentManager
    private MockitoComponentManager componentManager;

    @InjectMockComponents
    private SkinActionURLParameters skinActionURLParameters;

    @BeforeComponent
    void beforeComponent() throws Exception
    {
        this.componentManager.registerComponent(ComponentManager.class, "context", this.componentManager);
    }

    @Test
    void addParameters() throws Exception
    {
        SkinActionURLParametersProvider second =
            this.componentManager.registerMockComponent(SkinActionURLParametersProvider.class, "second");
        when(second.getParameters()).thenReturn(Map.of("secondParameter", "secondValue"));
        SkinActionURLParametersProvider first =
            this.componentManager.registerMockComponent(SkinActionURLParametersProvider.class, "first");
        when(first.getParameters()).thenReturn(Map.of("firstParameter", "firstValue"));
        SkinActionURLParametersProvider empty =
            this.componentManager.registerMockComponent(SkinActionURLParametersProvider.class, "empty");
        when(empty.getParameters()).thenReturn(null);

        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("cache-version", "42");
        this.skinActionURLParameters.addParameters(parameters);

        // The parameters of the providers are added in the order of the provider hints.
        assertEquals(List.of("cache-version", "firstParameter", "secondParameter"),
            List.copyOf(parameters.keySet()));
        assertEquals(List.of("42", "firstValue", "secondValue"), List.copyOf(parameters.values()));
    }

    @Test
    void addParametersWhenProviderFails() throws Exception
    {
        SkinActionURLParametersProvider failing =
            this.componentManager.registerMockComponent(SkinActionURLParametersProvider.class, "failing");
        when(failing.getParameters()).thenThrow(new IllegalStateException("error"));
        SkinActionURLParametersProvider working =
            this.componentManager.registerMockComponent(SkinActionURLParametersProvider.class, "working");
        when(working.getParameters()).thenReturn(Map.of("parameter", "value"));

        Map<String, Object> parameters = new LinkedHashMap<>();
        this.skinActionURLParameters.addParameters(parameters);

        assertEquals(Map.of("parameter", "value"), parameters);
        assertEquals("Failed to get the skin action URL parameters from provider [failing]. "
            + "Root cause: [IllegalStateException: error]", this.logCapture.getMessage(0));
    }

    @Test
    void addParametersWithoutProvider()
    {
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("cache-version", "42");
        this.skinActionURLParameters.addParameters(parameters);

        assertEquals(Map.of("cache-version", "42"), parameters);
    }
}
