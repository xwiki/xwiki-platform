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
package org.xwiki.uiextension.internal;

import java.util.List;
import java.util.Optional;

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.uiextension.UIExtension;
import org.xwiki.uiextension.UIExtensionManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultUIExtensionManager}.
 *
 * @version $Id$
 */
@ComponentTest
class DefaultUIExtensionManagerTest
{
    private static final String FAILED_INSTANCES = "Failed to lookup UIExtension instances";

    @InjectMockComponents
    private DefaultUIExtensionManager manager;

    @MockComponent
    @Named("context")
    private Provider<ComponentManager> contextComponentManagerProvider;

    @Mock
    private ComponentManager contextComponentManager;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.ERROR);

    @BeforeEach
    void beforeEach()
    {
        when(this.contextComponentManagerProvider.get()).thenReturn(this.contextComponentManager);
    }

    @Test
    void getWhenSpecificManagerLookupFails() throws Exception
    {
        when(this.contextComponentManager.hasComponent(UIExtensionManager.class, "epId")).thenReturn(true);
        when(this.contextComponentManager.getInstance(UIExtensionManager.class, "epId"))
            .thenThrow(new ComponentLookupException("error!"));

        assertEquals(List.of(), this.manager.get("epId"));

        assertEquals("Failed to initialize lookup a specific UIExtensionManager for the hint [epId]",
            this.logCapture.getMessage(0));
        assertEquals("error!", this.logCapture.getLogEvent(0).getThrowableProxy().getMessage());
    }

    @Test
    void getWhenExtensionsLookupFails() throws Exception
    {
        when(this.contextComponentManager.getInstanceList(UIExtension.class))
            .thenThrow(new ComponentLookupException("error!"));

        assertEquals(List.of(), this.manager.get("epId"));

        assertEquals(FAILED_INSTANCES, this.logCapture.getMessage(0));
        assertEquals("error!", this.logCapture.getLogEvent(0).getThrowableProxy().getMessage());
    }

    @Test
    void getUIExtension() throws Exception
    {
        UIExtension uix = mock(UIExtension.class, "uix1");
        when(uix.getId()).thenReturn("id1");
        when(this.contextComponentManager.getInstanceList(UIExtension.class)).thenReturn(List.of(uix));

        assertEquals(Optional.of(uix), this.manager.getUIExtension("id1"));
        assertEquals(Optional.empty(), this.manager.getUIExtension("unknownId"));
    }

    @Test
    void getUIExtensionWhenLookupFails() throws Exception
    {
        when(this.contextComponentManager.getInstanceList(UIExtension.class))
            .thenThrow(new ComponentLookupException("error!"));

        assertEquals(Optional.empty(), this.manager.getUIExtension("id1"));

        assertEquals(FAILED_INSTANCES, this.logCapture.getMessage(0));
        assertEquals("error!", this.logCapture.getLogEvent(0).getThrowableProxy().getMessage());
    }
}
