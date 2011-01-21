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
package org.xwiki.rendering.macro.cache;

import org.xwiki.properties.annotation.PropertyDescription;

/**
 * Parameters for the Cache macro.
 *
 * @version $Id$
 * @since 3.0M1
 */
public class CacheMacroParameters
{
    /**
     * @see #getId()
     */
    private String id;

    /**
     * @return the optional unique id to use to cache the content. If not defined then use the content itself as the id
     *         but this doesn't guarantee unicity since the same content could be located on several pages with
     *         different results.
     */
    public String getId()
    {
        return this.id;
    }

    /**
     * @param id refer to {@link #getId()}
     */
    @PropertyDescription("a unique id under which the content is cached")
    public void setId(String id)
    {
        this.id = id;
    }

}
