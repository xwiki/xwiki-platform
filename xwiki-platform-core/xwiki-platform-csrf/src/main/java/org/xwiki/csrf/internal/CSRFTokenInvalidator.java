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
package org.xwiki.csrf.internal;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.bridge.event.ActionExecutingEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.csrf.CSRFToken;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;

/**
 * {@link EventListener} which will invalidate the CSRF token for the current user whenever a {@code /logout/} action
 * occurs.
 * 
 * @version $Id$
 * @since 4.0M1
 */
// FIXME This is currently disabled because at the time this event is sent, the user has already been removed from the
// context, so we're messing things up for guests.
@Component(staticRegistration = false)
@Named("csrf-token-invalidator")
@Singleton
public class CSRFTokenInvalidator implements EventListener
{
    /** CSRF Token manager. */
    @Inject
    private CSRFToken tokenManager;

    @Override
    public List<Event> getEvents()
    {
        return Collections.singletonList(new ActionExecutingEvent("logout"));
    }

    @Override
    public String getName()
    {
        return "csrf-token-invalidator";
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        this.tokenManager.clearToken();
    }
}
