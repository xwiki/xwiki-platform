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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.cache.CacheManager;
import org.xwiki.cache.internal.MapCache;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.ObservationManager;
import org.xwiki.test.annotation.AllComponents;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.web.XWikiRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link DefaultRenderingCache}.
 *
 * @version $Id$
 * @since 2.4M1
 */
@OldcoreTest
@AllComponents
class DefaultRenderingCacheTest
{
    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    @Mock
    private XWikiRequest mockRequest;

    private XWikiDocument document;

    private RenderingCache renderingCache;

    private final Map<String, String[]> parameters = new HashMap<>();

    @BeforeEach
    void beforeEach() throws Exception
    {
        CacheManager cacheManager = this.oldcore.getMocker().registerMockComponent(CacheManager.class);
        when(cacheManager.createNewCache(any())).thenReturn(new MapCache<>(), new MapCache<>());

        this.document = new XWikiDocument(new DocumentReference("wiki", "space", "page"));
        this.document.setOriginalDocument(this.document.clone());

        this.oldcore.getXWikiContext().setRequest(this.mockRequest);
        when(this.mockRequest.getParameterMap()).thenReturn(this.parameters);

        this.oldcore.getConfigurationSource().setProperty("core.renderingcache.enabled", true);

        this.renderingCache = this.oldcore.getMocker().getInstance(RenderingCache.class);
    }

    @Test
    void getSetRenderedContent() throws Exception
    {
        this.oldcore.getConfigurationSource().setProperty("core.renderingcache.documents",
            Collections.singletonList(this.document.getPrefixedFullName()));

        this.renderingCache.setRenderedContent(this.document.getDocumentReference(), "source", "renderedContent",
            this.oldcore.getXWikiContext());

        assertEquals("renderedContent",
            this.renderingCache.getRenderedContent(this.document.getDocumentReference(), "source",
                this.oldcore.getXWikiContext()));

        this.parameters.put("param", new String[]{"value1", "value2"});

        assertNull(this.renderingCache.getRenderedContent(this.document.getDocumentReference(), "source",
            this.oldcore.getXWikiContext()));

        this.parameters.remove("param");

        assertEquals("renderedContent",
            this.renderingCache.getRenderedContent(this.document.getDocumentReference(), "source",
                this.oldcore.getXWikiContext()));

        ObservationManager observationManager = this.oldcore.getMocker().getInstance(ObservationManager.class);
        observationManager.notify(new DocumentUpdatedEvent(this.document.getDocumentReference()), this.document,
            this.oldcore.getXWikiContext());

        assertNull(this.renderingCache.getRenderedContent(this.document.getDocumentReference(), "source",
            this.oldcore.getXWikiContext()));
    }
}
