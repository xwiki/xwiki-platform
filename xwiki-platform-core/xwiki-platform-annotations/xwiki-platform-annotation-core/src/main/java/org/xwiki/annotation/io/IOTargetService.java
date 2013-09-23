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
package org.xwiki.annotation.io;

import org.xwiki.component.annotation.Role;

/**
 * This service provides functions to operate with annotations targets. It operates with string serialized references to
 * such targets. This interface does not restrict the implementation of the annotation targets, they can be anything
 * referencable through a string.
 * 
 * @version $Id$
 * @since 2.3M1
 */
@Role
public interface IOTargetService
{
    /**
     * @param reference serialized string reference of the content to get the source for
     * @return the source of the referenced content
     * @throws IOServiceException if any exception occurs when manipulating sources
     */
    String getSource(String reference) throws IOServiceException;

    /**
     * @param reference serialized string reference of the content whose syntax to return
     * @return the syntax of the source of the referenced content
     * @throws IOServiceException if any exception occurs when manipulating sources
     */
    String getSourceSyntax(String reference) throws IOServiceException;
}
