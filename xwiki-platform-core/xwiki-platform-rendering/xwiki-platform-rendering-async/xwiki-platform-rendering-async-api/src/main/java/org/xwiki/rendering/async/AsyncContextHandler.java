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
package org.xwiki.rendering.async;

import java.util.Collection;

import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;

/**
 * Called to give a chance to an handler to do something with the values associated to a cached rendering (inject some
 * required skin extension for example).
 * 
 * @version $Id$
 * @since 10.10RC1
 */
@Role
@Unstable
public interface AsyncContextHandler
{
    /**
     * @param values the values to restore
     */
    void use(Collection<Object> values);

    /**
     * Add to the returned HTML head tags required elements (required js/css, metadatas, etc.).
     * 
     * @param head the head to fill
     * @param values the values for which to add HTML meta (if needed)
     */
    void addHTMLHead(StringBuilder head, Collection<Object> values);
}
