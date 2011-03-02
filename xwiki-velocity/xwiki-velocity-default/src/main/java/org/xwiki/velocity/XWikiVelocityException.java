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
 *
 */
package org.xwiki.velocity;

/**
 * Any exception raised in the XWiki Velocity component must raise an exception of this type.
 * 
 * @version $Id$
 */
public class XWikiVelocityException extends Exception
{
    /**
     * Provides an id for serialization.
     */
    private static final long serialVersionUID = 2035182137507870523L;

    /**
     * Constructs a new exception with the specified detail message.
     * 
     * @param message the exception message
     * @see Exception#Exception(String)
     */
    public XWikiVelocityException(String message)
    {
        super(message);
    }

    /**
     * Constructs a new exception with the specified detail message and cause.
     * 
     * @param message the exception message
     * @param throwable the original exception to wrap
     * @see Exception#Exception(String, Throwable)
     */
    public XWikiVelocityException(String message, Throwable throwable)
    {
        super(message, throwable);
    }
}
