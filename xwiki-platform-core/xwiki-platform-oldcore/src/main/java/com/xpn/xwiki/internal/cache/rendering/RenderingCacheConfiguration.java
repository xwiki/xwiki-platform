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
package com.xpn.xwiki.internal.cache.rendering;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.DocumentReference;

/**
 * Configuration of the rendering cache.
 * 
 * @version $Id$
 * @since 2.4M1
 */
@Role
public interface RenderingCacheConfiguration
{
    /**
     * @return true if the rendering cache system is enabled in general
     */
    boolean isEnabled();

    /**
     * @return the time to live for each element in the cache
     */
    int getDuration();

    /**
     * @return the size of the cache
     */
    int getSize();

    /**
     * Indicate if the provided document's rendering result should be cached.
     * 
     * @param documentReference the reference of the document
     * @return true if the document should be cached, false otherwise
     */
    boolean isCached(DocumentReference documentReference);
}
