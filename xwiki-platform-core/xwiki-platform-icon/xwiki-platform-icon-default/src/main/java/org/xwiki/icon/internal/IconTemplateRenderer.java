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
package org.xwiki.icon.internal;

import org.xwiki.component.annotation.Role;
import org.xwiki.icon.IconException;
import org.xwiki.model.reference.DocumentReference;

/**
 * Interface for rendering icons based on a template.
 *
 * @version $Id$
 * @since 17.6.0RC1
 */
@Role
public interface IconTemplateRenderer
{
    /**
     * Renders the given icon based on the template of the renderer.
     *
     * @param iconValue the icon to render
     * @param contextDocumentReference the reference of the context document whose author's rights should be used
     * @return the rendered string
     * @throws IconException if the icon cannot be rendered
     */
    String render(String iconValue, DocumentReference contextDocumentReference) throws IconException;
}
