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
package com.xpn.xwiki.internal.parentchild;

import org.xwiki.component.annotation.Role;

/**
 * Configuration of the Parent/Child mechanism.
 *
 * Note: this configuration is supposed to be transient and should be removed when all users have migrated to Nested
 * Pages successfully. This is why we have created a dedicated role in order to not pollute
 * {@link com.xpn.xwiki.CoreConfiguration} with temporary things.
 *
 * @version $Id$
 * @since 9.0RC1
 * @since 8.4.1
 * @since 7.4.6
 */
@Role
public interface ParentChildConfiguration
{
    /**
     * @return whether or not legacy parent/child mechanism is enabled for the hierarchy handling
     */
    boolean isParentChildMechanismEnabled();
}
