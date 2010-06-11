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
 *
 */
package org.xwiki.security;

import org.xwiki.model.reference.EntityReference;

/**
 * Interface for wrapping a class around a properly preprocessed
 * entity reference for use with the cache.
 *
 * TODO Since the usage is broader than just beeing a key for the Cache,
 * this interface should be renamed.  Maybe "RightHierarchy".
 * @version $Id: $
 */
public interface RightCacheKey
{
    /**
     * @return The entity reference returned by this method represents
     * the document hierarchy within the cache.  It must not be used
     * for referencing documents.  We rely on adding the main wiki as
     * an additional WIKI reference entity as the root for virtual
     * wikis.
     */
    EntityReference getEntityReference();
}
