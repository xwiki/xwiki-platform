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
package org.xwiki.netflux.internal;

/**
 * A user accessing the current instance.
 * 
 * @version $Id$
 * @since 17.10.0
 */
public class RemoteUser extends User
{
    private final String instance;

    /**
     * Creates a new user with the specified name, using the given WebSocket session.
     * 
     * @param name the identifier of the user
     * @param instance the identifier of the instance on which the user is connected
     */
    public RemoteUser(String name, String instance)
    {
        super(name);

        this.instance = instance;
    }

    /**
     * @return the identifier of the instance on which the user is connected
     */
    public String getInstance()
    {
        return this.instance;
    }
}
