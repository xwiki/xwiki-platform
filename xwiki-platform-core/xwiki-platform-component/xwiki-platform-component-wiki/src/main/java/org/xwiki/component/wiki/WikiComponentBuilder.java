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
import org.xwiki.model.reference.DocumentReference;

/**
 * Allows to provide a list of documents holding one or more {@link WikiComponent}, and to build components from those
 * documents.
 *
 * @version $Id$
 * @since 4.2M3
 */
@Role
public interface WikiComponentBuilder
{
    /**
     * Get the list of documents holding components.
     *
     * @return the list of documents holding components
     */
    List<DocumentReference> getDocumentReferences();

    /**
     * Build the components defined in a document XObjects. Being able to define more than one component in a document
     * depends on the implementation. It is up to the implementation to determine if the last author of the document
     * has the required permissions to register a component.
     * 
     * @param reference the reference to the document that holds component definition objects
     * @return the constructed component definition
     * @throws WikiComponentException when the document contains invalid component definition(s)
     */
    List<WikiComponent> buildComponents(DocumentReference reference) throws WikiComponentException;
}
