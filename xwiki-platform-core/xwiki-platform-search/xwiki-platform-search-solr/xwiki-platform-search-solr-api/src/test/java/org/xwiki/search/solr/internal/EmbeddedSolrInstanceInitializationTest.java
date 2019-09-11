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
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.input.CloseShieldInputStream;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrCore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.environment.Environment;
import org.xwiki.search.solr.internal.api.SolrConfiguration;
import org.xwiki.search.solr.internal.api.SolrInstance;
import org.xwiki.test.annotation.AfterComponent;
import org.xwiki.test.annotation.AllComponents;
import org.xwiki.test.junit5.XWikiTempDir;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.mockito.MockitoComponentManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Test the initialization of the Solr instance.
 * 
 * @version $Id$
 */
@AllComponents
@ComponentTest
public class EmbeddedSolrInstanceInitializationTest
{
    private final static String SOLRHOME_PROPERTY =
        String.format("%s.%s.%s", "solr", EmbeddedSolrInstance.TYPE, "home");

    @XWikiTempDir
    private File permanentDirectory;

    private ConfigurationSource mockXWikiProperties;

    private Environment mockEnvironment;

    @InjectComponentManager
    private MockitoComponentManager componentManager;

    @AfterComponent
    public void afterComponent() throws Exception
    {
        this.mockXWikiProperties =
            this.componentManager.registerMockComponent(ConfigurationSource.class, "xwikiproperties");
        this.mockEnvironment = this.componentManager.registerMockComponent(Environment.class);
    }

    @BeforeEach
    public void beforeEach() throws Exception
    {
        when(this.mockEnvironment.getPermanentDirectory()).thenReturn(this.permanentDirectory);

        FileUtils.deleteDirectory(this.permanentDirectory);
        this.permanentDirectory.mkdirs();
    }

    /**
     * TODO DOCUMENT ME!
     * 
     * @param expected
     * @throws ComponentLookupException
     * @throws Exception
     */
    private void getInstanceAndAssertHomeDirectory(String expected) throws ComponentLookupException, Exception
    {
        SolrInstance instance = this.componentManager.getInstance(SolrInstance.class, EmbeddedSolrInstance.TYPE);
        assertNotNull(instance);

        EmbeddedSolrInstance implementation = ((EmbeddedSolrInstance) instance);
        CoreContainer container = implementation.getContainer();

        if (expected == null) {
            expected = implementation.getDefaultHomeDirectory();
        }

        assertEquals(expected, container.getSolrHome());
        assertEquals(1, container.getCores().size());
        SolrCore core = container.getCores().iterator().next();
        File coreBaseDirectory = new File(container.getSolrHome(), core.getName());
        File configDirectory = new File(coreBaseDirectory, DefaultSolrConfiguration.CONF_DIRECTORY);
        assertTrue(new File(configDirectory, core.getSchemaResource()).exists());
        assertTrue(new File(configDirectory, core.getConfigResource()).exists());
    }

    // Tests

    @Test
    public void testInitializationWhenValid() throws Exception
    {
        // Unzip the standard Solr home
        File solrHomeDirectory = new File(this.permanentDirectory, "solr");
        SolrConfiguration solrConfiguration = this.componentManager.getInstance(SolrConfiguration.class);
        InputStream stream = solrConfiguration.getHomeDirectoryConfiguration();
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
        when(this.mockXWikiProperties.getProperty(eq(SOLRHOME_PROPERTY), anyString()))
            .thenReturn(solrHomeDirectory.toString());

        getInstanceAndAssertHomeDirectory(solrHomeDirectory.toString());

        // Modify the configuration file
        File solrconfigFile = new File(solrHomeDirectory, "xwiki/conf/solrconfig.xml");
        String modifiedContent = FileUtils.readFileToString(solrconfigFile, StandardCharsets.UTF_8) + "/n";
        FileUtils.write(solrconfigFile, modifiedContent, StandardCharsets.UTF_8);

        getInstanceAndAssertHomeDirectory(solrHomeDirectory.toString());

        assertEquals(modifiedContent, FileUtils.readFileToString(solrconfigFile, StandardCharsets.UTF_8));
    }

    @Test
    public void testInitializationWhenInvalid() throws Exception
    {
        // Unzip the standard Solr home
        File solrHomeDirectory = new File(this.permanentDirectory, "solr");
        SolrConfiguration solrConfiguration = this.componentManager.getInstance(SolrConfiguration.class);
        InputStream stream = solrConfiguration.getHomeDirectoryConfiguration();
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
        when(this.mockXWikiProperties.getProperty(eq(SOLRHOME_PROPERTY), anyString()))
            .thenReturn(solrHomeDirectory.toString());

        // Make the configuration too old
        File solrconfigFile = new File(solrHomeDirectory, "xwiki/conf/solrconfig.xml");
        String modifiedContent = FileUtils.readFileToString(solrconfigFile, StandardCharsets.UTF_8)
            .replaceAll("<luceneMatchVersion>.*</luceneMatchVersion>", "<luceneMatchVersion>old</luceneMatchVersion>");
        FileUtils.write(solrconfigFile, modifiedContent, StandardCharsets.UTF_8);

        assertEquals(modifiedContent, FileUtils.readFileToString(solrconfigFile, StandardCharsets.UTF_8));

        getInstanceAndAssertHomeDirectory(solrHomeDirectory.toString());

        assertNotEquals(modifiedContent, FileUtils.readFileToString(solrconfigFile, StandardCharsets.UTF_8));
    }

    @Test
    public void testInstantiationWhenDoesNotExist() throws Exception
    {
        String newHome = new File(this.permanentDirectory, "doesNotExist").getAbsolutePath();
        when(this.mockXWikiProperties.getProperty(eq(SOLRHOME_PROPERTY), anyString())).thenReturn(newHome);

        getInstanceAndAssertHomeDirectory(newHome);
    }

    @Test
    public void testInstantiationWhenEmpty() throws ComponentLookupException, Exception
    {
        when(this.mockXWikiProperties.getProperty(eq(SOLRHOME_PROPERTY), anyString())).thenReturn("");

        // Not actually expecting anything. This will throw an exception.
        try {
            getInstanceAndAssertHomeDirectory(null);

            fail("Specify a valid directory. Empty values are not accepted.");
        } catch (ComponentLookupException e) {
            assertTrue(e.getCause() instanceof InitializationException);
            assertTrue(e.getCause().getCause() instanceof IllegalArgumentException);
        }
    }
}
