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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
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
import org.apache.solr.core.SolrCore;
import org.apache.solr.core.SolrResourceLoader;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.DisposePriority;
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
 * @since 12.2
 */
@Component
@Named(EmbeddedSolr.TYPE)
@Singleton
// Make sure the Solr store is disposed at the end in case some components needs it for their own dispose
@DisposePriority(10000)
public class EmbeddedSolr extends AbstractSolr implements Disposable, Initializable
{
    /**
     * Solr instance type for this implementation.
     */
    public static final String TYPE = "embedded";

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

    private CoreContainer container;

    private Path solrHomePath;

    private Path solrSearchCorePath;

    @Override
    public void initialize() throws InitializationException
    {
        this.solrHomePath = Paths.get(this.solrConfiguration.getHomeDirectory());
        this.solrSearchCorePath = this.solrHomePath.resolve(SolrClientInstance.CORE_NAME);

        try {
            // Create the Solr home if it does not already exist
            if (!Files.exists(this.solrHomePath)) {
                createHomeDirectory();
            }

            // Validate and create the search core
            if (Files.exists(this.solrSearchCorePath)) {
                // Make sure the core setup is up to date
                if (!isSearchCoreValid()) {
                    // Recreate the home folder
                    recreateSearchCore();
                }
            } else {
                createSearchCore();
            }

            // Start embedded Solr server.
            this.logger.info("Starting embedded Solr server...");
            this.logger.info("Using Solr home directory: [{}]", this.solrHomePath);

            // Initialize the SOLR back-end using an embedded server.
            this.container = createCoreContainer();

            this.logger.info("Started embedded Solr server.");
        } catch (Exception e) {
            throw new InitializationException(String.format(
                "Failed to initialize the Solr embedded server with home directory set to [%s]", this.solrHomePath), e);
        }
    }

    private CoreContainer createCoreContainer() throws SolrServerException
    {
        CoreContainer coreContainer = new CoreContainer(new SolrResourceLoader(this.solrHomePath));
        coreContainer.load();
        if (coreContainer.getCores().isEmpty()) {
            throw new SolrServerException(
                "Failed to initialize the Solr core. Please check the configuration and log messages.");
        }

        return coreContainer;
    }

    @Override
    protected SolrClient getInternalSolrClient(String coreName) throws SolrException
    {
        SolrCore core = this.container.getCore(coreName);

        return core != null ? new EmbeddedSolrServer(core) : null;
    }

    @Override
    protected SolrClient createCore(SolrCoreInitializer initializer) throws SolrException
    {
        // Prepare the filesystem
        // TODO: get rid of that we we find out how to have Solr fully create the core as it should...
        Path corePath;
        try {
            corePath = prepareCore(initializer);
        } catch (IOException e) {
            throw new SolrException("Failed to prepare the Solr core storage", e);
        }

        Map<String, String> parameters = new HashMap<>();

        // Indicate the path of the data
        if (initializer.isCache()) {
            parameters.put("dataDir", getCacheCoreDataDir(corePath, initializer.getCoreName()).toString());
        }

        // Create the actual core
        SolrCore core = this.container.create(initializer.getCoreName(), parameters);

        // Return a usable SolrClient instance
        return new EmbeddedSolrServer(core);
    }

    private Path prepareCore(SolrCoreInitializer initializer) throws IOException
    {
        Path corePath = this.container.getConfig().getCoreRootDirectory().resolve(initializer.getCoreName());

        // Create the core directory
        Files.createDirectory(corePath);

        // Copy configuration
        try (InputStream stream = this.solrConfiguration.getMinimalCoreDefaultContent()) {
            copyCoreConfiguration(stream, corePath, true);
        }

        return corePath;
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

    private boolean isSearchCoreValid()
    {
        // Exists but is unusable.
        if (!Files.isDirectory(this.solrSearchCorePath) || !Files.isWritable(this.solrSearchCorePath)
            || !Files.isReadable(this.solrSearchCorePath)) {
            throw new IllegalArgumentException(String
                .format("The given path [%s] must be a readable and writable directory", this.solrSearchCorePath));
        }

        // Check solrconfig.xml
        File solrconfigFile = this.solrSearchCorePath.resolve("conf/solrconfig.xml").toFile();
        return solrconfigFile.exists() && isExpectedSolrVersion(solrconfigFile);
    }

    private void recreateSearchCore() throws IOException
    {
        // Delete search core
        if (Files.exists(this.solrSearchCorePath)) {
            this.logger.warn("The Solr search core directory at [{}] is invalid. Deleting it and creating a new one.",
                this.solrSearchCorePath);

            // Delete the core directory
            FileUtils.deleteDirectory(this.solrSearchCorePath.toFile());

            // Delete the data directory
            FileUtils.deleteDirectory(resolveCacheCoreDataPath(SolrClientInstance.CORE_NAME).toFile());
        }

        // Recreate
        createSearchCore();
    }

    private void createHomeDirectory() throws IOException
    {
        // Initialize the Solr Home with the default configuration files if the folder does not already exist.
        // Add the configuration files required by Solr.

        this.logger.info("Generating a new Solr home directory at [{}]", this.solrHomePath);

        // Create the home directory
        Files.createDirectories(this.solrHomePath);

        // Copy the default solr.xml configuration file
        FileUtils.write(this.solrHomePath.resolve("solr.xml").toFile(), "<solr/>", StandardCharsets.UTF_8);

        // [RETRO COMPATIBILITY for < 12.3]
        // Check if the solr home is not already at the old location (/solr) and move things
        File oldHome = new File(this.environment.getPermanentDirectory(), "solr");
        if (oldHome.exists()) {
            // Move old cores to the new location
            for (File file : oldHome.listFiles()) {
                // We don't care about the "xwiki" core since it needs to be recreated anyway
                if (file.isDirectory() && !file.getName().equals("xwiki") && !file.getName().equals("META-INF")) {
                    // Move the folder in the new location
                    FileUtils.moveDirectoryToDirectory(file, this.solrHomePath.toFile(), false);
                }
            }
        }
    }

    private void copyCoreConfiguration(InputStream stream, Path corePath, boolean skipCoreProperties) throws IOException
    {
        try (ZipInputStream zstream = new ZipInputStream(stream)) {
            for (ZipEntry entry = zstream.getNextEntry(); entry != null; entry = zstream.getNextEntry()) {
                Path targetPath = corePath.resolve(entry.getName());
                if (entry.isDirectory()) {
                    Files.createDirectories(targetPath);
                } else if (!skipCoreProperties || !entry.getName().equals("core.properties")) {
                    FileUtils.copyInputStreamToFile(new CloseShieldInputStream(zstream), targetPath.toFile());
                }
            }
        }
    }

    private void createSearchCore() throws IOException
    {
        // Copy configuration
        copyCoreConfiguration(this.solrConfiguration.getSearchCoreDefaultContent(), this.solrSearchCorePath, false);

        // Indicate the path of the data
        createCacheCore(this.solrSearchCorePath, SolrClientInstance.CORE_NAME);
    }

    private void createCacheCore(Path corePath, String core) throws IOException
    {
        // Indicate the path of the data
        Path dataPath = getCacheCoreDataDir(corePath, core);
        FileUtils.write(corePath.resolve("core.properties").toFile(), "dataDir=" + dataPath, StandardCharsets.UTF_8,
            true);
    }

    private Path getCacheCoreDataDir(Path corePath, String core)
    {
        return corePath.relativize(resolveCacheCoreDataPath(core));

    }

    private Path resolveCacheCoreDataPath(String core)
    {
        return this.environment.getPermanentDirectory().toPath().resolve("cache/solr/" + core);
    }
}
