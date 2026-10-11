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
package com.xpn.xwiki.internal.cache;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.cache.CacheManager;
import org.xwiki.cache.config.CacheConfiguration;
import org.xwiki.cache.eviction.LRUEvictionConfiguration;
import org.xwiki.cache.internal.MapCache;
import org.xwiki.model.internal.reference.DefaultStringEntityReferenceSerializer;
import org.xwiki.model.internal.reference.DefaultSymbolScheme;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.internal.DefaultObservationManager;
import org.xwiki.test.annotation.ComponentList;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link DefaultDocumentCache}.
 *
 * @version $Id$
 * @since 2.4M1
 */
@OldcoreTest
@ComponentList({DefaultDocumentCache.class, DefaultObservationManager.class,
    DefaultStringEntityReferenceSerializer.class, DefaultSymbolScheme.class})
class DefaultDocumentCacheTest
{
    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    private XWikiDocument document;

    private DefaultDocumentCache<String> cache;

    @BeforeEach
    void beforeEach() throws Exception
    {
        CacheManager cacheManager = this.oldcore.getMocker().registerMockComponent(CacheManager.class);
        when(cacheManager.createNewCache(any())).thenReturn(new MapCache<>(), new MapCache<>());

        this.cache = this.oldcore.getMocker().getInstance(DocumentCache.class);

        CacheConfiguration cacheConfiguration = new CacheConfiguration();
        cacheConfiguration.setConfigurationId("documentcachetest");
        LRUEvictionConfiguration lru = new LRUEvictionConfiguration();
        cacheConfiguration.put(LRUEvictionConfiguration.CONFIGURATIONID, lru);
        this.cache.create(cacheConfiguration);

        this.document = new XWikiDocument(new DocumentReference("wiki", "space", "page"));
        this.document.setOriginalDocument(this.document.clone());
    }

    @AfterEach
    void afterEach()
    {
        this.cache.dispose();
    }

    @Test
    void getSet()
    {
        this.cache.set("data", this.document.getDocumentReference());
        this.cache.set("data2", this.document.getDocumentReference(), "ext1", "ext2");

        assertEquals("data", this.cache.get(this.document.getDocumentReference()));
        assertEquals("data2", this.cache.get(this.document.getDocumentReference(), "ext1", "ext2"));
    }

    @Test
    void eventBasedCleanup() throws Exception
    {
        this.cache.set("data", this.document.getDocumentReference());
        this.cache.set("data", this.document.getDocumentReference(), "ext1", "ext2");

        ObservationManager observationManager = this.oldcore.getMocker().getInstance(ObservationManager.class);
        observationManager.notify(
            new DocumentUpdatedEvent(this.document.getDocumentReference()), this.document,
            this.oldcore.getXWikiContext());

        assertNull(this.cache.get(this.document.getDocumentReference()));
        assertNull(this.cache.get(this.document.getDocumentReference(), "ext1", "ext2"));
    }
}
