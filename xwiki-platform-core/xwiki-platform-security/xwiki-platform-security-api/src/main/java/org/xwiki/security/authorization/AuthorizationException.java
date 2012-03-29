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

import java.util.Formatter;

/**
 * This is the base exception raised for various reasons by the authorization module.
 * 
 * @version $Id$
 * @since 4.0M2
 */
public class AuthorizationException extends Exception
{
    /** Serialization identifier. */
    private static final long serialVersionUID = 1L;

    /**
     * @param message Message.
     * @param cause Original cause.
     * @see java.lang.Exception
     */
    public AuthorizationException(String message, Throwable cause)
    {
        super(message, cause);
    }

    /**
     * @param userReference The user, for which the query was attempted.
     * @param entityReference The entity, on which the query was attempted.
     * @see java.lang.Exception
     */
    public AuthorizationException(DocumentReference userReference, EntityReference entityReference)
    {
        this(userReference, entityReference, null, null);
    }

    /**
     * @param userReference The user, for which the query was attempted.
     * @param entityReference The entity, on which the query was attempted.
     * @param message Message.
     * @see java.lang.Exception
     */
    public AuthorizationException(DocumentReference userReference, EntityReference entityReference, String message)
    {
        this(userReference, entityReference, message, null);
    }
    
    /**
     * @param userReference The user, for which the query was attempted.
     * @param entityReference The entity, on which the query was attempted.
     * @param message Message.
     * @param cause Original cause.
     * @see java.lang.Exception
     */
    public AuthorizationException(DocumentReference userReference,
                                  EntityReference entityReference,
                                  String message,
                                  Throwable cause)
    {
        super(new Formatter().format("%s when checking access to %s for user %s",
                                     message, 
                                     userReference,
                                     entityReference).toString(), cause);
    }

    /**
     * @param entityReference The entity, on which the query was attempted.
     * @param message Message.
     * @param cause Original cause.
     * @see java.lang.Exception
     */
    public AuthorizationException(EntityReference entityReference,
                                  String message,
                                  Throwable cause)
    {
        super(new Formatter().format("%s when checking access to %s",
                                     message, 
                                     entityReference).toString(), cause);
    }

    /**
     * @param userReference The user, for which the query was attempted.
     * @param entityReference The entity, on which the query was attempted.
     * @param cause Original cause.
     * @see java.lang.Exception
     */
    public AuthorizationException(DocumentReference userReference, EntityReference entityReference, Throwable cause)
    {
        this(userReference, entityReference, null, cause);
    }

}
