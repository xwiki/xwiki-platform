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

import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.DocumentReference;

/**
 * Exception raised by the AuthorizationManager when denying access.
 *
 * @version $Id$
 * @since 4.0M2
 */
public class AccessDeniedException extends AuthorizationException
{
    /** Serialization identifier. */
    private static final long serialVersionUID = 1L;

    /**
     * @param userReference The user, for which the query was attempted.
     * @param entityReference The entity, on which the query was attempted.
     */
    public AccessDeniedException(DocumentReference userReference, EntityReference entityReference)
    {
        this(userReference, entityReference, null);
    }

    /**
     * @param right The right currently being checked.
     * @param userReference The user, for which the query was attempted.
     * @param entityReference The entity, on which the query was attempted.
     */
    public AccessDeniedException(Right right, DocumentReference userReference, EntityReference entityReference)
    {
        this(right, userReference, entityReference, null);
    }

    /**
     * @param userReference The user, for which the query was attempted.
     * @param entityReference The entity, on which the query was attempted.
     * @param t a Throwable providing details about the underlying cause.
     */
    public AccessDeniedException(DocumentReference userReference, EntityReference entityReference, Throwable t)
    {
        this(null, userReference, entityReference, t);
    }

    /**
     * @param right The right currently being checked.
     * @param userReference The user, for which the query was attempted.
     * @param entityReference The entity, on which the query was attempted.
     * @param t a Throwable providing details about the underlying cause.
     */
    public AccessDeniedException(Right right, DocumentReference userReference, EntityReference entityReference,
        Throwable t)
    {
        super(right, userReference, entityReference, "Access denied", t);
    }
}
