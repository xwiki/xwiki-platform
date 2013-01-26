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

import java.net.URL;

import javax.inject.Provider;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.search.solr.internal.api.SolrInstance;
import org.xwiki.test.jmock.AbstractComponentTestCase;

/**
 * TODO DOCUMENT ME!
 * 
 * @version $Id$
 */
public class SolrInstanceProviderTest extends AbstractComponentTestCase
{
    private Provider<SolrInstance> provider;

    @Before
    @Override
    public void setUp() throws Exception
    {
        super.setUp();

        this.provider =
            getComponentManager().getInstance(new DefaultParameterizedType(null, Provider.class, SolrInstance.class));

        URL url = this.getClass().getClassLoader().getResource("solrhome");
        System.setProperty(EmbeddedSolrInstance.SOLR_HOME_SYSTEM_PROPERTY, url.getPath());
    }

    @Override
    @After
    public void tearDown() throws Exception
    {
        this.provider.get().shutDown();

        super.tearDown();
    }

    @Test
    public void testProviderLookup() throws Exception
    {
        Assert.assertNotNull(provider);
        Assert.assertTrue(this.provider instanceof SolrInstanceProvider);
    }

    @Test
    public void testInstanceRetrieval() throws Exception
    {
        SolrInstance instance = provider.get();
        Assert.assertNotNull(instance);
        Assert.assertTrue(instance instanceof EmbeddedSolrInstance);
    }

    // FIXME: add test for remote solr instance.
}
