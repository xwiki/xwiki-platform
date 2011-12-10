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
package org.xwiki.store;

/**
 * An exception which is unreasonable to expect and is caused by misconfiguration or
 * system failure. This is identical to {@link java.lang.RuntimeException} but subclasses
 * it just so that it can be caught if desired.
 *
 * @version $Id$
 * @since 3.3M1
 */
public class UnexpectedException extends RuntimeException
{
    /**
     * Constructs a new unexpected exception with null as its detail message.
     */
    public UnexpectedException()
    {
        super();
    }

    /**
     * Constructs a new unexpected exception with the specified detail message.
     *
     * @param message the String to explain the exception.
     */
    public UnexpectedException(final String message)
    {
        super(message);
    }

    /**
     * Constructs a new unexpected exception with the specified detail message and cause.
     *
     * @param message the String to explain the exception.
     * @param cause the Throwable which caused this exception.
     */
    public UnexpectedException(final String message, final Throwable cause)
    {
        super(message, cause);
    }

    /**
     * Constructs a new unexpected exception with the specified cause.
     *
     * @param cause the Throwable which caused this exception.
     */
    public UnexpectedException(final Throwable cause)
    {
        super(cause);
    }
}
