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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.search.solr.internal.api.SolrConfiguration;
import org.xwiki.search.solr.internal.api.SolrInstance;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.mockito.Mockito.when;

/**
 * Tests for {@link SolrInstanceProvider}.
 * 
 * @version $Id$
 */
public class SolrInstanceProviderTest
{
    @Rule
    public final MockitoComponentMockingRule<SolrInstanceProvider> mocker =
        new MockitoComponentMockingRule<SolrInstanceProvider>(SolrInstanceProvider.class);

    private SolrInstance embedded;

    private SolrInstance remote;

    private SolrConfiguration mockConfig;

    @Before
    public void setUp() throws Exception
    {
        this.mockConfig = this.mocker.getInstance(SolrConfiguration.class);

        this.embedded = this.mocker.registerMockComponent(SolrInstance.class, "embedded");
        this.remote = this.mocker.registerMockComponent(SolrInstance.class, "remote");
    }

    @Test
    public void testEmbeddedInstanceRetrieval() throws Exception
    {
        when(this.mockConfig.getServerType()).thenReturn("embedded");
        SolrInstance instance = this.mocker.getComponentUnderTest().get();
        Assert.assertNotNull(instance);
        Assert.assertSame(this.embedded, instance);
    }

    @Test
    public void testRemoteInstanceRetrieval() throws Exception
    {
        when(this.mockConfig.getServerType()).thenReturn("remote");
        SolrInstance instance = this.mocker.getComponentUnderTest().get();
        Assert.assertNotNull(instance);
        Assert.assertSame(this.remote, instance);
    }

    @Test(expected = InitializationException.class)
    public void testInstanceRetrievalWithWrongConfiguration() throws Throwable
    {
        when(this.mockConfig.getServerType()).thenReturn("none");
        try {
            this.mocker.getComponentUnderTest();
        } catch (ComponentLookupException ex) {
            throw ex.getCause();
        }
    }
}
