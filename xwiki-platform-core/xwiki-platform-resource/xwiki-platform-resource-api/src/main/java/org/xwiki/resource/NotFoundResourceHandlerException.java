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
 * Whenever a Resource Handler doesn't exist for handling a given Resource Reference.
 *
 * @version $Id$
 * @since 6.1M2
 */
public class NotFoundResourceHandlerException extends ResourceReferenceHandlerException
{
    /**
     * Class ID for serialization.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Construct a new exception with the specified detail message.
     *
     * @param reference the resource reference for which no Resource Handler  could be found
     */
    public NotFoundResourceHandlerException(ResourceReference reference)
    {
        super(computeMessage(reference));
    }

    /**
     * Construct a new exception with the specified detail message and cause.
     *
     * @param resource the resource for which no Action could be found
     * @param throwable the cause. This can be retrieved later by the Throwable.getCause() method. (A null value
     *        is permitted, and indicates that the cause is nonexistent or unknown)
     */
    public NotFoundResourceHandlerException(ResourceReference resource, Throwable throwable)
    {
        super(computeMessage(resource), throwable);
    }

    private static String computeMessage(ResourceReference reference)
    {
        return String.format("No Handler was found to handle Resource Reference [%s]", reference);
    }
}
