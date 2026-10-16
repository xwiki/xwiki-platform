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
package org.xwiki.resource.internal.entity;

import org.xwiki.component.annotation.Role;
import org.xwiki.resource.ResourceReferenceHandlerException;
import org.xwiki.resource.entity.EntityResourceReference;

/**
 * Prepares the context in which the Handler of an Entity Resource Action executes, as the legacy {@code XWikiAction}s
 * do before executing an action.
 *
 * @version $Id$
 * @since 18.10.0RC1
 */
@Role
public interface EntityResourceContextInitializer
{
    /**
     * Initializes the context needed to handle the passed reference, without waiting for what it targets to be
     * initialized: when it isn't, the request is answered directly (e.g. with the initialization progress) instead.
     *
     * @param reference the Entity Resource Reference about to be handled
     * @return {@code true} if the reference can be handled, {@code false} if the request has already been answered and
     *         the reference must not be handled
     * @throws ResourceReferenceHandlerException if the context cannot be initialized
     */
    boolean initialize(EntityResourceReference reference) throws ResourceReferenceHandlerException;
}
