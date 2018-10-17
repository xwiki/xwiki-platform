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
package org.xwiki.rendering.async.internal;

import java.util.List;
import java.util.Set;

import org.xwiki.model.reference.EntityReference;
import org.xwiki.stability.Unstable;

/**
 * Execute a task and return a String result.
 * 
 * @version $Id$
 * @since 10.9RC1
 */
@Unstable
public interface AsyncRenderer
{
    /**
     * @return the id used as prefix (concatenated with contextual information) for the actual job identifier
     */
    List<String> getId();

    /**
     * @return the resulting {@link String}
     */
    String render();

    /**
     * @return get the references involved in the rendering (they will be used to invalidate the cache when one of those
     *         entities is modified)
     */
    Set<EntityReference> getReferences();
}
