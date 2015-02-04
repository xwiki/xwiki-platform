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
package org.xwiki.resource;

/**
 * Means that an error occurred while trying to construct an {@link ResourceReference} object.
 *
 * @version $Id$
 * @since 5.3M1
 */
public class ResourceReferenceCreationException extends Exception
{
    /**
     * @param message The detailed message. This can later be retrieved by the Throwable.getMessage() method.
     */
    public ResourceReferenceCreationException(String message)
    {
        super(message);
    }

    /**
     * @param message The detailed message. This can later be retrieved by the Throwable.getMessage() method.
     * @param throwable the cause. This can be retrieved later by the Throwable.getCause() method. (A null value
     *        is permitted, and indicates that the cause is nonexistent or unknown)
     */
    public ResourceReferenceCreationException(String message, Throwable throwable)
    {
        super(message, throwable);
    }
}
