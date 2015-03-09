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
package org.xwiki.watchlist.internal.api;

/**
 * Default exception thrown by the watchlist module.
 * 
 * @version $Id$
 */
public class WatchListException extends Exception
{
    /**
     * Class ID used in serialization.
     */
    private static final long serialVersionUID = -7465297713674629352L;

    /**
     * @param message the exception message
     */
    public WatchListException(String message)
    {
        super(message);
    }

    /**
     * @param cause the cause of this exception
     */
    public WatchListException(Throwable cause)
    {
        super(cause);
    }

    /**
     * @param message the exception message
     * @param cause the cause of this exception
     */
    public WatchListException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
