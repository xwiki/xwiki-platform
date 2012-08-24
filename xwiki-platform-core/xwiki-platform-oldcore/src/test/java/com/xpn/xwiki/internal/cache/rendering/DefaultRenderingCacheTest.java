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
package com.xpn.xwiki.internal.cache.rendering;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jmock.Expectations;
import org.junit.Assert;
import org.junit.Test;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.ObservationManager;
import org.xwiki.test.MockConfigurationSource;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.test.AbstractBridgedComponentTestCase;
import com.xpn.xwiki.web.XWikiServletRequestStub;

/**
 * Unit test for {@link DefaultRenderingCache}.
 * 
 * @version $Id$
 * @since 2.4M1
 */
public class DefaultRenderingCacheTest extends AbstractBridgedComponentTestCase
{
    private XWiki mockXWiki;

    private XWikiDocument document;

    private RenderingCache renderingCache;

    private XWikiServletRequestStub mockRequest;

    private Map<String, String> parameters = new HashMap<String, String>();

    private String refresh;

    @Override
    public void setUp() throws Exception
    {
        super.setUp();

        this.document = new XWikiDocument(new DocumentReference("wiki", "space", "page"));
        this.document.setOriginalDocument(this.document.clone());

        this.mockXWiki = getMockery().mock(XWiki.class);
        getContext().setWiki(this.mockXWiki);

        this.mockRequest = getMockery().mock(XWikiServletRequestStub.class);
        getContext().setRequest(this.mockRequest);

        this.renderingCache = getComponentManager().getInstance(RenderingCache.class);

        // @formatter:off
        getMockery().checking(new Expectations() {{
            allowing(mockXWiki).getDocument(document.getDocumentReference(), getContext()); will(returnValue(document));
            allowing(mockRequest).getParameterMap(); will(returnValue(parameters));
            allowing(mockRequest).getParameter("refresh"); will(returnValue(refresh));
        }});
        //@formatter:on
    }

    @Override
    protected void registerComponents() throws Exception
    {
        super.registerComponents();

        getConfigurationSource().setProperty("core.renderingcache.enabled", true);
    }

    @Test
    public void testGetSetRenderedContent() throws Exception
    {
        MockConfigurationSource source = getConfigurationSource();

        source.setProperty("core.renderingcache.documents",
            Collections.singletonList(this.document.getPrefixedFullName()));

        this.renderingCache.setRenderedContent(this.document.getDocumentReference(), "source", "renderedContent",
            getContext());

        Assert.assertEquals("renderedContent",
            this.renderingCache.getRenderedContent(this.document.getDocumentReference(), "source", getContext()));

        this.parameters.put("param", "value");
        
        Assert.assertNull(this.renderingCache.getRenderedContent(this.document.getDocumentReference(), "source",
            getContext()));

        this.parameters.remove("param");

        Assert.assertEquals("renderedContent",
            this.renderingCache.getRenderedContent(this.document.getDocumentReference(), "source", getContext()));

        ObservationManager observationManager = getComponentManager().getInstance(ObservationManager.class);
        observationManager.notify(
            new DocumentUpdatedEvent(this.document.getDocumentReference()), this.document, getContext());

        Assert.assertNull(this.renderingCache.getRenderedContent(this.document.getDocumentReference(), "source",
            getContext()));
    }
}
