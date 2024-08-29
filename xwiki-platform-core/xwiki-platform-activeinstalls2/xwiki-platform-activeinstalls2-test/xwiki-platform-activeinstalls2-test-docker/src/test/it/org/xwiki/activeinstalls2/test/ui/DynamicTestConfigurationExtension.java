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
package org.xwiki.activeinstalls2.test.ui;

import java.util.List;
import java.util.Properties;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.xwiki.activeinstalls2.internal.XWikiElasticSearchExtension;
import org.xwiki.test.docker.junit5.TestConfiguration;

/**
 * Inject a dynamically-generated {@link TestConfiguration} in order to set the {@code activeinstalls2.pingURL} config
 * property to point to the ES instance started dynamically (on a dynamic port) during the test.
 *
 * @version $Id$
 * @since 14.5
 */
public class DynamicTestConfigurationExtension implements BeforeAllCallback
{
    @Override
    public void beforeAll(ExtensionContext extensionContext)
    {
        // Get the Elasticsearch container set by XWikiElasticSearchExtension
        ExtensionContext.Store store = XWikiElasticSearchExtension.getStore(extensionContext);
        ElasticsearchContainer esContainer = (ElasticsearchContainer) store.get(ElasticsearchContainer.class);

        // Save a TestConfiguration in the global test context so that it's merged in XWikiDockerExtension.
        ExtensionContext.Store globalStore =  extensionContext.getRoot().getStore(ExtensionContext.Namespace.GLOBAL);
        TestConfiguration configuration = new TestConfiguration();
        Properties properties = new Properties();
        String hostAddress = String.format("http://%s:%d", esContainer.getHost(), esContainer.getMappedPort(9200));
        properties.setProperty("xwikiPropertiesAdditionalProperties",
            String.format("activeinstalls2.pingURL=%s", hostAddress));
        configuration.setProperties(properties);
        // When the Servlet engine is running inside docker, Active Installs (thus running inside docker) will need
        // access the socket of the Elasticsearch server running on the host. Thus, we need to expose the port, see
        // https://www.testcontainers.org/features/networking/#exposing-host-ports-to-the-container.
        configuration.setSSHPorts(List.of(esContainer.getMappedPort(9200)));
        globalStore.put(TestConfiguration.class, configuration);
    }
}
