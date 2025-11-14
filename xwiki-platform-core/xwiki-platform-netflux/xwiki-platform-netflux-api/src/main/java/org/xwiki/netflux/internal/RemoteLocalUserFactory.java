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

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.websocket.Session;

import org.xwiki.component.annotation.Component;
import org.xwiki.observation.remote.RemoteObservationManagerConfiguration;

/**
 * An implementation of {@link LocalUserFactory} which avoid collision in a cluster use case.
 * 
 * @version $Id$
 * @since 17.10.0RC1
 */
@Component
@Singleton
public class RemoteLocalUserFactory extends DefaultLocalUserFactory
{
    @Inject
    private RemoteObservationManagerConfiguration remoteConfiguation;

    @Override
    protected String getId(Session session)
    {
        return this.remoteConfiguation.getId() + '-' + super.getId(session);
    }
}
