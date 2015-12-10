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
package org.xwiki.webjars.internal;

import org.xwiki.component.annotation.Role;
import org.xwiki.resource.ResourceReferenceHandlerException;

/**
 * Initializes the Execution Context and XWiki Context for the WebJars Handler.
 *
 * @version $Id$
 * @since 7.1.4, 7.2M4
 */
@Role
public interface ContextInitializer
{
    /**
     * @param wikiId the id of the wiki that needs to be set as the current wiki (e.g. {@code xwiki})
     * @throws ResourceReferenceHandlerException if an error happens during the Context initialization
     */
    void initialize(String wikiId) throws ResourceReferenceHandlerException;
}
