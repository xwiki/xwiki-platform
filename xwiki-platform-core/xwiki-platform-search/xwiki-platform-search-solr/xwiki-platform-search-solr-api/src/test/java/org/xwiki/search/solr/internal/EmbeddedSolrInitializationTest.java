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
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.input.CloseShieldInputStream;
import org.apache.lucene.util.Version;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrCore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.environment.Environment;
import org.xwiki.search.solr.Solr;
import org.xwiki.search.solr.internal.api.SolrConfiguration;
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
class EmbeddedSolrInitializationTest
{
    private final static String SOLRHOME_PROPERTY = String.format("%s.%s.%s", "solr", EmbeddedSolr.TYPE, "home");

    private final static String SEARCH_SOLRCORE = SolrClientInstance.CORE_NAME + '_' + Version.LATEST.major;

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

        when(this.mockXWikiProperties.getProperty(eq(SOLRHOME_PROPERTY), anyString())).then(new Answer<String>()
        {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable
            {
                return invocation.getArgument(1);
            }
        });
    }

    private void getInstanceAndAssertHomeDirectory(String expected) throws ComponentLookupException, Exception
    {
        Solr instance = this.componentManager.getInstance(Solr.class, EmbeddedSolr.TYPE);
        assertNotNull(instance);

        EmbeddedSolr implementation = ((EmbeddedSolr) instance);
        CoreContainer container = implementation.getContainer();

        if (expected == null) {
            SolrConfiguration configuration = this.componentManager.getInstance(SolrConfiguration.class);
            expected = configuration.getDefaultHomeDirectory();
        }

        assertEquals(expected, container.getSolrHome());
        assertEquals(List.of(SEARCH_SOLRCORE), container.getLoadedCoreNames());
        SolrCore core = container.getCore(container.getLoadedCoreNames().get(0));
        File coreBaseDirectory = new File(container.getSolrHome(), core.getName());
        File configDirectory = new File(coreBaseDirectory, DefaultSolrConfiguration.CONF_DIRECTORY);
        assertTrue(new File(configDirectory, core.getSchemaResource()).exists());
        assertTrue(new File(configDirectory, core.getConfigResource()).exists());
    }

    // Tests

    @Test
    void testInitializationWhenValid() throws Exception
    {
        // Create the Solr home
        File solrHomeDirectory = new File(this.permanentDirectory, "store/solr");
        solrHomeDirectory.mkdirs();
        FileUtils.write(new File(solrHomeDirectory, "solr.xml"), "<solr/>", StandardCharsets.UTF_8);

        // Unzip the standard Solr home
        File solrSearchCoreDirectory = new File(solrHomeDirectory, SEARCH_SOLRCORE);
        SolrConfiguration solrConfiguration = this.componentManager.getInstance(SolrConfiguration.class);
        InputStream stream = solrConfiguration.getSearchCoreDefaultContent();
        try (ZipInputStream zstream = new ZipInputStream(stream)) {
            for (ZipEntry entry = zstream.getNextEntry(); entry != null; entry = zstream.getNextEntry()) {
                if (entry.isDirectory()) {
                    File destinationDirectory = new File(solrSearchCoreDirectory, entry.getName());
                    destinationDirectory.mkdirs();
                } else {
                    File destinationFile = new File(solrSearchCoreDirectory, entry.getName());
                    FileUtils.copyInputStreamToFile(CloseShieldInputStream.wrap(zstream), destinationFile);
                }
            }
        }

        getInstanceAndAssertHomeDirectory(solrHomeDirectory.toString());

        // Modify the configuration file
        File solrconfigFile = new File(solrSearchCoreDirectory, "conf/solrconfig.xml");
        String modifiedContent = FileUtils.readFileToString(solrconfigFile, StandardCharsets.UTF_8) + "/n";
        FileUtils.write(solrconfigFile, modifiedContent, StandardCharsets.UTF_8);

        getInstanceAndAssertHomeDirectory(solrHomeDirectory.toString());

        assertEquals(modifiedContent, FileUtils.readFileToString(solrconfigFile, StandardCharsets.UTF_8));
    }

    @Test
    void testInitializationWhenInvalid() throws Exception
    {
        // Create the Solr home
        File solrHomeDirectory = new File(this.permanentDirectory, "store/solr");
        solrHomeDirectory.mkdirs();
        FileUtils.write(new File(solrHomeDirectory, "solr.xml"), "<solr/>", StandardCharsets.UTF_8);

        // Unzip the standard Solr home
        File solrSearchCoreDirectory = new File(solrHomeDirectory, SEARCH_SOLRCORE);
        SolrConfiguration solrConfiguration = this.componentManager.getInstance(SolrConfiguration.class);
        InputStream stream = solrConfiguration.getSearchCoreDefaultContent();
        try (ZipInputStream zstream = new ZipInputStream(stream)) {
            for (ZipEntry entry = zstream.getNextEntry(); entry != null; entry = zstream.getNextEntry()) {
                if (entry.isDirectory()) {
                    File destinationDirectory = new File(solrSearchCoreDirectory, entry.getName());
                    destinationDirectory.mkdirs();
                } else {
                    File destinationFile = new File(solrSearchCoreDirectory, entry.getName());
                    FileUtils.copyInputStreamToFile(CloseShieldInputStream.wrap(zstream), destinationFile);
                }
            }
        }

        // Make the configuration too old
        File solrconfigFile = new File(solrSearchCoreDirectory, "conf/solrconfig.xml");
        String modifiedContent = FileUtils.readFileToString(solrconfigFile, StandardCharsets.UTF_8)
            .replaceAll("<luceneMatchVersion>.*</luceneMatchVersion>", "<luceneMatchVersion>old</luceneMatchVersion>");
        FileUtils.write(solrconfigFile, modifiedContent, StandardCharsets.UTF_8);

        assertEquals(modifiedContent, FileUtils.readFileToString(solrconfigFile, StandardCharsets.UTF_8));

        getInstanceAndAssertHomeDirectory(solrHomeDirectory.toString());

        assertNotEquals(modifiedContent, FileUtils.readFileToString(solrconfigFile, StandardCharsets.UTF_8));
    }

    @Test
    void testInstantiationWhenDoesNotExist() throws Exception
    {
        String newHome = new File(this.permanentDirectory, "doesNotExist").getAbsolutePath();
        when(this.mockXWikiProperties.getProperty(eq(SOLRHOME_PROPERTY), anyString())).thenReturn(newHome);

        getInstanceAndAssertHomeDirectory(newHome);
    }
    
    /**
     * This looks for a regression where the file.seperator on windows was not properly escaped. 
     * in the ""xwiki\store\solr\search\core.properties" file. 
     */
    @Test
    void testCacheHomeProperlyEscapedOnWindows() throws Exception {
        String newHome = new File(this.permanentDirectory, "doesNotExist").getAbsolutePath();
        when(this.mockXWikiProperties.getProperty(eq(SOLRHOME_PROPERTY), anyString())).thenReturn(newHome);
        
        Solr instance = this.componentManager.getInstance(Solr.class, EmbeddedSolr.TYPE);
        EmbeddedSolr implementation = ((EmbeddedSolr) instance);
        CoreContainer container = implementation.getContainer();
        SolrCore core = container.getCore(container.getLoadedCoreNames().get(0));
        File coreBaseDirectory = new File(container.getSolrHome(), core.getName());       
        
        File file = new File(coreBaseDirectory,"core.properties");
        assertTrue(file.exists(), "Couldn't find solr cache properties file: "+file.toString());
        Properties properties = new Properties();
        try( BufferedReader in = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8)); ){
        	properties.load(in);
        }
        String dataDir = properties.getProperty("DataDir");
        assertNotNull(dataDir, "DataDir property from properties file was null: "+file.toString());
        String fileSeperator = System.getProperty("file.separator");
        assertTrue(dataDir.contains(fileSeperator),"File seperators were not escaped properly in the "
        		+ "cache path!: \""+dataDir+"\" does not contain '"+fileSeperator+"'!");
        
    }
}
