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
package org.xwiki.component.wiki.internal;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;

/**
 * Utility class allowing to manipulate context information.
 *
 * @version $Id$
 * @since 4.3M2
 */
@Role
public interface WikiComponentManagerContext
{
    /**
     * @return A reference to the context user.
     */
    DocumentReference getCurrentUserReference();

    /**
     * @return A reference to the context document.
     */
    EntityReference getCurrentEntityReference();

    /**
     * Set the context user.
     *
     * @param reference A reference to the context user to set
     */
    void setCurrentUserReference(DocumentReference reference);

    /**
     * Set the context document.
     *
     * @param reference A reference to the context document to set
     */
    void setCurrentEntityReference(EntityReference reference);
}
