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
package org.xwiki.uiextension;

import java.util.List;

import org.xwiki.component.annotation.Role;

/**
 * A UIExtensionFilter provides a way to filter a list of {@link UIExtension}.
 *
 * @version $Id$
 * @since 4.3M1
 */
@Role
public interface UIExtensionFilter
{
    /**
     * Filter a list of {@link UIExtension}. Some filters require information provided by the user of the API, since we
     * don't support Constructor or setter injection we need the "parameter" argument to pass information to the filter.
     *
     * @param extensions The list of {@link UIExtension}s to filter
     * @param parameters A list of optional parameters
     * @return The filtered list
     */
    List<UIExtension> filter(List<UIExtension> extensions, String... parameters);
}
