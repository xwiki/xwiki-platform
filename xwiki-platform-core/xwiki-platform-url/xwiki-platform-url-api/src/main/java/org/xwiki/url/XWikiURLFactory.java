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
package org.xwiki.url;

import org.xwiki.component.annotation.ComponentRole;

import java.util.Map;

/**
 * Transforms some representation of an XWiki URL into a {@link XWikiURL} instance.
 * 
 * @version $Id$
 * @param <T> the object to transform into a XWiki URL
 */
@ComponentRole
public interface XWikiURLFactory<T>
{
    /**
     * Transforms some representation of a XWiki URL into a {@link XWikiURL} instance.
     * 
     * @param urlRepresentation the object to transform into a {@link XWikiURL} instance
     * @param parameters generic parameters that depend on the underlying implementation. In order to know what to
     *        pass you need to check the documentation for the implementation you're using.
     * @return the {@link XWikiURL} instance
     * @throws InvalidURLException if the input representation doesn't represent a valid XWiki URL
     */
    XWikiURL createURL(T urlRepresentation, Map<String, Object> parameters) throws InvalidURLException;
}
