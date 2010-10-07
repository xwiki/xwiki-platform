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
package org.xwiki.rendering.parser;

import org.xwiki.component.annotation.ComponentRole;
import org.xwiki.rendering.listener.ResourceReference;

/**
 * Interface for parsing resource references (references to links, images, attachments, etc) for various wiki syntaxes.
 * 
 * @version $Id$
 * @since 2.5RC1
 */
@ComponentRole
public interface ResourceReferenceParser
{
    /**
     * Parses a resource reference represented (reference to a link, image, attachment, etc)  as a String into a
     * {@link org.xwiki.rendering.listener.ResourceReference} object.
     * 
     * @param rawLink the string representation of the resource reference to parse (the supported syntax depends on the
     *        parser implementation used)
     * @return the parsed resource reference
     */
    ResourceReference parse(String rawLink);
}
