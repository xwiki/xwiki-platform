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
package org.xwiki.container;

import org.xwiki.component.annotation.Role;

/**
 * Let any component that need to perform further initializations involving the Request
 * do so. For example the Velocity component creates a brand new Velocity Context which
 * it puts in the Request.
 *
 * @version $Id$
 */
@Role
public interface RequestInitializer
{
    /**
     * Perform initializations here, such as storing objects in the Request.
     * 
     * @param request the XWiki Request object
     * @throws RequestInitializerException if the initialization fails. It's expected that the
     *         application should stop if this happens.
     */
    void initialize(Request request) throws RequestInitializerException;
}
