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
package org.xwiki.model;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.EntityReference;

/**
 * Allows accessing Model Objects for current objects (current document, current wiki, current space, etc) placed in the
 * Execution Context.
 * 
 * @version $Id$
 * @since 2.2M1
 */
@Role
public interface ModelContext
{
    /**
     * @return the reference to the current entity located in the Execution Context or null if there's none
     */
    EntityReference getCurrentEntityReference();

    /**
     * @param entityReference the reference to the current entity located in the Execution Context
     * @since 2.4M2
     */
    void setCurrentEntityReference(EntityReference entityReference);
}
