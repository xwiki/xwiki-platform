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

import static org.mockito.Mockito.when;

import java.io.File;
import java.net.URL;
import java.util.Date;

import org.apache.commons.io.FileUtils;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrCore;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.environment.Environment;
import org.xwiki.search.solr.internal.api.SolrInstance;
import org.xwiki.test.annotation.AllComponents;
import org.xwiki.test.mockito.MockitoComponentManagerRule;

/**
 * Test the initialization of the Solr instance.
 * 
 * @version $Id$
 */
@AllComponents
public class EmbeddedSolrInstanceInitializationTest
{
    @Rule
    public final MockitoComponentManagerRule mocker = new MockitoComponentManagerRule();

    protected File PERMANENT_DIRECTORY = new File("target", "data-" + new Date().getTime());

    @Before
    public void setup() throws Exception
    {
        Environment mockEnvironment = this.mocker.registerMockComponent(Environment.class);

        when(mockEnvironment.getPermanentDirectory()).thenReturn(PERMANENT_DIRECTORY);

        FileUtils.deleteDirectory(PERMANENT_DIRECTORY);
        PERMANENT_DIRECTORY.mkdirs();
    }

    @After
    public void tearDown() throws Exception
    {
        //FileUtils.deleteDirectory(PERMANENT_DIRECTORY);
    }

    @Test
    public void testInitialization() throws Exception
    {
        URL url = this.getClass().getClassLoader().getResource("solrhome");
        System.setProperty(EmbeddedSolrInstance.SOLR_HOME_SYSTEM_PROPERTY, url.getPath());

        getInstanceAndAssertHomeDirectory(url.getPath());
    }

    @Test
    public void testInstantiationNewHome() throws Exception
    {
        String newHome = new File(PERMANENT_DIRECTORY, "doesNotExist").getAbsolutePath();
        System.setProperty(EmbeddedSolrInstance.SOLR_HOME_SYSTEM_PROPERTY, newHome);

        getInstanceAndAssertHomeDirectory(newHome);
    }

    @Test
    public void testInstantiationNoHome() throws ComponentLookupException, Exception
    {
        System.setProperty(EmbeddedSolrInstance.SOLR_HOME_SYSTEM_PROPERTY, "");

        // Not actually expecting null, just trying to reuse code. Actually expecting default directory.
        getInstanceAndAssertHomeDirectory(null);
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
        SolrInstance instance = mocker.getInstance(SolrInstance.class, "embedded");
        Assert.assertNotNull(instance);

        EmbeddedSolrInstance implementation = ((EmbeddedSolrInstance) instance);
        CoreContainer container = implementation.getContainer();

        if (expected == null) {
            expected = implementation.getDefaultHomeDirectory();
        }

        Assert.assertEquals(expected + File.separator, container.getSolrHome());
        Assert.assertEquals(1, container.getCores().size());
        SolrCore core = container.getCores().iterator().next();
        File coreBaseDirectory = new File(container.getSolrHome(), core.getName());
        File configDirectory = new File(coreBaseDirectory, DefaultSolrConfiguration.CONF_DIRECTORY);
        Assert.assertTrue(new File(configDirectory, core.getSchemaResource()).exists());
        Assert.assertTrue(new File(configDirectory, core.getConfigResource()).exists());
    }

}
