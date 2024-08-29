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
package org.xwiki.configuration;

import org.xwiki.component.annotation.Role;
import org.xwiki.user.UserReference;

/**
 * Provides authorization for a given {@link ConfigurationSource}.
 *
 * @version $Id$
 * @since 12.4RC1
 */
@Role
public interface ConfigurationSourceAuthorization
{
    /**
     * @param key the key for which to check the access right for
     * @param userReference the reference to the user to check for permissions
     * @param right the right to check (e.g. {@code AccessRight.READ})
     * @return true if the property can be accessed for the passed right (e.g. READ or WRITE)
     */
    boolean hasAccess(String key, UserReference userReference, ConfigurationRight right);
}
