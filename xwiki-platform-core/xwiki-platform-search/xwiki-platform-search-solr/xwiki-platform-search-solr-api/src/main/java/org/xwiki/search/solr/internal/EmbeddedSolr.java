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
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.input.CloseShieldInputStream;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.lucene.util.Version;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.core.CoreContainer;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.phase.Disposable;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.environment.Environment;
import org.xwiki.search.solr.SolrCoreInitializer;
import org.xwiki.search.solr.SolrException;
import org.xwiki.search.solr.internal.api.SolrConfiguration;

/**
 * Embedded Solr server running in the same JVM.
 * 
 * @version $Id$
 * @since 12.2RC1
 */
@Component
@Named(EmbeddedSolr.TYPE)
@Singleton
public class EmbeddedSolr extends AbstractSolr implements Disposable, Initializable
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

            this.logger.info("Started embedded Solr server.");
        } catch (Exception e) {
            throw new InitializationException(String
                .format("Failed to initialize the Solr embedded server with home directory set to [%s]", solrHome), e);
        }
    }

    private CoreContainer createCoreContainer(String solrHome) throws SolrServerException
    {
        CoreContainer coreContainer = new CoreContainer(solrHome);
        coreContainer.load();
        if (coreContainer.getCores().isEmpty()) {
            throw new SolrServerException(
                "Failed to initialize the Solr core. Please check the configuration and log messages.");
        }

        return coreContainer;
    }

    @Override
    protected SolrClient createSolrClient(String coreName) throws SolrException
    {
        return new EmbeddedSolrServer(this.container, coreName);
    }

    @Override
    public void dispose()
    {
        super.dispose();

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
            if (!isValid(solrHomeDirectory)) {
                // Recreate the home folder
                recreateHomeDirectory(solrHomeDirectory);
            }
        } else {
            createHomeDirectory(solrHomeDirectory);
        }

        // Create cores files
        this.logger.info("Initializing solr cores...");
        try {
            for (SolrCoreInitializer coreInitializer : this.componentManager
                .<SolrCoreInitializer>getInstanceList(SolrCoreInitializer.class)) {
                try {
                    initializeCore(coreInitializer, solrHomeDirectory);
                } catch (Exception e) {
                    this.logger.error("Failed to initialize a Solr core", e);
                }
            }
        } catch (ComponentLookupException e) {
            this.logger.error("Failed to initialize Solr cores", e);
        }
    }

    private void initializeCore(SolrCoreInitializer coreInitializer, File solrHomeDirectory) throws IOException
    {
        String coreName = coreInitializer.getCoreName();

        this.logger.info("  Initializing solr core [{}]...", coreName);

        File coreDirectory = new File(solrHomeDirectory, coreName);

        if (!coreDirectory.exists()) {
            this.logger.info("  The core [{}] does not exist, creating it", coreName);

            // Create the core directory
            coreDirectory.mkdir();

            // Create the minimum core content
            File corePropertiesFile = new File(coreDirectory, "core.properties");
            corePropertiesFile.createNewFile();

            FileUtils.write(new File(coreDirectory, "solrconfig.xml"),
                "<config><luceneMatchVersion>" + Version.LATEST + "</luceneMatchVersion></config>",
                StandardCharsets.UTF_8);

            FileUtils.write(new File(coreDirectory, "managed-schema"),
                "<schema name=\"" + coreName + "\" version=\"1.6\">"
                    + "<field name=\"id\" type=\"string\" required=\"true\" indexed=\"true\" stored=\"true\" />"
                    + "<uniqueKey>id</uniqueKey>"
                    + "<fieldType name=\"string\" class=\"solr.StrField\" sortMissingLast=\"true\" docValues=\"true\"/>"
                    + "</schema>",
                StandardCharsets.UTF_8);

            new File(coreDirectory, "conf").mkdir();

            this.logger.info("  The core [{}] has been created", coreName);
        } else {
            this.logger.debug("  The core [{}] already exist", coreName);
        }
    }

    private boolean isExpectedSolrVersion(File solrconfigFile)
    {
        XMLInputFactory factory = XMLInputFactory.newInstance();

        try (FileReader reader = new FileReader(solrconfigFile)) {
            XMLStreamReader xmlReader = factory.createXMLStreamReader(reader);

            for (xmlReader.nextTag(); xmlReader.isStartElement(); xmlReader.nextTag()) {
                if (xmlReader.getLocalName().equals("luceneMatchVersion")) {
                    String versionStr = xmlReader.getElementText();

                    // Valid if the version used in the configuration is the currently bundled version
                    return Version.LATEST.toString().equals(versionStr);
                }
            }
        } catch (Exception e) {
            this.logger.warn("Failed to parse Solr configuration at [{}]: {}", solrconfigFile,
                ExceptionUtils.getRootCauseMessage(e));
        }

        // Not the right version or invalid configuration
        return false;
    }

    private boolean isValid(File solrHomeDirectory)
    {
        // Exists but is unusable.
        if (!solrHomeDirectory.isDirectory() || !solrHomeDirectory.canWrite() || !solrHomeDirectory.canRead()) {
            throw new IllegalArgumentException(
                String.format("The given path [%s] must be a readable and writable directory", solrHomeDirectory));
        }

        // Check solrconfig.xml
        File solrconfigFile = new File(solrHomeDirectory, "xwiki/conf/solrconfig.xml");
        return solrconfigFile.exists() && isExpectedSolrVersion(solrconfigFile);
    }

    private void recreateHomeDirectory(File solrHomeDirectory) throws IOException
    {
        // Archive solr home
        if (solrHomeDirectory.exists()) {
            File newDirectory = archiveHomeDirectory(solrHomeDirectory);

            this.logger.warn("The Solr home directory at [{}] is invalid. Archiving it at [{}] and creating a new one.",
                solrHomeDirectory, newDirectory);
        }

        // Recreate
        createHomeDirectory(solrHomeDirectory);
    }

    private File archiveHomeDirectory(File solrHomeDirectory) throws IOException
    {
        // Append the date to the home directory to archive it
        File archiveDirectory = solrHomeDirectory.getParentFile();
        archiveDirectory = new File(archiveDirectory, solrHomeDirectory.getName() + "-" + new Date().getTime());

        FileUtils.moveDirectoryToDirectory(solrHomeDirectory, archiveDirectory, true);

        return archiveDirectory;
    }

    private void createHomeDirectory(File solrHomeDirectory) throws IOException
    {
        // Create the home directory
        if (!solrHomeDirectory.mkdirs()) {
            // Does not exist and can not be created.
            throw new IllegalArgumentException(String.format("The given path [%s] could not be created due to an "
                + "invalid value or to insufficient filesystem permissions", solrHomeDirectory));
        }

        // Initialize the Solr Home with the default configuration files if the folder does not already exist.
        // Add the configuration files required by Solr.

        this.logger.info("Generating a new Solr home directory at [{}]", solrHomeDirectory);

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

    /**
     * @return the configured home directory location or the default value if no configuration is present.
     */
    private String determineHomeDirectory()
    {
        String defaultValue = getDefaultHomeDirectory();

        return this.solrConfiguration.getInstanceConfiguration(EmbeddedSolr.TYPE, "home", defaultValue);
    }

    /**
     * @return the default home directory located inside the environment's permanent directory.
     */
    String getDefaultHomeDirectory()
    {
        return new File(this.environment.getPermanentDirectory(), DEFAULT_SOLR_DIRECTORY_NAME).getPath();
    }
}
