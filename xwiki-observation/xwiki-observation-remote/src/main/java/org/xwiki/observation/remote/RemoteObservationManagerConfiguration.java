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
package org.xwiki.observation.remote;

import java.util.List;

import org.xwiki.component.annotation.ComponentRole;

/**
 * Provide configuration for remote observation manager.
 * 
 * @version $Id$
 * @since 2.0M3
 */
@ComponentRole
public interface RemoteObservationManagerConfiguration
{
    /**
     * @return indicate if the remote observation manager is enabled
     */
    boolean isEnabled();

    /**
     * @return the channels to start at init
     */
    List<String> getChannels();

    /**
     * @return the identifier of the network adapter implementation to use to actually send and receive network messages
     */
    String getNetworkAdapter();
}
