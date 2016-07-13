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
package org.xwiki.ratings;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.DocumentReference;

/**
 * Provide instances of objects configured relative to the passed document.
 *
 * @param <T> the type of the returned object
 * @version $Id$
 * @since 6.4M3
 */
@Role
//TODO: replace this system by a default component dynamically taking into account the configuration behind the scene
public interface ConfiguredProvider<T>
{
    /**
     * Returns an instance of a component based on the current configuration.
     * 
     * @param documentRef the document to which the ratings are associated to
     * @return an instance of RatingsManager (default or separate)
     */
    T get(DocumentReference documentRef);
}
