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
package org.xwiki.edit;

/**
 * An edit exception.
 * 
 * @version $Id$
 * @since 8.2RC1
 */
public class EditException extends Exception
{
    /**
     * Class ID for serialization.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Construct an exception with the specified detail message.
     *
     * @param message the detailed message; this can later be retrieved by the Throwable.getMessage() method
     */
    public EditException(String message)
    {
        super(message);
    }

    /**
     * Construct an exception with the specified detail message and cause.
     *
     * @param message the detailed message; this can later be retrieved by the Throwable.getMessage() method
     * @param throwable the cause; this can be retrieved later by the Throwable.getCause() method; (a null value is
     *            permitted, and indicates that the cause is nonexistent or unknown)
     */
    public EditException(String message, Throwable throwable)
    {
        super(message, throwable);
    }
}
