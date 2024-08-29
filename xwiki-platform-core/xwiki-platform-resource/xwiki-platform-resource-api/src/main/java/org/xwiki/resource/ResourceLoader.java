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
package org.xwiki.resource;

import java.io.InputStream;

import org.xwiki.component.annotation.Role;

/**
 * Load Resources pointed to by Resource Reference as a Input Stream.
 *
 * @param <T> the Resource Reference type to load
 * @version $Id$
 * @since 6.1M2
 */
@Role
public interface ResourceLoader<T extends ResourceReference>
{
    /**
     * @param reference the reference to the Resource to load
     * @return the Resource content as an Input Stream or null if it doesn't exist or cannot be loaded
     */
    InputStream load(T reference);
}
