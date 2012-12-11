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

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.core.CoreContainer;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.environment.Environment;

/**
 * Embedded Solr instance running in the same JVM.
 * 
 * @version $Id$
 * @since 4.3M2
 */
@Component
@Named("embedded")
@InstantiationStrategy(ComponentInstantiationStrategy.SINGLETON)
public class EmbeddedSolrInstance extends AbstractSolrInstance
{
    /**
     * Classpath location pattern for the default configuration files.
     */
    protected static final String CONF_FILE_LOCATION_PATTERN = "/%s/%s";

    /**
     * Name of the default Solr configuration file.
     */
    protected static final String SOLRCONFIG_XML = "solrconfig.xml";

    /**
     * Name of the classpath folder where the default configuration files are located.
     */
    protected static final String CONF_DIRECTORY = "conf";

    /**
     * Name of the default schema configuration file.
     */
    protected static final String SCHEMA_XML = "schema.xml";

    /**
     * Name of the default Solr cores configuration file.
     */
    protected static final String SOLR_XML = "solr.xml";

    /**
     * SOLR HOME KEY.
     */
    protected static final String SOLR_HOME_KEY = "solr.solr.home";

    /**
     * Default directory name for Solr's configuration and index files.
     */
    protected static final String DEFAULT_SOLR_DIRECTORY_NAME = "solr";

    /**
     * Properties.
     */
    @Inject
    @Named("xwikiproperties")
    private ConfigurationSource configuration;

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
            System.setProperty(SOLR_HOME_KEY, solrHome);
            logger.info("Using Solr home directory: {}", solrHome);

            // Initialize the SOLR backend using an embedded server.
            CoreContainer.Initializer initializer = new CoreContainer.Initializer();
            CoreContainer initializedContainer = initializer.initialize();

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
        File solrHomeDir = new File(solrHome);
        boolean existsButIsUnusable = (solrHomeDir.exists() && (!solrHomeDir.isDirectory() || !solrHomeDir.canWrite()));
        if (existsButIsUnusable || (!solrHomeDir.exists() && !solrHomeDir.mkdirs())) {
            throw new IllegalArgumentException(String.format("The given path '%s' must be a writable directory",
                solrHomeDir));
        }

        // Initialize the Solr Home with the default schema.xml and solrconfig.xml if they don`t already exist.
        File confDirectory = new File(solrHomeDir, CONF_DIRECTORY);
        if (!confDirectory.exists()) {
            confDirectory.mkdir();
        }

        // Initialize the cores configuration file.
        copyFileIfNotExists(solrHomeDir, SOLR_XML);

        // Initialize configuration files in the conf directory of the default core.
        String[] fileNames =
        {SOLRCONFIG_XML, SCHEMA_XML, "protwords.txt", "stopwords.txt", "synonyms.txt", "elevate.xml"};
        for (String fileName : fileNames) {
            copyFileIfNotExists(confDirectory, fileName);
        }
    }

    /**
     * Copy a file from the set of default ones if it is not already present in the destination directory.
     * 
     * @param destinationDirectory directory where to copy the file.
     * @param fileName the name of the file to copy from the default configuration files.
     * @throws IOException if the copy operation fails.
     */
    private void copyFileIfNotExists(File destinationDirectory, String fileName) throws IOException
    {
        File destinationFile = new File(destinationDirectory, fileName);
        if (!destinationFile.exists()) {
            URL inputUrl = getClass().getResource(String.format(CONF_FILE_LOCATION_PATTERN, CONF_DIRECTORY, fileName));
            if (inputUrl != null) {
                FileUtils.copyURLToFile(inputUrl, destinationFile);
            }
        }
    }

    /**
     * @return the home directory determined from the {@value #SOLR_HOME_KEY} system property, configuration or default
     *         location (in that order).
     */
    private String determineHomeDirectory()
    {
        if (StringUtils.isNotBlank(System.getProperty(SOLR_HOME_KEY))) {
            return System.getProperty(SOLR_HOME_KEY);
        }

        String defaultValue = getDefaultHomeDirectory();

        return configuration.getProperty("search.solr.home", defaultValue);
    }

    /**
     * @return the default home directory located inside the environment's permanent directory.
     */
    String getDefaultHomeDirectory()
    {
        String result = new File(environment.getPermanentDirectory(), DEFAULT_SOLR_DIRECTORY_NAME).getPath();

        return result;
    }

    @Override
    public void shutDown()
    {
        if (this.server != null) {
            ((EmbeddedSolrServer) this.server).shutdown();
        }
    }
}
