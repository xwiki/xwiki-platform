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

import org.apache.solr.core.CoreContainer;
import org.jmock.Expectations;
import org.junit.Test;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.environment.Environment;
import org.xwiki.search.solr.SolrInstance;
import org.xwiki.search.solr.internal.EmbeddedSolrInstance;
import org.xwiki.test.AbstractComponentTestCase;

/**
 * Test the initialization of the Solr instance.
 * 
 * @version $Id$
 */
public class EmbeddedSolrInstanceInitializationTest extends AbstractComponentTestCase
{
    @Override
    protected void registerComponents() throws Exception
    {
        final Environment mockEnvironment =
            getComponentManager().registerMockComponent(getMockery(), Environment.class);

        getMockery().checking(new Expectations()
        {
            {
                allowing(mockEnvironment).getPermanentDirectory();
                will(returnValue(new File(".data")));
            }
        });
    }

    @Test
    public void testInitialization() throws Exception
    {
        URL url = this.getClass().getClassLoader().getResource("solrhome");
        System.setProperty(EmbeddedSolrInstance.SOLR_HOME_KEY, url.getPath());

        getInstanceAndAssertHomeDirectory(url.getPath());
    }

    @Test
    public void testInstantiationNewHome() throws Exception
    {
        URL url = this.getClass().getClassLoader().getResource("solrhome");
        String newHome = new File(url.getPath(), "doesNotExist").toString();
        System.setProperty(EmbeddedSolrInstance.SOLR_HOME_KEY, newHome);

        getInstanceAndAssertHomeDirectory(newHome);
    }

    @Test
    public void testInstantiationNoHome() throws ComponentLookupException, Exception
    {
        System.setProperty(EmbeddedSolrInstance.SOLR_HOME_KEY, "");

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
    }

}
