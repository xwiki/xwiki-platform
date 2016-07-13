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

import org.xwiki.component.annotation.Role;

/**
 * Normalize a relative URL. Various implementations can exist, for example one implementation may normalize the passed
 * relative URL into a full absolute {@link java.net.URL} object. Another implementation may simply prefix the passed
 * relative URL with a Servlet Container's webapp name (aka application context name). Note that in general the
 * implementations will depends on the Container in which XWiki is executing (eg Servlet Container).
 * <p>
 * It's also important that Normalizers should be independent of URL Scheme formats since they should be usable
 * for all URL Schemes. Note that Normalizers should not replace {@link org.xwiki.resource.ResourceReferenceSerializer}
 * implementations.
 * </p>
 *
 * @param <T> the type of object to return (eg {@link ExtendedURL}, {@link java.net.URL})
 * @version $Id$
 * @since 6.1M2
 */
@Role
public interface URLNormalizer<T>
{
    /**
     * @param partialURL the partial URL to normalize
     * @return the normalized URL, what is done depends on the implementation
     */
    T normalize(ExtendedURL partialURL);
}
