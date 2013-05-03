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

import java.lang.reflect.ParameterizedType;
import java.net.URL;
import java.util.Map;

import org.xwiki.component.annotation.Role;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.stability.Unstable;

/**
 * Transforms some representation of an XWiki URL into a {@link XWikiURL} instance.
 *
 * @param <T> the object to transform into a XWiki URL
 * @param <U> the return type (e.g. {@link XWikiURL}, {@link XWikiEntityURL}, etc)
 * @version $Id$
 */
@Role
@Unstable
public interface XWikiURLFactory<T, U extends XWikiURL>
{
    /**
     * Type instance for {@code XWikiURLFactory<URL, XWikiURL>}.
     */
    ParameterizedType TYPE_URL_XWIKIURL =
        new DefaultParameterizedType(null, XWikiURLFactory.class, URL.class, XWikiURL.class);

    /**
     * Transforms some representation of a XWiki URL into a {@link XWikiURL} instance.
     *
     * @param urlRepresentation the object to transform into a {@link XWikiURL} instance
     * @param parameters generic parameters that depend on the underlying implementation. In order to know what to pass
     * you need to check the documentation for the implementation you're using.
     * @return the {@link XWikiURL} instance
     * @throws URLCreationException if there was an error while creating the XWiki URL object
     * @throws UnsupportedURLException if the passed URL points to an unsupported URL type that we cannot parse
     */
    U createURL(T urlRepresentation, Map<String, Object> parameters)
        throws URLCreationException, UnsupportedURLException;
}
