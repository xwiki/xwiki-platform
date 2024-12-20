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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.input.CloseShieldInputStream;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.lucene.util.Version;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.CoreContainer.CoreLoadFailure;
import org.apache.solr.core.CoreDescriptor;
import org.apache.solr.core.SolrCore;
import org.apache.solr.servlet.SolrDispatchFilter;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.DisposePriority;
import org.xwiki.component.phase.Disposable;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.environment.Environment;
import org.xwiki.search.solr.AbstractSolrCoreInitializer;
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

    private static final String SOLRCONFIG_PATH = "conf/solrconfig.xml";

    private static final String SCHEMA_PATH = "conf/managed-schema.xml";

    private static final long SEARCH_CORE_SCHEMA_VERSION = AbstractSolrCoreInitializer.SCHEMA_VERSION_16_6;
    
    private static final String CORE_PROPERTIES_FILENAME = "core.properties";
    
    private static final String DATA_DIR_PROPERTY = "DataDir";

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
        this.solrHomePath = Paths.get(this.solrConfiguration.getHomeDirectory()).toAbsolutePath();
        this.solrSearchCorePath = this.solrHomePath.resolve(toSolrCoreName(SolrClientInstance.CORE_NAME));

        try {
            // Create the Solr home if it does not already exist
            if (!Files.exists(this.solrHomePath)) {
                createHomeDirectory();
            } else {
                updateHomeDirectory();
            }

            // Solr expects the Solr home path to be indicated as system property
            System.setProperty(SolrDispatchFilter.SOLR_INSTALL_DIR_ATTRIBUTE, this.solrHomePath.toString());

            // Validate and create the search core
            if (Files.exists(this.solrSearchCorePath)) {
                // Make sure the core setup is up to date
                if (!isSearchCoreValid()) {
                    // Recreate the home folder
                    recreateSearchCore();
                }
            } else {
                // Remove search cores from previous locations
                Path pre1601Path = this.solrHomePath.resolve(SolrClientInstance.CORE_NAME);
                if (Files.exists(pre1601Path)) {
                    FileUtils.deleteDirectory(pre1601Path.toFile());
                }

                // Create the new search core
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
        CoreContainer coreContainer = XWikiCoreContainer.createAndLoad(this.solrHomePath);
        Map<String, CoreLoadFailure> failures = coreContainer.getCoreInitFailures();
        if (MapUtils.isNotEmpty(failures)) {
            for (Map.Entry<String, CoreLoadFailure> failure : failures.entrySet()) {
                this.logger.error("Failed to initialize Solr core with id [{}]", failure.getKey(),
                    failure.getValue().exception);
            }

            if (coreContainer.getLoadedCoreNames().isEmpty()) {
                throw new SolrServerException(
                    "Failed to initialize the Solr core. Please check previous log messages.");
            }
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
    protected SolrClient createSolrClient(String solrCoreName, boolean isCache) throws SolrException
    {
        // Prepare the filesystem
        // TODO: get rid of that when we find out how to have Solr fully create the core as it should...
        Path corePath;
        try {
            corePath = prepareCore(solrCoreName);
        } catch (IOException e) {
            throw new SolrException("Failed to prepare the Solr core storage", e);
        }

        Map<String, String> parameters = new HashMap<>();

        // Indicate the path of the data
        if (isCache) {
            parameters.put(CoreDescriptor.CORE_DATADIR, getCacheCoreDataDir(corePath, solrCoreName).toString());
        }

        // Don't load the core on startup to workaround a possible dead lock during Solr init
        parameters.put(CoreDescriptor.CORE_LOADONSTARTUP, "false");

        // Create the actual core
        SolrCore core = this.container.create(solrCoreName, parameters);

        // Return a usable SolrClient instance
        return new EmbeddedSolrServer(core);
    }

    private Path prepareCore(String solrCoreName) throws IOException
    {
        Path corePath = this.container.getConfig().getCoreRootDirectory().resolve(solrCoreName);

        // Create the core directory
        Files.createDirectory(corePath);

        // Copy configuration
        try (InputStream stream = this.solrConfiguration.getMinimalCoreDefaultContent()) {
            copyCoreConfiguration(stream, corePath, true, null);
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

    @Override
    protected int getSolrMajorVersion()
    {
        return Version.LATEST.major;
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

    private Version getLuceneVersion(File solrconfigFile)
    {
        XMLInputFactory factory = XMLInputFactory.newInstance();
        // Prevent any XXE attack by disabling DOCTYPE declarations (even though we control the solr config file and
        // thus the risk is almost non-existent. The user would need to find a way to replace it).
        // Note that all solrconfig files checked didn't contain any DOCTYPE so that should be good.
        // This will also prevent SonarQube from complaining every time this file is modified.
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);

        try (FileReader reader = new FileReader(solrconfigFile)) {
            XMLStreamReader xmlReader = factory.createXMLStreamReader(reader);

            for (xmlReader.nextTag(); xmlReader.isStartElement(); xmlReader.nextTag()) {
                if (xmlReader.getLocalName().equals("luceneMatchVersion")) {
                    return Version.parse(xmlReader.getElementText());
                }
            }
        } catch (Exception e) {
            this.logger.warn("Failed to parse Solr configuration at [{}]: {}", solrconfigFile,
                ExceptionUtils.getRootCauseMessage(e));
        }

        // Not the right version or invalid configuration
        return null;
    }

    private long getCoreVersion(File schemaFile)
    {
        XMLInputFactory factory = XMLInputFactory.newInstance();
        // Prevent any XXE attack by disabling DOCTYPE declarations (even though we control the solr config file and
        // thus the risk is almost non-existent. The user would need to find a way to replace it).
        // Note that all solrconfig files checked didn't contain any DOCTYPE so that should be good.
        // This will also prevent SonarQube from complaining every time this file is modified.
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);

        try (FileReader reader = new FileReader(schemaFile)) {
            XMLStreamReader xmlReader = factory.createXMLStreamReader(reader);

            for (xmlReader.nextTag(); xmlReader.isStartElement(); xmlReader.nextTag()) {
                if (xmlReader.getLocalName().equals("fieldType")
                    && SolrSchemaUtils.SOLR_TYPENAME_CVERSION.equals(xmlReader.getAttributeValue(null, "name"))) {
                    String version = xmlReader.getAttributeValue(null, SolrSchemaUtils.SOLR_VERSIONFIELDTYPE_VALUE);
                    if (version != null) {
                        return NumberUtils.createLong(version);
                    }

                    break;
                }
            }
        } catch (Exception e) {
            this.logger.warn("Failed to parse Solr configuration at [{}]: {}", schemaFile,
                ExceptionUtils.getRootCauseMessage(e));
        }

        // Not the right version or invalid configuration
        return -1;
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
        File solrconfigFile = this.solrSearchCorePath.resolve(SOLRCONFIG_PATH).toFile();
        if (!solrconfigFile.exists() || !Version.LATEST.equals(getLuceneVersion(solrconfigFile))) {
            return false;
        }

        // Check the version of the schema
        File schemaFile = this.solrSearchCorePath.resolve(SCHEMA_PATH).toFile();
        if (!schemaFile.exists() || SEARCH_CORE_SCHEMA_VERSION > getCoreVersion(schemaFile)) {
            return false;
        }

        // Everything seems to have as expected
        return true;
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
            FileUtils.deleteDirectory(resolveCacheCoreDataPath(toSolrCoreName(SolrClientInstance.CORE_NAME)).toFile());
        }

        // Recreate
        createSearchCore();
    }

    private void writeHomeConfiguration() throws IOException
    {
        StringBuilder builder = new StringBuilder();
        builder.append("<solr>");
        // Disable the log watcher until Solr support SLF4J 2
        // FIXME: remove when Solr upgrade SLF4J (or stop logging a stack trace at least)
        builder.append("<logging><str name=\"enabled\">false</str></logging>");
        builder.append("</solr>");
        FileUtils.write(this.solrHomePath.resolve("solr.xml").toFile(), builder.toString(), StandardCharsets.UTF_8);
    }

    private void createHomeDirectory() throws IOException
    {
        // Initialize the Solr Home with the default configuration files if the folder does not already exist.
        // Add the configuration files required by Solr.

        this.logger.info("Generating a new Solr home directory at [{}]", this.solrHomePath);

        // Create the home directory
        Files.createDirectories(this.solrHomePath);

        // Write the default solr.xml configuration file
        writeHomeConfiguration();

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

    private void updateHomeDirectory() throws IOException
    {
        // Make sure the Solr Home contains expected configuration

        this.logger.info("Updating Solr home directory at [{}]", this.solrHomePath);

        // Reset the solr.xml configuration file
        writeHomeConfiguration();

        // Make sure cores have the expected configuration
        try (Stream<Path> stream = Files.list(this.solrHomePath)) {
            stream.filter(Files::isDirectory).forEach(this::updateCore);
        }
    }

    private void updateCore(Path corePath)
    {
        try {
            if (this.componentManager.hasComponent(SolrCoreInitializer.class,
                toXWikiCoreName(corePath.getFileName().toString()))) {
                Path solrconfig = corePath.resolve(SOLRCONFIG_PATH);

                // If Solr was upgraded, reset the solrconfig.xml
                if (Files.exists(solrconfig)) {
                    Version luceneVersion = getLuceneVersion(solrconfig.toFile());

                    // But only if it's the same major version (otherwise it needs to be migrated to a totally different
                    // core)
                    if (luceneVersion == null || getSolrMajorVersion() == luceneVersion.major) {
                        // Reset solr configuration
                        try (InputStream stream = this.solrConfiguration.getMinimalCoreDefaultContent()) {
                            copyCoreConfiguration(stream, corePath, true, Set.of(SOLRCONFIG_PATH));
                        }
                    }
                }
            }
        } catch (Exception e) {
            this.logger.error("Failed to update Solr core located at [{}]", corePath, e);
        }
    }

    private void copyCoreConfiguration(InputStream stream, Path corePath, boolean skipCoreProperties, Set<String> force)
        throws IOException
    {
        try (ZipInputStream zstream = new ZipInputStream(stream)) {
            for (ZipEntry entry = zstream.getNextEntry(); entry != null; entry = zstream.getNextEntry()) {
                Path targetPath = corePath.resolve(entry.getName());
                if (entry.isDirectory()) {
                    Files.createDirectories(targetPath);
                } else if ((force != null && force.contains(entry.getName())) || (!Files.exists(targetPath)
                    && (!skipCoreProperties || !entry.getName().equals(CORE_PROPERTIES_FILENAME)))) {
                    FileUtils.copyInputStreamToFile(CloseShieldInputStream.wrap(zstream), targetPath.toFile());
                }
            }
        }
    }

    private void createSearchCore() throws IOException
    {
        // Copy configuration
        copyCoreConfiguration(this.solrConfiguration.getSearchCoreDefaultContent(), this.solrSearchCorePath, false,
            null);

        // Indicate the path of the data
        createCacheCore(this.solrSearchCorePath, toSolrCoreName(SolrClientInstance.CORE_NAME));
    }

    private void createCacheCore(Path corePath, String solrCoreName) throws IOException
    {
        // Indicate the path of the data
        Path dataDir = getCacheCoreDataDir(corePath, solrCoreName);
        File corePropertiesFile = corePath.resolve(CORE_PROPERTIES_FILENAME).toFile();
        Properties coreProperties = new Properties();
        // we used to append this property to this file.
        // I'm not sure any other properties are ever in here, but jic let's be passive
        // So I load the existing file if it exists, and add my property.
        if(corePropertiesFile.exists()) 
        {
        	try(BufferedReader in = new BufferedReader(new FileReader(corePropertiesFile, StandardCharsets.UTF_8)) )
        	{
        		coreProperties.load(in);
        	}
        }
        coreProperties.setProperty(DATA_DIR_PROPERTY, dataDir.toString());
        // Normally we write using the Apache Commons FileUtils, but this is a properties file. 
        // Use the standard library Properties class writes it in the proper format.
        try(PrintWriter out = new PrintWriter(corePropertiesFile, StandardCharsets.UTF_8); )
        {
        	coreProperties.store(out, "");
        }
    }

    private Path getCacheCoreDataDir(Path corePath, String solrCoreName)
    {
        return corePath.relativize(resolveCacheCoreDataPath(solrCoreName));

    }

    private Path resolveCacheCoreDataPath(String solrCoreName)
    {
        return this.environment.getPermanentDirectory().toPath().resolve("cache/solr/" + solrCoreName).toAbsolutePath();
    }
}
