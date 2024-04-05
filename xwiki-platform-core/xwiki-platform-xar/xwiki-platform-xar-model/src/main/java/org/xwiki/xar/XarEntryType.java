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

import org.xwiki.component.annotation.Role;

/**
 * The type of the {@link XarEntry} which control the behavior to adopt while upgrading, editing, etc.
 * 
 * @version $Id$
 * @since 10.3
 */
@Role
public interface XarEntryType
{
    /**
     * The upgrade behavior.
     * 
     * @version $Id$
     */
    enum UpgradeType
    {
        /**
         * Apply a 3 ways merge (the default).
         */
        THREEWAYS,

        /**
         * Always overwrite the existing document.
         */
        OVERWRITE,

        /**
         * If what already exists is non standard don't touch it.
         */
        SKIP,

        /**
         * If anything already exists don't touch it.
         */
        SKIP_ALLWAYS
    }

    /**
     * @return the name of the type
     */
    String getName();

    /**
     * @return true if editing this document is allowed
     */
    boolean isEditAllowed();

    /**
     * @return true if deleting this document is allowed
     */
    boolean isDeleteAllowed();

    /**
     * @return the upgrade behavior
     */
    UpgradeType getUpgradeType();
}
