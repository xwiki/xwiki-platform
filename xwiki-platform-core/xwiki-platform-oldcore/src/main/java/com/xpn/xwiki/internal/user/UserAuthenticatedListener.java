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
package com.xpn.xwiki.internal.user;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.container.Container;
import org.xwiki.container.servlet.ServletRequest;
import org.xwiki.observation.event.AbstractLocalEventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.security.authentication.UserAuthenticatedEvent;

/**
 * Listener for {@link UserAuthenticatedEvent} events.
 *
 * @version $Id$
 * @since 16.10.19
 * @since 18.7.0RC1
 * @since 18.4.4
 * @since 17.10.12
 */
@Component
@Named(UserAuthenticatedListener.NAME)
@Singleton
public class UserAuthenticatedListener extends AbstractLocalEventListener
{
    /**
     * Name of the listener.
     */
    public static final String NAME = "com.xpn.xwiki.internal.user.UserAuthenticatedListener";

    @Inject
    private Container container;

    /**
     * Default constructor.
     */
    public UserAuthenticatedListener()
    {
        super(UserAuthenticatedListener.NAME, new UserAuthenticatedEvent());
    }

    @Override
    public void processLocalEvent(Event event, Object source, Object data)
    {
        if (this.container.getRequest() instanceof ServletRequest servletResuest) {
            // Since a new user has been authenticated, we change the session ID to prevent session fixation attacks.
            // We assume there is a very high chance that the authenticator stored metadata related to the newly
            // authenticated user that should not be accessed by anyone else. If it's not the case, changing the session
            // ID should not cause problems, it's just not strictly required.
            // We also assume all authenticator properly send UserAuthenticatedEvent events. If it's not the case, it's
            // probably a bug to fix, and the authenticator can always add its own session fixation protection.
            servletResuest.getRequest().changeSessionId();
        }
    }
}
