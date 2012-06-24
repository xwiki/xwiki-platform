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
package org.xwiki.officeimporter;

/**
 * The class used to indicate exceptions specific to the Office Importer plugin.
 * 
 * @version $Id$
 * @since 1.8M1
 */
public class OfficeImporterException extends Exception
{
    /**
     * Class version.
     */
    private static final long serialVersionUID = 1628817654803316987L;

    /**
     * Constructs a new exception with the specified message.
     * 
     * @param message The explanation of the exception.
     */
    public OfficeImporterException(String message)
    {
        super(message);
    }

    /**
     * Constructs a new exception with the specified cause.
     * 
     * @param throwable The underlying cause for this exception.
     */
    public OfficeImporterException(Throwable throwable)
    {
        super(throwable);
    }

    /**
     * Constructs a new exception with the specified message and cause.
     * 
     * @param message The explanation of the exception.
     * @param throwable The underlying cause for this exception.
     */
    public OfficeImporterException(String message, Throwable throwable)
    {
        super(message, throwable);
    }
}
