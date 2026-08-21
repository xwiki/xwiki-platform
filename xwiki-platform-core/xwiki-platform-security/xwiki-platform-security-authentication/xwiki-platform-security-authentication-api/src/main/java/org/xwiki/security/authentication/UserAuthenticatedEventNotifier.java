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
package org.xwiki.security.authentication;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.user.UserReference;

/**
 * Helper to notify about a user who just authenticated.
 * 
 * @version $Id$
 * @since 16.10.19
 * @since 18.7.0RC1
 * @since 18.4.4
 * @since 17.10.12
 */
@Role
public interface UserAuthenticatedEventNotifier
{
    /**
     * Send an event indicating that a user has been authenticated.
     * <p>
     * Resolve a string as a {@code UserReference} and notify a {@code UserAuthenticatedEvent} created with that user
     * reference.
     * 
     * @param userReference string form of the reference of user that will be resolved as a {@code UserReference} and
     *            passed to the {@code UserAuthenticatedEvent} instance creation
     */
    void notify(String userReference);

    /**
     * Send an event indicating that a user has been authenticated.
     * <p>
     * Resolve a {@code DocumentReference} as a {@code UserReference} and notify a {@code UserAuthenticatedEvent}
     * created with that user reference.
     * 
     * @param userReference string form of the reference of user that will be resolved as a {@code UserReference} and
     *            passed to the {@code UserAuthenticatedEvent} instance creation
     */
    void notify(DocumentReference userReference);

    /**
     * Send an event indicating that a user has been authenticated.
     * 
     * @param userReference the reference of the user to pass to the {@code UserAuthenticatedEvent}
     */
    void notify(UserReference userReference);
}
