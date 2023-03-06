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
package org.xwiki.icon;

/**
 * Exception relating to icon manipulations.
 *
 * @since 6.2M1
 * @version $Id$
 */
public class IconException extends Exception
{
    /**
     * Constructor.
     *
     * @param message message to store in the exception
     * @param source source of the error
     */
    public IconException(String message, Throwable source)
    {
        super(message, source);
    }

    /**
     * Constructor with just a message.
     *
     * @param message the message to store in the exception
     * @since 14.10.6
     * @since 15.2RC1
     */
    public IconException(String message)
    {
        super(message);
    }
}
