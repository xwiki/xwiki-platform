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
package org.xwiki.edit;

import org.xwiki.component.annotation.Role;

/**
 * Configuration options for editing XWiki documents.
 * 
 * @version $Id$
 * @since 12.5
 */
@Role
public interface DocumentEditConfiguration
{
    /**
     * @return {@code true} if XWiki documents should be edited in-place (from view mode) whenever possible,
     *         {@code false} otherwise (the stand-alone edit mode is preferred instead)
     */
    boolean inPlaceEditingEnabled();
}
