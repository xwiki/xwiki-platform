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
package org.xwiki.index.test.ui.docker;

import java.util.Properties;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.xwiki.test.docker.internal.junit5.DockerTestUtils;
import org.xwiki.test.docker.junit5.TestConfiguration;

/**
 * Inject a dynamically-generated {@link TestConfiguration} in order to set the {@code index.sortCollation} config
 * property to a database-dependent collation.
 *
 * @version $Id$
 * @since 17.6.0RC1
 */
public class DynamicTestConfigurationExtension implements BeforeAllCallback
{
    @Override
    public void beforeAll(ExtensionContext extensionContext)
    {
        // Get the already loaded configuration to get the used database engine.
        TestConfiguration loadedConfiguration = DockerTestUtils.getTestConfiguration(extensionContext);

        // Collations to test. They were chosen such that they offer sorting for German umlauts that differs from the
        // default "binary" sorting.
        String collation = switch (loadedConfiguration.getDatabase()) {
            case MYSQL, MARIADB -> "utf8mb4_german2_ci";
            case HSQLDB_EMBEDDED -> "de_DE";
            case POSTGRESQL -> "unicode";
            case ORACLE -> "GENERIC_M_CI";
        };

        // Save a TestConfiguration in the global test context so that it's merged in XWikiDockerExtension.
        ExtensionContext.Store globalStore = extensionContext.getRoot().getStore(ExtensionContext.Namespace.GLOBAL);
        TestConfiguration configuration = new TestConfiguration();
        Properties properties = new Properties();
        properties.setProperty("xwikiPropertiesAdditionalProperties",
            String.format("index.sortCollation=%s", collation));
        configuration.setProperties(properties);
        globalStore.put(TestConfiguration.class, configuration);
    }
}
