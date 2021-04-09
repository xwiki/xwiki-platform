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

import org.slf4j.Logger;

import org.xwiki.component.annotation.Component;
import org.xwiki.observation.ObservationManager;
import org.xwiki.security.authentication.UserAuthenticatedEvent;
import org.xwiki.stability.Unstable;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;

/**
 * Wraps an {@code ObservationManager} and a {@code UserReferenceResolver} to
 * notify about user who are authenticated through {@code MyFormAuthenticator} and
 * {@code MyBasicAuthenticator}.
 *
 * @version $Id$
 * @since 13.3RC1
 */
@Component(roles = UserAuthenticatedManager.class)
@Singleton
@Unstable
public class UserAuthenticatedManager
{
    /**
     * The unique instance of this class.
     */
    public static final UserAuthenticatedManager INSTANCE = new UserAuthenticatedManager();

    @Inject
    private Logger logger;

    @Inject
    @Named("observer")
    private ObservationManager observationManager;

    @Inject
    @Named("referenceresolver")
    private UserReferenceResolver<String> userReferenceResolver;

    /**
     * Nothing to pass to this class.
     */
    public UserAuthenticatedManager()
    {
        // Voluntarily empty. We want to have a single instance of this class.
    }

    /**
     * @param userReference {@code UserReference} of user who triggers a UserAuthenticatedEvent
     */
    public void notify(UserReference userReference)
    {
        this.notify(new UserAuthenticatedEvent(userReference));
    }

    /**
     * @param stringUserReference string form of the reference of user who triggers a UserAuthenticatedEvent
     */
    public void notify(String stringUserReference)
    {
        UserReference userReference = this.userReferenceResolver.resolve(stringUserReference);
        this.notify(new UserAuthenticatedEvent(userReference));
    }

    /**
     * @param event {@code UserAuthenticatedEvent} that has already been created
     */
    public void notify(UserAuthenticatedEvent event)
    {
        this.logger.debug("User authenticated for [{}]", event.getUserReference());
        this.observationManager.notify(event, null);
    }
}

