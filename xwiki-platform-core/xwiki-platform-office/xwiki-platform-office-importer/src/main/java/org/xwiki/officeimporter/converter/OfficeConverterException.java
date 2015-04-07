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
package org.xwiki.officeimporter.converter;

/**
 * Represents an exception that can be thrown during office document conversion operations.
 * 
 * @version $Id$
 * @since 5.0M2
 */
public class OfficeConverterException extends Exception
{
    /**
     * Class version.
     */
    private static final long serialVersionUID = -8177462576326415823L;

    /**
     * Constructs a new instance.
     * 
     * @param message the exception message
     */
    public OfficeConverterException(String message)
    {
        super(message);
    }

    /**
     * Constructs a new instance.
     * 
     * @param message the exception message
     * @param cause the {@link Throwable} which is the cause of this exception
     */
    public OfficeConverterException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
