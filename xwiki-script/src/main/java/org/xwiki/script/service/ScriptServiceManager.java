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
package org.xwiki.script.service;

import org.xwiki.component.annotation.ComponentRole;

/**
 * Provides access to {@link ScriptService} implementation at runtime. Implementations of this interface are supposed
 * to be bound to the Script Context so that they can be access from scripts.
 *
 * @version $Id$
 * @since 2.3M1
 */
@ComponentRole
public interface ScriptServiceManager
{
    /**
     * Note that we're voluntarily using an API name of "get" to make it extra easy to access Script Services from
     * Velocity (since in Velocity writing <code>$services.name</code> is equivalent to writing
     * <code>$services.get("name")</code>). It also makes it a short and easy API name for other scripting languages.
     *
     * @param serviceName the name of the service to look for
     * @return the service asked
     */
    ScriptService get(String serviceName) throws ScriptServiceNotFoundException;
}
