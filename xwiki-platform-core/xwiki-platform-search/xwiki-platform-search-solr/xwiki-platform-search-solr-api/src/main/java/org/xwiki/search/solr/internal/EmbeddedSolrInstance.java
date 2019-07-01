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
package org.xwiki.search.solr.internal;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.input.CloseShieldInputStream;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.core.CoreContainer;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLifecycleException;
import org.xwiki.component.phase.Disposable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.environment.Environment;
import org.xwiki.search.solr.internal.api.SolrConfiguration;

/**
 * Embedded Solr instance running in the same JVM.
 * 
 * @version $Id$
 * @since 4.3M2
 */
@Component
@Named(EmbeddedSolrInstance.TYPE)
@Singleton
public class EmbeddedSolrInstance extends AbstractSolrInstance implements Disposable
{
    /**
     * Solr instance type for this implementation.
     */
    public static final String TYPE = "embedded";

    /**
     * Default directory name for Solr's configuration and index files.
     */
    public static final String DEFAULT_SOLR_DIRECTORY_NAME = "solr";

    /**
     * Solr configuration.
     */
    @Inject
    private SolrConfiguration solrConfiguration;

    /**
     * Environment used to get the xwiki permanent directory.
     */
    @Inject
    private Environment environment;

    /**
     * Solr CoreContainer.
     */
    private CoreContainer container;

    @Override
    public void initialize() throws InitializationException
    {
        String solrHome = determineHomeDirectory();
        try {
            // Validate and initialize the home directory if needed.
            validateAndInitializeHomeDirectory(solrHome);

            // Start embedded Solr server.
            this.logger.info("Starting embedded Solr server...");
            this.logger.info("Using Solr home directory: [{}]", solrHome);

            // Initialize the SOLR back-end using an embedded server.
            this.container = createCoreContainer(solrHome);
            // If we get here then there is at least one core found. We there are more, we use the first one.
            String coreName = this.container.getCores().iterator().next().getName();
            this.server = new EmbeddedSolrServer(container, coreName);

            this.logger.info("Started embedded Solr server.");
        } catch (Exception e) {
            throw new InitializationException(String.format(
                "Failed to initialize the Solr embedded server with home directory set to [%s]", solrHome), e);
        }
    }

    private CoreContainer createCoreContainer(String solrHome) throws SolrServerException
    {
        CoreContainer coreContainer = new CoreContainer(solrHome);
        coreContainer.load();
        if (coreContainer.getCores().isEmpty()) {
            throw new SolrServerException(
                "Failed to initialize the Solr core. Please check the configuration and log messages.");
        } else if (coreContainer.getCores().size() > 1) {
            this.logger.warn("Multiple Solr cores detected: {}. Using the first one.", coreContainer.getAllCoreNames());
        }
        return coreContainer;
    }

    @Override
    public void dispose() throws ComponentLifecycleException
    {
        if (this.server != null) {
            try {
                this.server.close();
            } catch (IOException e) {
                this.logger.error("Failed to close server", e);
            }
        }

        if (this.container != null) {
            this.container.shutdown();
        }
    }

    /**
     * Useful when testing.
     * 
     * @return the container
     */
    protected CoreContainer getContainer()
    {
        return this.container;
    }

    /**
     * Checks rights, creates paths and adds default config XML files if they don`t already exist.
     * 
     * @param solrHome the directory to use as Solr home.
     * @throws IllegalArgumentException if the provided directory is not usable (is a file, is not writable, etc.).
     * @throws IOException if the XML files are not copied successfully.
     */
    private void validateAndInitializeHomeDirectory(String solrHome) throws IllegalArgumentException, IOException
    {
        // Validate and create the directory if it does not already exist.
        File solrHomeDirectory = new File(solrHome);
        if (solrHomeDirectory.exists()) {
            // Exists but is unusable.
            if (!solrHomeDirectory.isDirectory() || !solrHomeDirectory.canWrite() || !solrHomeDirectory.canRead()) {
                throw new IllegalArgumentException(String.format(
                    "The given path [%s] must be a readable and writable directory", solrHomeDirectory));
            }
        } else {
            // Create the home directory
            if (!solrHomeDirectory.mkdirs()) {
                // Does not exist and can not be created.
                throw new IllegalArgumentException(String.format(
                    "The given path [%s] could not be created due to and invalid value %s", solrHomeDirectory,
                    "or to insufficient filesystem permissions"));
            }

            // Initialize the Solr Home with the default configuration files if the folder does not already exist.
            // Add the configuration files required by Solr.

            InputStream stream = this.solrConfiguration.getHomeDirectoryConfiguration();
            try (ZipInputStream zstream = new ZipInputStream(stream)) {
                for (ZipEntry entry = zstream.getNextEntry(); entry != null; entry = zstream.getNextEntry()) {
                    if (entry.isDirectory()) {
                        File destinationDirectory = new File(solrHomeDirectory, entry.getName());
                        destinationDirectory.mkdirs();
                    } else {
                        File destinationFile = new File(solrHomeDirectory, entry.getName());
                        FileUtils.copyInputStreamToFile(new CloseShieldInputStream(zstream), destinationFile);
                    }
                }
            }
        }
    }

    /**
     * @return the configured home directory location or the default value if no configuration is present.
     */
    private String determineHomeDirectory()
    {
        String defaultValue = getDefaultHomeDirectory();

        return this.solrConfiguration.getInstanceConfiguration(TYPE, "home", defaultValue);
    }

    /**
     * @return the default home directory located inside the environment's permanent directory.
     */
    String getDefaultHomeDirectory()
    {
        String result = new File(this.environment.getPermanentDirectory(), DEFAULT_SOLR_DIRECTORY_NAME).getPath();

        return result;
    }
}
