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
package org.xwiki.rendering.renderer.reference.link;

import org.xwiki.component.annotation.ComponentRole;
import org.xwiki.rendering.listener.reference.ResourceReference;

/**
 * Generate Link label. Used when no label has been specified by the user for links.
 * 
 * @version $Id$
 * @since 1.8RC2
 */
@ComponentRole
public interface LinkLabelGenerator
{
    /**
     * Generate a clean String label for the provided {@link org.xwiki.rendering.listener.reference.ResourceReference}.
     * 
     * @param reference the link reference
     * @return the generated label
     * @since 2.5RC1
     */
    String generate(ResourceReference reference);
}
