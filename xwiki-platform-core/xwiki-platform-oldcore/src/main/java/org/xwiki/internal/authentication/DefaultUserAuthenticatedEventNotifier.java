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

package org.xwiki.internal.authentication;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.ObservationManager;
import org.xwiki.security.authentication.UserAuthenticatedEvent;
import org.xwiki.security.authentication.UserAuthenticatedEventNotifier;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;

/**
 * This notifier helps dealing with events triggered when a user is authenticated through XWiki Oldcore's
 * authenticators. It wraps an {@code ObservationManager} and a {@code UserReferenceResolver} to notify about user who
 * are authenticated through {@code MyFormAuthenticator} and {@code MyBasicAuthenticator}.
 *
 * @version $Id$
 * @since 16.10.19
 * @since 18.7.0RC1
 * @since 18.4.4
 * @since 17.10.12
 */
@Component
@Singleton
public class DefaultUserAuthenticatedEventNotifier implements UserAuthenticatedEventNotifier
{
    @Inject
    private Logger logger;

    @Inject
    private ObservationManager observationManager;

    @Inject
    private UserReferenceResolver<String> stringResolver;

    @Inject
    @Named("document")
    private UserReferenceResolver<DocumentReference> documentResolver;

    @Override
    public void notify(String userReference)
    {
        notify(this.stringResolver.resolve(userReference));
    }

    @Override
    public void notify(DocumentReference userReference)
    {
        notify(this.documentResolver.resolve(userReference));
    }

    @Override
    public void notify(UserReference userReference)
    {
        notify(new UserAuthenticatedEvent(userReference));
    }

    /**
     * Notify a {@link UserAuthenticatedEvent} that has already been created.
     *
     * @param event {@code UserAuthenticatedEvent}
     */
    private void notify(UserAuthenticatedEvent event)
    {
        this.logger.debug("User authenticated for [{}]", event.getUserReference());

        this.observationManager.notify(event, null);
    }
}
