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
package org.xwiki.test.integration.maven;

import java.net.ProxySelector;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.aether.repository.Proxy;
import org.eclipse.aether.repository.RemoteRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Unit tests for {@link RepositoryResolver}.
 *
 * @version $Id$
 */
class RepositoryResolverTest
{
    private static final List<String> PROXY_PROPERTIES = List.of("http.proxyHost", "http.proxyPort",
        "https.proxyHost", "https.proxyPort", "http.nonProxyHosts");

    private final Map<String, String> savedProperties = new HashMap<>();

    private ProxySelector savedProxySelector;

    @BeforeEach
    void saveProxyProperties()
    {
        this.savedProxySelector = ProxySelector.getDefault();
        // The JVM running the tests may itself be configured to use a proxy.
        PROXY_PROPERTIES.forEach(name -> {
            this.savedProperties.put(name, System.getProperty(name));
            System.clearProperty(name);
        });
    }

    @AfterEach
    void restoreProxyProperties()
    {
        ProxySelector.setDefault(this.savedProxySelector);
        this.savedProperties.forEach((name, value) -> {
            if (value == null) {
                System.clearProperty(name);
            } else {
                System.setProperty(name, value);
            }
        });
    }

    @Test
    void repositoriesUseTheJVMProxy()
    {
        System.setProperty("https.proxyHost", "proxy.example.org");
        System.setProperty("https.proxyPort", "3128");

        List<RemoteRepository> repositories = new RepositoryResolver(true).getRepositories();

        assertFalse(repositories.isEmpty());
        repositories.forEach(repository -> {
            Proxy proxy = repository.getProxy();
            assertNotNull(proxy, repository.getId());
            assertEquals("proxy.example.org", proxy.getHost(), repository.getId());
            assertEquals(3128, proxy.getPort(), repository.getId());
        });
    }

    @Test
    void repositoriesWithoutJVMProxy()
    {
        // Clearing the proxy system properties isn't enough when the JVM uses the system proxies
        // (java.net.useSystemProxies), so also install a JVM proxy selector that never returns a proxy.
        ProxySelector.setDefault(ProxySelector.of(null));

        List<RemoteRepository> repositories = new RepositoryResolver(true).getRepositories();

        assertFalse(repositories.isEmpty());
        repositories.forEach(repository -> assertNull(repository.getProxy(), repository.getId()));
    }
}
