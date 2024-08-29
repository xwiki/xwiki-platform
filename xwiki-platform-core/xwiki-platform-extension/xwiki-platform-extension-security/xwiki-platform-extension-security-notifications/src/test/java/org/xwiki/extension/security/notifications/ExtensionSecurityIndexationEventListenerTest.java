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
package org.xwiki.extension.security.notifications;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.event.Event;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Test of {@link ExtensionSecurityIndexationEventListener}.
 *
 * @version $Id$
 * @since 15.5
 */
@ComponentTest
class ExtensionSecurityIndexationEventListenerTest
{
    @InjectMockComponents
    private ExtensionSecurityIndexationEventListener listener;

    @MockComponent
    private ObservationManager observationManager;

    @Test
    void processLocalEventData0()
    {
        this.listener.processLocalEvent(mock(Event.class), "source", 0L);
        verifyNoInteractions(this.observationManager);
    }

    @Test
    void processLocalEventData1()
    {
        this.listener.processLocalEvent(mock(Event.class), "source", 1L);
        verify(this.observationManager).notify(
            new NewExtensionSecurityVulnerabilityTargetableEvent(Set.of("xwiki:XWiki.XWikiAdminGroup")),
            "org.xwiki.platform:xwiki-platform-extension-security-notifications", "1");
    }
}
