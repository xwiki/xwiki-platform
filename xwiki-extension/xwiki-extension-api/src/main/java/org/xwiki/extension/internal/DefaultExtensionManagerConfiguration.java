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
package org.xwiki.extension.internal;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.logging.AbstractLogEnabled;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.extension.ExtensionManagerConfiguration;
import org.xwiki.extension.repository.ExtensionRepositoryId;

@Component
public class DefaultExtensionManagerConfiguration extends AbstractLogEnabled implements ExtensionManagerConfiguration
{
    private static final String USERHOME = System.getProperty("user.home");

    private static final File XWIKIHOME = new File(USERHOME, ".xwiki");

    private static final File EXTENSIONSHOME = new File(XWIKIHOME, "extensions");

    private static final Pattern REPOSITORYIDPATTERN = Pattern.compile("([^:]+):([^:]+):(.+)");

    @Requirement("xwikiproperties")
    private ConfigurationSource configurationSource;

    // Cache

    private File localRepository;

    public File getLocalRepository()
    {
        if (this.localRepository == null) {
            String localRepositoryPath = this.configurationSource.getProperty("extension.localRepository");

            if (localRepositoryPath == null) {
                this.localRepository = new File(EXTENSIONSHOME, "repository/");
            } else {
                this.localRepository = new File(localRepositoryPath);
            }
        }

        return this.localRepository;
    }

    public List<ExtensionRepositoryId> getRepositories()
    {
        List<ExtensionRepositoryId> repositories = new ArrayList<ExtensionRepositoryId>();

        List<String> repositoryStrings =
            this.configurationSource.getProperty("extension.repositories", Collections.<String> emptyList());

        if (repositoryStrings != null && !repositoryStrings.isEmpty()) {
            for (String repositoryString : repositoryStrings) {
                try {
                    ExtensionRepositoryId extensionRepositoryId = parseRepository(repositoryString);
                    repositories.add(extensionRepositoryId);
                } catch (Exception e) {
                    getLogger().warn("Faild to parse repository [" + repositoryString + "] from configuration", e);
                }
            }
        } else {
            try {
                repositories.add(new ExtensionRepositoryId("xwiki-releases", "maven", new URI(
                    "http://maven.xwiki.org/releases/")));
                repositories.add(new ExtensionRepositoryId("central", "maven",
                    new URI("http://repo1.maven.org/maven2/")));
            } catch (Exception e) {
                // Should never happen
            }
        }

        return repositories;
    }

    private ExtensionRepositoryId parseRepository(String repositoryString) throws URISyntaxException
    {
        Matcher matcher = REPOSITORYIDPATTERN.matcher(repositoryString);

        if (matcher.matches()) {
            new ExtensionRepositoryId(matcher.group(1), matcher.group(2), new URI(matcher.group(3)));
        }

        // TODO: throw exception
        return null;
    }
}
