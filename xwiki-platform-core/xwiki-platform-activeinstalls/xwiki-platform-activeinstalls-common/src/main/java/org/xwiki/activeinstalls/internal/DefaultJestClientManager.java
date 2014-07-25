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
package org.xwiki.activeinstalls.internal;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.activeinstalls.ActiveInstallsConfiguration;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLifecycleException;
import org.xwiki.component.phase.Disposable;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;

import io.searchbox.client.JestClient;
import io.searchbox.client.JestClientFactory;
import io.searchbox.client.config.ClientConfig;

/**
 * Factory to get a singleton {@link JestClient} instance since it's threadsafe. The URL to connect to is defined in
 * the Active Install configuration.
 *
 * @version $Id$
 * @since 5.2M2
 */
@Component
@Singleton
public class DefaultJestClientManager implements JestClientManager, Initializable, Disposable
{
    @Inject
    private ActiveInstallsConfiguration configuration;

    /**
     * The Jest Client singleton instance to use to connect to the remote instance.
     */
    private JestClient client;

    @Override
    public void initialize() throws InitializationException
    {
        String pingURL = this.configuration.getPingInstanceURL();
        ClientConfig clientConfig = new ClientConfig.Builder(pingURL).multiThreaded(true).build();
        JestClientFactory factory = new JestClientFactory();
        factory.setClientConfig(clientConfig);
        this.client = factory.getObject();
    }

    @Override
    public void dispose() throws ComponentLifecycleException
    {
        if (this.client != null) {
            this.client.shutdownClient();
        }
    }

    @Override
    public JestClient getClient()
    {
        return this.client;
    }
}
