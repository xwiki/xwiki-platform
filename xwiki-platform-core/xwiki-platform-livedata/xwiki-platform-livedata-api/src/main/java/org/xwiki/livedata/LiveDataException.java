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
package org.xwiki.livedata;

import org.xwiki.stability.Unstable;

/**
 * Exception related to live data processing.
 *
 * @version $Id$
 * @since 12.6
 */
@Unstable
public class LiveDataException extends Exception
{
    /**
     * Serialization identifier.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Creates a new instance that wraps the specified source throwable.
     *
     * @param source source of the error
     */
    public LiveDataException(Throwable source)
    {
        super(source);
    }

    /**
     * Creates a new instance with the specified message and source.
     *
     * @param message message to store in the exception
     * @param source source of the error
     */
    public LiveDataException(String message, Throwable source)
    {
        super(message, source);
    }
}
