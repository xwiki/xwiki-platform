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
package org.xwiki.url.internal.standard;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.url.ExtendedURL;

/**
 * Extracts the reference to the wiki pointed to by the specified {@link ExtendedURL} object.
 *
 * @version $Id$
 * @since 5.1M1
 */
@Role
public interface WikiReferenceExtractor
{
    /**
     * Extract the name of the wiki the URL is pointing to.
     *
     * @param url the URL from which to extract the wiki reference
     * @return the reference to the wiki pointed to by the passed URL
     * @since 6.3M1 returns only a WikiReference (whereas before it was returning both a WikiReference and a boolean)
     */
    WikiReference extract(ExtendedURL url);
}
