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

package com.xpn.xwiki.user.impl.xwiki;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xpn.xwiki.web.Utils;

import org.xwiki.observation.ObservationManager;
import org.xwiki.security.authentication.UserAuthenticatedEvent;
import org.xwiki.stability.Unstable;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;

/**
 * Wraps an {@link org.xwiki.observation.ObservationManager} and a {@link org.xwiki.user.UserReferenceResolver} to
 * notify about user who are authenticated through {@link com.xpn.xwiki.user.impl.xwiki.MyFormAuthenticator} and {@link
 * com.xpn.xwiki.user.impl.xwiki.MyBasicAuthenticator}.
 *
 * @version $Id$
 * @since 13.3RC1
 */
@Unstable
public class UserAuthenticatedManager
{
    /**
     * The unique instance of this class.
     */
    public static final UserAuthenticatedManager INSTANCE = new UserAuthenticatedManager();

    private static final Logger LOGGER = LoggerFactory.getLogger(UserAuthenticatedManager.class);

    @Inject
    private ObservationManager observationManager;

    @Inject
    private UserReferenceResolver<String> userReferenceResolver;

    /**
     * Nothing to pass to this class.
     */
    public UserAuthenticatedManager()
    {
        // Voluntarily empty. We want to have a single instance of this class.
    }

    /**
     * @return {@link org.xwiki.observation.ObservationManager}
     */
    private ObservationManager getObservationManager()
    {
        if (this.observationManager == null) {
            this.observationManager = Utils.getComponent(ObservationManager.class);
        }
        return this.observationManager;
    }

    /**
     * @return {@link org.xwiki.user.UserReferenceResolver} to resolve a string into a proper {@link
     *     org.xwiki.user.UserReference}
     */
    private UserReferenceResolver<String> getUserReferenceResolver()
    {
        if (this.userReferenceResolver == null) {
            this.userReferenceResolver = Utils.getComponent(UserReferenceResolver.class);
        }
        return this.userReferenceResolver;
    }

    /**
     * @param userReference {@link org.xwiki.user.UserReference} of user who triggers a UserAuthenticatedEvent
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
        UserReference userReference = this.getUserReferenceResolver().resolve(stringUserReference);
        this.notify(new UserAuthenticatedEvent(userReference));
    }

    /**
     * @param event {@link org.xwiki.security.authentication.UserAuthenticatedEvent} that has already been created
     */
    public void notify(UserAuthenticatedEvent event)
    {
        LOGGER.debug("User authenticated for [{}]", event.getUserReference());
        this.getObservationManager().notify(event, null);
    }
}

