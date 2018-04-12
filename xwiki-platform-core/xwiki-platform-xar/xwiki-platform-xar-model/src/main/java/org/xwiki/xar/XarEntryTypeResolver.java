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
package org.xwiki.xar;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;

/**
 * Resolve proper {@link XarEntryType} instance depending on the name and the context.
 * 
 * @version $Id$
 * @since 10.3RC1
 */
@Component
@Role
@Unstable
public interface XarEntryTypeResolver
{
    /**
     * The prefix used for the hints of components specific to the entry reference.
     */
    String DOCUMENT_PREFIX = "document:";

    /**
     * @param entry the XAR entry
     * @param fallbackOnDefault if true, return the default {@link XarEntryType} if no specific component can be found
     * @return the {@link XarEntryType} instance
     */
    XarEntryType resolve(XarEntry entry, boolean fallbackOnDefault);

    /**
     * @return the default {@link XarEntryType} instance
     */
    XarEntryType getDefault();
}
