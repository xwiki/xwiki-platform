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
package org.xwiki.ratings;

import org.xwiki.stability.Unstable;

/**
 * Generic exception for Rating API.
 *
 * @version $Id$
 * @since 12.9RC1
 */
@Unstable
public class RatingsException extends Exception
{
    private static final long serialVersionUID = -2229233170941158972L;

    /**
     * Default constructor with message.
     *
     * @param message Cause of the exception.
     */
    public RatingsException(String message)
    {
        super(message);
    }

    /**
     * Default constructor with message and parent exception.
     *
     * @param message Cause of the exception.
     * @param throwable Parent of the exception.
     */
    public RatingsException(String message, Throwable throwable)
    {
        super(message, throwable);
    }
}
