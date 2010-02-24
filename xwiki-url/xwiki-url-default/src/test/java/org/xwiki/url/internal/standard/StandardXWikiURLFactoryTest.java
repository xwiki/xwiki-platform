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
package org.xwiki.url.internal.standard;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Assert;
import org.junit.Test;
import org.xwiki.component.descriptor.DefaultComponentDescriptor;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.test.AbstractComponentTestCase;
import org.xwiki.url.XWikiEntityURL;
import org.xwiki.url.XWikiURL;
import org.xwiki.url.XWikiURLFactory;
import org.xwiki.url.XWikiURLType;

import java.net.URL;
import java.util.Collections;

/**
 * Unit tests for {@link org.xwiki.url.internal.standard.StandardXWikiURLFactory}.
 *
 * @version $Id$
 * @since 2.3M1
 */
public class StandardXWikiURLFactoryTest extends AbstractComponentTestCase
{
    private XWikiURLFactory factory;

    private HostResolver mockHostResolver;

    private StandardURLConfiguration mockConfiguration;

    private Mockery mockery = new Mockery();

    @Override protected void registerComponents() throws Exception
    {
        this.mockConfiguration = this.mockery.mock(StandardURLConfiguration.class);
        DefaultComponentDescriptor<StandardURLConfiguration> descriptorUC =
            new DefaultComponentDescriptor<StandardURLConfiguration>();
        descriptorUC.setRole(StandardURLConfiguration.class);
        getComponentManager().registerComponent(descriptorUC, this.mockConfiguration);

        this.mockHostResolver = this.mockery.mock(HostResolver.class);
        DefaultComponentDescriptor<HostResolver> descriptorHR =
            new DefaultComponentDescriptor<HostResolver>();
        descriptorHR.setRole(HostResolver.class);
        getComponentManager().registerComponent(descriptorHR, this.mockHostResolver);

        this.factory = getComponentManager().lookup(XWikiURLFactory.class, "standard");
    }

    @Test
    public void testCreateDomainBasedMainWikiURL() throws Exception
    {
        this.mockery.checking(new Expectations() {{
            allowing(mockHostResolver).resolve("localhost");
                will(returnValue(new WikiReference("Wiki")));
            allowing(mockConfiguration).isPathBasedMultiWikiFormat();
                will(returnValue(false));
        }});

        XWikiURL xwikiURL = this.factory.createURL(new URL("http://localhost:8080/xwiki/bin/view/Space/Page"),
            Collections.<String, Object>singletonMap("ignorePrefix", "/xwiki"));

        Assert.assertEquals(XWikiURLType.ENTITY, xwikiURL.getType());
        XWikiEntityURL entityURL = (XWikiEntityURL) xwikiURL;
        Assert.assertEquals("view", entityURL.getAction());
        Assert.assertEquals(new DocumentReference("Wiki", "Space", "Page"), entityURL.getEntityReference());
    }

    @Test
    public void testCreateDomainBasedSubWikiURL() throws Exception
    {
        this.mockery.checking(new Expectations() {{
            allowing(mockHostResolver).resolve("subwiki.domain.ext");
                will(returnValue(new WikiReference("Wiki")));
            allowing(mockConfiguration).isPathBasedMultiWikiFormat();
                will(returnValue(false));
        }});

        XWikiURL xwikiURL = this.factory.createURL(new URL("http://subwiki.domain.ext/xwiki/bin/view/Space/Page"),
            Collections.<String, Object>singletonMap("ignorePrefix", "/xwiki"));

        Assert.assertEquals(XWikiURLType.ENTITY, xwikiURL.getType());
        XWikiEntityURL entityURL = (XWikiEntityURL) xwikiURL;
        Assert.assertEquals("view", entityURL.getAction());
        Assert.assertEquals(new DocumentReference("Wiki", "Space", "Page"), entityURL.getEntityReference());
    }

    @Test
    public void testCreatePathBasedMainWikiURL() throws Exception
    {
        this.mockery.checking(new Expectations() {{
            allowing(mockHostResolver).resolve("localhost");
                will(returnValue(new WikiReference("Wiki")));
            allowing(mockConfiguration).isPathBasedMultiWikiFormat();
                will(returnValue(true));
            allowing(mockConfiguration).getWikiPathPrefix();
                will(returnValue("wiki"));
        }});

        XWikiURL xwikiURL = this.factory.createURL(new URL("http://localhost:8080/xwiki/bin/view/Space/Page"),
            Collections.<String, Object>singletonMap("ignorePrefix", "/xwiki"));

        Assert.assertEquals(XWikiURLType.ENTITY, xwikiURL.getType());
        XWikiEntityURL entityURL = (XWikiEntityURL) xwikiURL;
        Assert.assertEquals("view", entityURL.getAction());
        Assert.assertEquals(new DocumentReference("Wiki", "Space", "Page"), entityURL.getEntityReference());
    }

    @Test
    public void testCreatePathBasedSubWikiURL() throws Exception
    {
        this.mockery.checking(new Expectations() {{
            allowing(mockHostResolver).resolve("subwiki");
                will(returnValue(new WikiReference("Wiki")));
            allowing(mockConfiguration).isPathBasedMultiWikiFormat();
                will(returnValue(true));
            allowing(mockConfiguration).getWikiPathPrefix();
                will(returnValue("wiki"));
        }});

        XWikiURL xwikiURL = this.factory.createURL(new URL("http://host/xwiki/wiki/subwiki/view/Space/Page"),
            Collections.<String, Object>singletonMap("ignorePrefix", "/xwiki"));

        Assert.assertEquals(XWikiURLType.ENTITY, xwikiURL.getType());
        XWikiEntityURL entityURL = (XWikiEntityURL) xwikiURL;
        Assert.assertEquals("view", entityURL.getAction());
        Assert.assertEquals(new DocumentReference("Wiki", "Space", "Page"), entityURL.getEntityReference());
    }
}
