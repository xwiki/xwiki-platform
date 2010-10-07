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
package org.xwiki.rendering.renderer.link;

import org.xwiki.component.annotation.ComponentRole;
import org.xwiki.rendering.listener.ResourceReference;

/**
 * Generate Resource Reference labels for URIs. For example an implementation for MAILTO URIs would remove the scheme
 * part and the query string part.
 *
 * @version $Id$
 * @since 2.2RC1
 */
@ComponentRole
public interface URILabelGenerator
{
    /**
     * @param reference the reference pointing to a URI for which we want to generate a label
     * @return the URI label to display when rendering resource references
     * @since 2.5RC1
     */
    String generateLabel(ResourceReference reference);
}
