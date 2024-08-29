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
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import org.xwiki.component.annotation.Component;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.event.Event;
import org.xwiki.security.authentication.UserAuthenticatedEvent;
import org.xwiki.security.authentication.UserUnauthenticatedEvent;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;

import com.xpn.xwiki.XWikiContext;

/**
 * This notifier helps dealing with events triggered when a user is authenticated through XWiki Oldcore's
 * authenticators. It wraps an {@code ObservationManager} and a {@code UserReferenceResolver} to
 * notify about user who are authenticated through {@code MyFormAuthenticator} and
 * {@code MyBasicAuthenticator}.
 *
 * @version $Id$
 * @since 13.3RC1
 */
@Component(roles = UserAuthenticationEventNotifier.class)
@Singleton
public class UserAuthenticationEventNotifier
{

    @Inject
    private Logger logger;

    @Inject
    private ObservationManager observationManager;

    @Inject
    private UserReferenceResolver<String> userReferenceResolver;

    @Inject
    private Provider<XWikiContext> contextProvider;

    /**
     * Resolve a string as a {@code UserReference} and notify a {@code UserAuthenticatedEvent} created with that user
     * reference.
     *
     * @param stringUserReference string form of the reference of user that will be resolved as a {@code
     * UserReference} and passed to the {@code UserAuthenticatedEvent} instance creation
     */
    public void notifyUserAuthenticated(String stringUserReference)
    {
        if (!StringUtils.isBlank(stringUserReference)) {
            UserReference userReference = this.userReferenceResolver.resolve(stringUserReference);
            if (this.logger.isDebugEnabled()) {
                this.logger.debug("User [{}] authenticated", userReference);
            }
            this.notify(new UserAuthenticatedEvent(userReference));
        }
    }

    /**
     * Notify that the given user is now logged out.
     *
     * @param stringUserReference the user for whom to fire a {@link UserUnauthenticatedEvent}.
     */
    public void notifyUserUnauthenticated(String stringUserReference)
    {
        if (!StringUtils.isBlank(stringUserReference)) {
            UserReference userReference = this.userReferenceResolver.resolve(stringUserReference);
            if (this.logger.isDebugEnabled()) {
                this.logger.debug("User [{}] unauthenticated", userReference);
            }
            this.notify(new UserUnauthenticatedEvent(userReference));
        }
    }

    /**
     * Notify a {@link UserAuthenticatedEvent} that has already been created.
     *
     * @param event {@code UserAuthenticatedEvent}
     */
    private void notify(Event event)
    {
        this.observationManager.notify(event, null, this.contextProvider.get());
    }
}
