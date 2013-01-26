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
import java.net.URL;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.core.CoreContainer;
import org.xwiki.component.annotation.Component;
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
public class EmbeddedSolrInstance extends AbstractSolrInstance
{
    /**
     * Solr instance type for this implementation.
     */
    public static final String TYPE = "embedded";

    /**
     * Solr home directory system property.
     */
    public static final String SOLR_HOME_SYSTEM_PROPERTY = "solr.solr.home";

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

    @Override
    public void initialize() throws InitializationException
    {
        String solrHome = determineHomeDirectory();
        try {
            // Validate and initialize the home directory if needed.
            validateAndInitializeHomeDirectory(solrHome);

            // Start embedded Solr server.
            logger.info("Starting embedded Solr server...");
            System.setProperty(SOLR_HOME_SYSTEM_PROPERTY, solrHome);
            logger.info("Using Solr home directory: {}", solrHome);

            // Initialize the SOLR back-end using an embedded server.
            CoreContainer.Initializer initializer = new CoreContainer.Initializer();
            CoreContainer initializedContainer = initializer.initialize();
            if (initializedContainer.getCores().size() == 0) {
                throw new SolrServerException(
                    "Failed to initialize the Solr core. Please check the configuration and log messages");
            }

            container = initializedContainer;
            server = new EmbeddedSolrServer(container, "");

            logger.info("Started embedded Solr server.");
        } catch (Exception e) {
            throw new InitializationException(String.format(
                "Failed to initialize the solr embedded server with home directory set to '%s'", solrHome), e);
        }
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
                    "The given path '%s' must be a readable and writable directory", solrHomeDirectory));
            }
        } else {
            // Create the home directory
            if (!solrHomeDirectory.mkdirs()) {
                // Does not exist and can not be created.
                throw new IllegalArgumentException(String.format(
                    "The given path '%s' could not be created due to insufficient filesystem permissions",
                    solrHomeDirectory));
            }

            // Initialize the Solr Home with the default configuration files if the folder does not already exist.
            // Add the configuration files required by Solr.
            Map<String, URL> homeDirectoryConfiguration = this.solrConfiguration.getHomeDirectoryConfiguration();
            for (Map.Entry<String, URL> file : homeDirectoryConfiguration.entrySet()) {
                File destinationFile = new File(solrHomeDirectory, file.getKey());
                FileUtils.copyURLToFile(file.getValue(), destinationFile);
            }
        }
    }

    /**
     * @return the home directory determined from the {@value #SOLR_HOME_SYSTEM_PROPERTY} system property, configuration
     *         or default location (in that order).
     */
    private String determineHomeDirectory()
    {
        if (StringUtils.isNotBlank(System.getProperty(SOLR_HOME_SYSTEM_PROPERTY))) {
            return System.getProperty(SOLR_HOME_SYSTEM_PROPERTY);
        }

        String defaultValue = getDefaultHomeDirectory();

        return solrConfiguration.getInstanceConfiguration(TYPE, "home", defaultValue);
    }

    /**
     * @return the default home directory located inside the environment's permanent directory.
     */
    String getDefaultHomeDirectory()
    {
        String result = new File(environment.getPermanentDirectory(), DEFAULT_SOLR_DIRECTORY_NAME).getPath();

        return result;
    }
}
