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
package org.xwiki.observation.remote.script;

import java.util.Collection;

import javax.inject.Named;
import javax.inject.Singleton;

import jakarta.inject.Inject;

import org.xwiki.component.annotation.Component;
import org.xwiki.observation.remote.NetworkChannel;
import org.xwiki.observation.remote.RemoteObservationManager;
import org.xwiki.observation.remote.RemoteObservationManagerConfiguration;
import org.xwiki.script.service.ScriptService;

/**
 * Various script APIs related remote observation and clustering.
 * 
 * @version $Id$
 * @since 17.9.0RC1
 */
@Component
@Named(ObservationScriptService.ROLEHINT + '.' + RemoteObservationScriptService.ID)
@Singleton
public class RemoteObservationScriptService implements ScriptService
{
    /**
     * The identifier of the sub extension {@link org.xwiki.script.service.ScriptService}.
     */
    public static final String ID = "remote";

    @Inject
    private RemoteObservationManagerConfiguration configuration;

    @Inject
    private RemoteObservationManager manager;

    /**
     * @return the unique identifier of the instance in the cluster
     */
    public String getId()
    {
        return this.configuration.getId();
    }

    /**
     * @return indicate if the remote observation manager is enabled
     */
    public boolean isEnabled()
    {
        return this.configuration.isEnabled();
    }

    /**
     * @return the identifier of the network adapter implementation to use to actually communicate with other instances
     */
    public String getAdapterId()
    {
        return this.configuration.getNetworkAdapter();
    }

    /**
     * @return the channels used to communicate with other XWiki instances
     */
    public Collection<NetworkChannel> getChannels()
    {
        return this.manager.getChannels();
    }
}
