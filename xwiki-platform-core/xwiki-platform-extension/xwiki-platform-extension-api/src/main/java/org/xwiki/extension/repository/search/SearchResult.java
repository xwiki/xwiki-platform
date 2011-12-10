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
package org.xwiki.extension.repository.search;

import org.xwiki.extension.Extension;

/**
 * The result of a search of extensions.
 * 
 * @param <E> the extension type
 * @version $Id$
 */
public interface SearchResult<E extends Extension> extends Iterable<E>
{
    /**
     * @return the total number of possible results without offset or maximum results limits
     */
    int getTotalHits();

    /**
     * @return the index in the total number of possible search result where this extract starts
     */
    int getOffset();

    /**
     * @return the number of found extensions
     */
    int getSize();
}
