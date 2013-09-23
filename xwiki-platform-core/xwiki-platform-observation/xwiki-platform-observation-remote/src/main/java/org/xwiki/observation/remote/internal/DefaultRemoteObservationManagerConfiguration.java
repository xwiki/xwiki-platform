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
package org.xwiki.observation.remote.internal;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.observation.remote.RemoteObservationManagerConfiguration;

/**
 * Provide remote events specific configuration.
 * 
 * @version $Id$
 * @since 2.0M3
 */
@Component
@Singleton
public class DefaultRemoteObservationManagerConfiguration implements RemoteObservationManagerConfiguration
{
    /**
     * USed to access configuration storage.
     */
    @Inject
    @Named("xwikiproperties")
    private ConfigurationSource configurationSource;

    @Override
    public boolean isEnabled()
    {
        Boolean enabled = this.configurationSource.getProperty("observation.remote.enabled", Boolean.class);

        return enabled != null ? enabled : false;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<String> getChannels()
    {
        List<String> channels = this.configurationSource.getProperty("observation.remote.channels", List.class);

        return channels == null ? Collections.<String> emptyList() : channels;
    }

    @Override
    public String getNetworkAdapter()
    {
        return this.configurationSource.getProperty("observation.remote.networkadapter", "jgroups");
    }
}
