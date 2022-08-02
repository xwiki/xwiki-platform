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
package org.xwiki.csrf;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.xwiki.bridge.event.ActionExecutingEvent;
import org.xwiki.csrf.internal.CSRFTokenInvalidator;
import org.xwiki.observation.event.Event;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

/**
 * Tests for the {@link CSRFTokenInvalidator} component.
 * 
 * @version $Id$
 * @since 4.0M1
 */
@ComponentTest
public class CSRFTokenInvalidatorTest
{
    /** Tested component. */
    @InjectMockComponents
    private CSRFTokenInvalidator invalidator;

    @MockComponent
    private CSRFToken mockCSRFTokenManager;

    /**
     * Test that the list of monitored events contains an ActionExecutingEvent for the /logout/ action.
     */
    @Test
    public void testEvents()
    {
        List<Event> events = this.invalidator.getEvents();
        assertTrue(events.contains(new ActionExecutingEvent("logout")),
            "Invalidator doesn't listen to /logout/ events");
    }

    /**
     * Tests that the token will get invalidated when a logout event occurs.
     * 
     * @throws Exception
     */
    @Test
    public void testInvalidationOnLogout()
    {
        this.invalidator.onEvent(new ActionExecutingEvent("logout"), null, null);
        verify(mockCSRFTokenManager, atLeastOnce()).clearToken();
    }
}
