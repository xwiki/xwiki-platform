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
import org.xwiki.model.reference.EntityReference;

/**
 * This is the base exception raised for various reasons by the authorization module.
 * 
 * @version $Id$
 * @since 4.0M2
 */
public class AuthorizationException extends Exception
{
    /** Constant value displayed for null entityReference. */
    static final String NULL_ENTITY = "Main Wiki";

    /** Constant value displayed for null userReference. */
    static final String NULL_USER = "Public";

    /** Serialization identifier. */
    private static final long serialVersionUID = 1L;

    /**
     * @param message Message.
     */
    public AuthorizationException(String message)
    {
        super(message);
    }

    /**
     * @param message Message.
     * @param cause Original cause.
     */
    public AuthorizationException(String message, Throwable cause)
    {
        super(message, cause);
    }

    /**
     * @param userReference The user, for which the query was attempted.
     * @param entityReference The entity, on which the query was attempted.
     */
    public AuthorizationException(DocumentReference userReference, EntityReference entityReference)
    {
        this(userReference, entityReference, null, null);
    }

    /**
     * @param userReference The user, for which the query was attempted.
     * @param entityReference The entity, on which the query was attempted.
     * @param message Message.
     */
    public AuthorizationException(DocumentReference userReference, EntityReference entityReference, String message)
    {
        this(null, userReference, entityReference, message, null);
    }

    /**
     * @param right The right being checked.
     * @param userReference The user, for which the query was attempted.
     * @param entityReference The entity, on which the query was attempted.
     * @param message Message.
     */
    public AuthorizationException(Right right, DocumentReference userReference, EntityReference entityReference,
        String message)
    {
        this(right, userReference, entityReference, message, null);
    }

    /**
     * @param userReference The user, for which the query was attempted.
     * @param entityReference The entity, on which the query was attempted.
     * @param message Message.
     * @param cause Original cause.
     */
    public AuthorizationException(DocumentReference userReference, EntityReference entityReference, String message,
        Throwable cause)
    {
        this(null, userReference, entityReference, message, cause);
    }

    /**
     * @param right The right being checked.
     * @param userReference The user, for which the query was attempted.
     * @param entityReference The entity, on which the query was attempted.
     * @param message Message.
     * @param cause Original cause.
     */
    public AuthorizationException(Right right,
                                  DocumentReference userReference,
                                  EntityReference entityReference,
                                  String message,
                                  Throwable cause)
    {
        super(String.format("%s when checking %s access to [%s] for user [%s]",
                            message,
                            (right == null) ? "" : "[" + right.getName() + "]",
                            (entityReference == null) ? NULL_ENTITY : entityReference,
                            (userReference == null) ? NULL_USER : userReference), cause);
    }

    /**
     * @param entityReference The entity, on which the query was attempted.
     * @param message Message.
     * @param cause Original cause.
     */
    public AuthorizationException(EntityReference entityReference,
                                  String message,
                                  Throwable cause)
    {
        super(String.format("%s when checking access to [%s]",
                            message,
                            (entityReference == null) ? NULL_ENTITY : entityReference), cause);
    }

    /**
     * @param userReference The user, for which the query was attempted.
     * @param entityReference The entity, on which the query was attempted.
     * @param cause Original cause.
     */
    public AuthorizationException(DocumentReference userReference, EntityReference entityReference, Throwable cause)
    {
        this(userReference, entityReference, null, cause);
    }

}
