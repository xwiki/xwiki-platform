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
package org.xwiki.workspace;

/**
 * Exceptions thrown by the Workspace API.
 * 
 * @version $Id$
 */
public class WorkspaceException extends Exception
{
    /** Serializable UID. */
    private static final long serialVersionUID = -7490442041971008340L;

    /**
     * @param message the exception's message
     */
    public WorkspaceException(String message)
    {
        this(message, null);
    }

    /**
     * @param message the exception's message
     * @param exception the cause of this exception
     */
    public WorkspaceException(String message, Throwable exception)
    {
        super(message, exception);
    }
}
