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
package org.xwiki.security;

import org.xwiki.component.annotation.Role;

/**
 * Provide configuration for the security module.
 *
 * @version $Id$
 * @since 13.10RC1
 */
@Role
public interface SecurityConfiguration
{
    /**
     * @return the number used to control how many items are retrieved through queries (for example inside Velocity
     *         templates). This limit can be customized in the {@code xwiki.properties} file in order to allow
     *         retrieving more or less items. Default value is {@code 100} (this number corresponds to the
     *         LiveTable/LiveData max items view limit). This is to avoid DOS attacks.
     */
    int getQueryItemsLimit();
}
