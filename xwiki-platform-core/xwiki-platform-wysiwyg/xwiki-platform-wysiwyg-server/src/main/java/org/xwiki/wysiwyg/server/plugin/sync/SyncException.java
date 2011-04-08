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
package org.xwiki.wysiwyg.server.plugin.sync;

/**
 * A synchronization exception.
 * 
 * @version $Id$
 */
public class SyncException extends Exception
{
    /**
     * Field required by all {@link java.io.Serializable} classes.
     */
    private static final long serialVersionUID = 4456364417718269072L;

    /**
     * Creates a new synchronization exception.
     */
    public SyncException()
    {
        super();
    }

    /**
     * Creates a new synchronization exception with the specified message.
     * 
     * @param message the exception message
     */
    public SyncException(String message)
    {
        super(message);
    }

    /**
     * Creates a new synchronization exception with the specified cause.
     * 
     * @param cause the exception cause
     */
    public SyncException(Throwable cause)
    {
        super(cause);
    }

    /**
     * Creates a new synchronization exception with the given message and the specified cause.
     * 
     * @param message the exception message
     * @param cause the exception cause
     */
    public SyncException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
