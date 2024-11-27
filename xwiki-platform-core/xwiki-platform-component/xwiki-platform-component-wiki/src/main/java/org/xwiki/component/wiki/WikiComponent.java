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

import java.lang.reflect.Type;

import org.xwiki.component.descriptor.ComponentDescriptor;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;

/**
 * Represents the definition of a wiki component implementation. A java component can extend this interface if it needs
 * to be bound to a document, in order to be unregistered and registered again when the document is modified, and
 * unregistered when the document is deleted.
 * 
 * @version $Id$
 * @since 4.2M3
 */
public interface WikiComponent
{
    /**
     * Get the reference of the document this component instance is bound to.
     *
     * @return the reference to the document holding this wiki component definition.
     */
    DocumentReference getDocumentReference();

    /**
     * Get the entity reference of the bounded component instance.
     *
     * @return the entity reference bounded to the component
     * @since 9.5RC1
     */
    default EntityReference getEntityReference()
    {
        return this.getDocumentReference();
    }

    /**
     * Get the reference to the author of the document this component instance is bound to.
     *
     * @return the reference to the author of the document holding this wiki component definition.
     */
    DocumentReference getAuthorReference();
    
    /**
     * @return the role implemented by this component implementation.
     */
    Type getRoleType();

    /**
     * @return the hint of the role implemented by this component implementation.
     */
    String getRoleHint();

    /**
     * The role type priority represents the priority for ordering components sharing the same type: it can be used to
     * order the components when retrieving a list of components of the same type. The lower the value, the higher the
     * priority.
     *
     * @return the role type priority of the component.
     * @since 15.4RC1
     */
    default int getRoleTypePriority()
    {
        return ComponentDescriptor.DEFAULT_PRIORITY;
    }

    /**
     * The role hint priority represents the priority for ordering components sharing the same type and hint: it can be
     * used to decide which component should be overridden when loading them. The lower the value, the higher the
     * priority.
     *
     * @return the role hint priority of the component.
     * @since 15.4RC1
     */
    default int getRoleHintPriority()
    {
        return ComponentDescriptor.DEFAULT_PRIORITY;
    }

    /**
     * @return the {@link WikiComponentScope} of the component.
     */
    WikiComponentScope getScope();
}
