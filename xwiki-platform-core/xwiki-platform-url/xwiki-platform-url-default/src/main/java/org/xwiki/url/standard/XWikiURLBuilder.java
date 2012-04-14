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
package org.xwiki.url.standard;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.url.XWikiURL;

import java.util.List;

/**
 * @version $Id$
 * @since 2.3M1
 */
@Role
public interface XWikiURLBuilder
{
    /**
     * Builds a {@link XWikiURL} object from the passed URL path segments.
     *
     * @param wikiReference the wiki reference part of the
     * @param pathSegments the URL path segments (ie the parts separated by forward slashes in the URL) but starting
     *        at the action segment part (e.g. List of ("view", "Space", "Page") for "/view/Space/Page")
     * @return the {@link XWikiURL} object
     */
    XWikiURL build(WikiReference wikiReference, List<String> pathSegments);
}
