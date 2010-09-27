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
import org.xwiki.rendering.listener.Link;
import org.xwiki.rendering.listener.LinkType;

/**
 * Parses a raw link by determining if it has the right type and return a non-null
 * {@link org.xwiki.rendering.listener.Link} object if it has.
 *
 * @version $Id$
 * @since 2.5M2
 */
@ComponentRole
public interface LinkTypeParser
{
    /**
     * @return the link type (document, url, attachment, etc).
     */
    LinkType getType();

    /**
     * @param reference the raw link reference to parse
     * @return the parsed link information if the passed reference can be parsed by this link type parser or null if
     *         the passed reference isn't valid
     */
    Link parse(String reference);
}
