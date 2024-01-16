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
package org.xwiki.notifications.filters.internal.listener;

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.filters.internal.FilterPreferencesModelBridge;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static ch.qos.logback.classic.Level.WARN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test of {@link DeletedWikiEventListener}.
 *
 * @version $Id$
 * @since 14.5
 * @since 14.4.1
 * @since 13.10.7
 */
@ComponentTest
class DeletedWikiEventListenerTest
{
    @InjectMockComponents
    private DeletedWikiEventListener listener;

    @Named("cached")
    @MockComponent
    private Provider<FilterPreferencesModelBridge> modelBridgeProvider;

    @Mock
    private FilterPreferencesModelBridge filterPreferencesModelBridge;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @BeforeEach
    void setUp()
    {
        when(this.modelBridgeProvider.get()).thenReturn(this.filterPreferencesModelBridge);
    }

    @Test
    void onEvent() throws Exception
    {
        this.listener.onEvent(null, "wikiid", null);
        verify(this.filterPreferencesModelBridge).deleteFilterPreferences(new WikiReference("wikiid"));
    }

    @Test
    void onEventException() throws Exception
    {
        doThrow(NotificationException.class).when(this.filterPreferencesModelBridge)
            .deleteFilterPreferences(new WikiReference("wikiid"));
        this.listener.onEvent(null, "wikiid", null);
        assertEquals(1, this.logCapture.size());
        assertEquals("Failed to delete notification preferences for wiki [wikiid]. Cause: [NotificationException: ].",
            this.logCapture.getMessage(0));
        assertEquals(WARN, this.logCapture.getLogEvent(0).getLevel());
    }
}
