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
package org.xwiki.officeimporter.openoffice;

/**
 * Represents exceptions encountered during operations related to openoffice server management.
 * 
 * @version $Id$
 * @since 1.8RC3
 */
public class OpenOfficeServerManagerException extends Exception
{
    /**
     * Class version.
     */
    private static final long serialVersionUID = 4165514722538231532L;

    /**
     * Constructs a new {@link OpenOfficeServerManagerException}.
     * 
     * @param message the string explaining the error.
     * @param throwable the {@link Throwable} which is the cause of this exception.
     */
    public OpenOfficeServerManagerException(String message, Throwable throwable)
    {
        super(message, throwable);
    }
    
    /**
     * Constructs a new {@link OpenOfficeServerManagerException}.
     * 
     * @param throwable the {@link Throwable} which is the cause of this exception.
     */
    public OpenOfficeServerManagerException(Throwable throwable)
    {
        super(throwable);
    }    
}
