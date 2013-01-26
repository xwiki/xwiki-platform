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
import java.net.URL;

import junit.framework.Assert;

import org.apache.commons.io.FileUtils;
import org.apache.solr.core.CoreContainer;
import org.jmock.Expectations;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.environment.Environment;
import org.xwiki.search.solr.internal.api.SolrInstance;
import org.xwiki.test.jmock.AbstractComponentTestCase;

/**
 * Test the initialization of the Solr instance.
 * 
 * @version $Id$
 */
public class EmbeddedSolrInstanceInitializationTest extends AbstractComponentTestCase
{
    protected File PERMANENT_DIRECTORY = new File(System.getProperty("java.io.tmpdir"), "data");

    @Override
    protected void registerComponents() throws Exception
    {
        final Environment mockEnvironment =
            getComponentManager().registerMockComponent(getMockery(), Environment.class);

        getMockery().checking(new Expectations()
        {
            {
                allowing(mockEnvironment).getPermanentDirectory();
                will(returnValue(PERMANENT_DIRECTORY));
            }
        });
    }

    @Before
    public void setup() throws Exception
    {
        FileUtils.deleteDirectory(PERMANENT_DIRECTORY);
        PERMANENT_DIRECTORY.mkdirs();
    }

    @After
    public void tearDown() throws Exception
    {
        EmbeddedSolrInstance instance = getComponentManager().getInstance(SolrInstance.class, "embedded");
        instance.shutDown();

        super.tearDown();

        FileUtils.deleteDirectory(PERMANENT_DIRECTORY);
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
        SolrInstance instance = getComponentManager().getInstance(SolrInstance.class, "embedded");
        Assert.assertNotNull(instance);

        EmbeddedSolrInstance implementation = ((EmbeddedSolrInstance) instance);
        CoreContainer container = implementation.getContainer();

        if (expected == null) {
            expected = implementation.getDefaultHomeDirectory();
        }
        Assert.assertEquals(expected + File.separator, container.getSolrHome());
        Assert.assertTrue(new File(new File(container.getSolrHome(), DefaultSolrConfiguration.CONF_DIRECTORY),
            "schema.xml").exists());
        Assert.assertTrue(new File(new File(container.getSolrHome(), DefaultSolrConfiguration.CONF_DIRECTORY),
            "solrconfig.xml").exists());
    }

}
