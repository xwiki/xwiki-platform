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

import org.jmock.Mock;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.ObservationManager;
import org.xwiki.test.MockConfigurationSource;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.test.AbstractBridgedXWikiComponentTestCase;

/**
 * Unit test for {@link DefaultRenderingCache}.
 * 
 * @version $Id$
 * @since 2.4M1
 */
public class DefaultRenderingCacheTest extends AbstractBridgedXWikiComponentTestCase
{
    private Mock mockXWiki;

    private XWikiDocument document;

    private RenderingCache renderingCache;

    @Before
    public void setUp() throws Exception
    {
        super.setUp();

        this.document = new XWikiDocument(new DocumentReference("wiki", "space", "page"));

        this.mockXWiki = mock(XWiki.class);
        getContext().setWiki((XWiki) this.mockXWiki.proxy());

        this.mockXWiki.stubs().method("getDocument").with(eq(this.document.getDocumentReference()), ANYTHING).will(
            returnValue(this.document));

        this.renderingCache = getComponentManager().lookup(RenderingCache.class);
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

        source.setProperty("core.renderingcache.documents", Collections.singletonList(this.document
            .getPrefixedFullName()));

        this.renderingCache.setRenderedContent(this.document.getDocumentReference(), "source", "renderedContent",
            getContext());

        assertEquals("renderedContent", this.renderingCache.getRenderedContent(this.document.getDocumentReference(),
            "source", getContext()));

        getComponentManager().lookup(ObservationManager.class).notify(
            new DocumentUpdatedEvent(this.document.getDocumentReference()), this.document, getContext());

        assertNull("renderedContent", this.renderingCache.getRenderedContent(this.document.getDocumentReference(),
            "source", getContext()));
    }
}
