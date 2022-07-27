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
package org.xwiki.container.servlet.internal;

import java.util.List;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;

import org.junit.jupiter.api.Test;
import org.xwiki.component.manager.ComponentLifecycleException;
import org.xwiki.container.servlet.events.SessionCreatedEvent;
import org.xwiki.container.servlet.events.SessionDestroyedEvent;
import org.xwiki.observation.ObservationManager;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link HttpSessionManager}.
 *
 * @version $Id$
 * @since 14.5
 */
@ComponentTest
class HttpSessionManagerTest
{
    @InjectMockComponents
    private HttpSessionManager httpSessionManager;

    @MockComponent
    private ObservationManager observationManager;

    @Test
    void sessionCreatedAndDestroyed()
    {
        assertTrue(this.httpSessionManager.getSessionList().isEmpty());
        HttpSessionEvent httpSessionEvent = mock(HttpSessionEvent.class);
        HttpSession httpSession = mock(HttpSession.class);
        when(httpSessionEvent.getSession()).thenReturn(httpSession);

        this.httpSessionManager.sessionCreated(httpSessionEvent);
        assertEquals(1,  this.httpSessionManager.getSessionList().size());
        assertEquals(httpSession, this.httpSessionManager.getSessionList().get(0));
        verify(this.observationManager).notify(any(SessionCreatedEvent.class), eq(httpSession), isNull());

        this.httpSessionManager.sessionDestroyed(httpSessionEvent);
        assertTrue(this.httpSessionManager.getSessionList().isEmpty());
        verify(this.observationManager).notify(any(SessionDestroyedEvent.class), eq(httpSession), isNull());
    }


    @Test
    void dispose() throws ComponentLifecycleException
    {
        assertTrue(this.httpSessionManager.getSessionList().isEmpty());
        HttpSessionEvent httpSessionEvent = mock(HttpSessionEvent.class);
        HttpSession httpSession1 = mock(HttpSession.class, "session1");
        HttpSession httpSession2 = mock(HttpSession.class, "session2");
        HttpSession httpSession3 = mock(HttpSession.class, "session3");
        when(httpSessionEvent.getSession())
            .thenReturn(httpSession1)
            .thenReturn(httpSession2)
            .thenReturn(httpSession3);

        this.httpSessionManager.sessionCreated(httpSessionEvent);
        this.httpSessionManager.sessionCreated(httpSessionEvent);
        this.httpSessionManager.sessionCreated(httpSessionEvent);

        assertEquals(List.of(httpSession1, httpSession2, httpSession3), this.httpSessionManager.getSessionList());

        when(httpSession1.isNew()).thenReturn(false);
        when(httpSession2.isNew()).thenReturn(true);
        when(httpSession3.isNew()).thenReturn(false);

        this.httpSessionManager.dispose();
        verify(httpSession1).invalidate();
        verify(httpSession2, never()).invalidate();
        verify(httpSession3).invalidate();
    }
}