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
package org.xwiki.component.wiki;

import java.util.List;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.ObjectReference;

/**
 * Allows to build one or more {@link WikiComponent} based on the XObjects contained in an EntityReference.
 * The role hint of the component should match the XObject class name
 *
 * @version $Id$
 * @since 9.5RC1
 */
@Role
public interface WikiObjectComponentBuilder
{
    /**
     * Get the class that should trigger {@link #buildComponents(ObjectReference)} when one XObject implementing
     * this very class is added, updated or deleted in the wiki.
     *
     * @return an {@link EntityReference} to the correct class.
     */
    ObjectReference getClassReference();

    /**
     * Build the components that is linked to the given {@link ObjectReference}.
     *
     * @param reference the reference of the object that should be used to create the component.
     * @return the new component
     * @throws WikiComponentException if the given {@link ObjectReference} is incompatible with the current builder or
     * if the {@link WikiComponentBuilder} has not been able to instanciate the component.
     */
    List<WikiComponent> buildComponents(ObjectReference reference) throws WikiComponentException;
}
