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
package org.xwiki.security.authorization;

import org.xwiki.model.reference.DocumentReference;

/**
 * AuthorizationContext encapsulates the authorization information that is used for granting access to various
 * resources.  The athorization context cannot be modified directly, so it is safe to make it available to unprivileged
 * code.
 *
 * @version $Id$
 * @since 4.3M1
 */
public interface AuthorizationContext
{
    /** The execution context key where the authorization context must be stored. */
    String EXECUTION_CONTEXT_KEY = "authorization_context";

    /** @return The effective user name that is executing the request. */
    DocumentReference getEffectiveUser();

    /** @return The user name of the content author.  {@code null} if there is no known content author.  */
    DocumentReference getContentAuthor();

}
