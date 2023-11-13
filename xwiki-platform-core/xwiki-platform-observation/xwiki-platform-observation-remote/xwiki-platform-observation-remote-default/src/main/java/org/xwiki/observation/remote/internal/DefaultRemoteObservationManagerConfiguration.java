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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.environment.Environment;
import org.xwiki.observation.remote.RemoteObservationManagerConfiguration;

/**
 * Provide remote events specific configuration.
 *
 * @version $Id$
 * @since 2.0M3
 */
@Component
@Singleton
public class DefaultRemoteObservationManagerConfiguration implements RemoteObservationManagerConfiguration,
    Initializable
{
    /**
     * USed to access configuration storage.
     */
    @Inject
    @Named("xwikiproperties")
    private ConfigurationSource configurationSource;

    @Inject
    private Environment environment;

    private String id;

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

        return channels == null ? Collections.<String>emptyList() : channels;
    }

    @Override
    public String getNetworkAdapter()
    {
        return this.configurationSource.getProperty("observation.remote.networkadapter", "jgroups");
    }

    @Override
    public String getId()
    {
        return this.id;
    }

    @Override
    public void initialize() throws InitializationException
    {
        Path observation = getObservationDirectory();
        observation.toFile().mkdirs();
        Path idFile = getIdFile();
        if (!Files.exists(idFile)) {
            try {
                String idString = UUID.randomUUID().toString();
                Files.writeString(idFile, idString);
                this.id = idString;
            } catch (IOException e) {
                throw new InitializationException(String.format("Failed to create observation id file [%s]", idFile),
                    e);
            }
        } else {
            try {
                this.id = Files.readString(idFile);
            } catch (IOException e) {
                throw new InitializationException(String.format("Failed to read the observation id file [%s]", idFile),
                    e);
            }
        }
    }

    private Path getIdFile()
    {
        return getObservationDirectory().resolve("id.txt");
    }

    private Path getObservationDirectory()
    {
        return this.environment.getPermanentDirectory().toPath().resolve("observation");
    }
}
