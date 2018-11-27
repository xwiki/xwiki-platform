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
package org.xwiki.test.docker.junit5;

import java.util.Arrays;
import java.util.List;

import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.impl.RemoteRepositoryManager;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;

/**
 * Handle Repository-related code.
 *
 * @version $Id$
 * @since 10.9
 */
public class RepositoryResolver
{
    private static final String DEFAULT_REPO_TYPE = "default";

    private static final RepositoryPolicy REPOSITORY_POLICY = new RepositoryPolicy(true, "never", "warn");

    private RemoteRepositoryManager remoteRepositoryManager;

    private RepositorySystem system;

    private RepositorySystemSession systemSession;

    private List<RemoteRepository> repositories;

    private TestConfiguration testConfiguration;

    /**
     * @param testConfiguration the configuration to build (database, debug mode, etc)
     */
    public RepositoryResolver(TestConfiguration testConfiguration)
    {
        this.testConfiguration = testConfiguration;
        this.system = newRepositorySystem();
        this.repositories = newRemoteRepositories();
        this.systemSession = newSession(this.system);
    }

    /**
     * @return the repository manager
     */
    public RemoteRepositoryManager getRemoteRepositoryManager()
    {
        return this.remoteRepositoryManager;
    }

    /**
     * @return the repository system
     */
    public RepositorySystem getSystem()
    {
        return this.system;
    }

    /**
     * @return the repository session
     */
    public RepositorySystemSession getSession()
    {
        return this.systemSession;
    }

    /**
     * @return the list of Maven repositories from which to resolve artifacts from
     */
    public List<RemoteRepository> getRepositories()
    {
        return this.repositories;
    }

    private RepositorySystem newRepositorySystem()
    {
        DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
        locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
        locator.addService(TransporterFactory.class, FileTransporterFactory.class);
        locator.addService(TransporterFactory.class, HttpTransporterFactory.class);

        this.remoteRepositoryManager = locator.getService(RemoteRepositoryManager.class);

        return locator.getService(RepositorySystem.class);
    }

    private RepositorySystemSession newSession(RepositorySystem system)
    {
        // If the local repository is configured when calling Maven, we should use the configured repo.
        // This is done by passing the maven.repo.local system property.
        String localRepoLocation = System.getProperty("maven.repo.local",
            String.format("%s/.m2/repository", System.getProperty("user.home")));
        DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
        LocalRepository localRepo = new LocalRepository(localRepoLocation);
        session.setLocalRepositoryManager(system.newLocalRepositoryManager(session, localRepo));

        if (this.testConfiguration != null && this.testConfiguration.isOffline()) {
            session.setOffline(true);
        }

        return session;
    }

    private List<RemoteRepository> newRemoteRepositories()
    {
        RemoteRepository mavenCentral = new RemoteRepository.Builder(
            "central", DEFAULT_REPO_TYPE, "http://repo1.maven.org/maven2/")
            .setPolicy(REPOSITORY_POLICY)
            .build();
        RemoteRepository mavenXWiki = new RemoteRepository.Builder(
            "xwiki", DEFAULT_REPO_TYPE, "http://nexus.xwiki.org/nexus/content/groups/public")
            .setPolicy(REPOSITORY_POLICY)
            .build();
        RemoteRepository mavenXWikiSnapshot = new RemoteRepository.Builder(
            // Note: we make sure to use the same id as the one used my Maven in the hope to have more up to date
            // metadata (in maven-metadata-xwiki-snapshots.xml).
            "xwiki-snapshots", DEFAULT_REPO_TYPE, "http://nexus.xwiki.org/nexus/content/groups/public-snapshots/")
            .setPolicy(REPOSITORY_POLICY)
            .build();

        return Arrays.asList(mavenXWiki, mavenCentral, mavenXWikiSnapshot);
    }
}
