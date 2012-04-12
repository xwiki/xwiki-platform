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

import junit.framework.Assert;

import org.jmock.Expectations;
import org.junit.Test;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.cache.config.CacheConfiguration;
import org.xwiki.cache.eviction.LRUEvictionConfiguration;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.ObservationManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.test.AbstractBridgedComponentTestCase;

/**
 * Unit test for {@link DefaultDocumentCache}.
 * 
 * @version $Id$
 * @since 2.4M1
 */
public class DefaultDocumentCacheTest extends AbstractBridgedComponentTestCase
{
    private XWiki mockXWiki;

    private XWikiDocument document;

    private DefaultDocumentCache<String> cache;

    @Override
    public void setUp() throws Exception
    {
        super.setUp();

        this.cache = (DefaultDocumentCache<String>) getComponentManager().getInstance(DocumentCache.class);

        CacheConfiguration cacheConfiguration = new CacheConfiguration();
        cacheConfiguration.setConfigurationId("documentcachetest");
        LRUEvictionConfiguration lru = new LRUEvictionConfiguration();
        cacheConfiguration.put(LRUEvictionConfiguration.CONFIGURATIONID, lru);
        this.cache.create(cacheConfiguration);

        this.document = new XWikiDocument(new DocumentReference("wiki", "space", "page"));
        this.document.setOriginalDocument(this.document.clone());

        this.mockXWiki = getMockery().mock(XWiki.class);
        getContext().setWiki(this.mockXWiki);
        
        getMockery().checking(new Expectations() {{
            allowing(mockXWiki).getDocument(document.getDocumentReference(), getContext()); will(returnValue(document));
        }});
    }

    @Override
    public void tearDown() throws Exception
    {
        this.cache.dispose();

        super.tearDown();
    }

    @Test
    public void testGetSet()
    {
        this.cache.set("data", this.document.getDocumentReference());
        this.cache.set("data2", this.document.getDocumentReference(), "ext1", "ext2");

        Assert.assertEquals("data", this.cache.get(this.document.getDocumentReference()));
        Assert.assertEquals("data2", this.cache.get(this.document.getDocumentReference(), "ext1", "ext2"));
    }

    @Test
    public void testEventBasedCleanup() throws Exception
    {
        this.cache.set("data", this.document.getDocumentReference());
        this.cache.set("data", this.document.getDocumentReference(), "ext1", "ext2");

        ObservationManager observationManager = getComponentManager().getInstance(ObservationManager.class);
        observationManager.notify(
            new DocumentUpdatedEvent(this.document.getDocumentReference()), this.document, getContext());

        Assert.assertNull(this.cache.get(this.document.getDocumentReference()));
        Assert.assertNull(this.cache.get(this.document.getDocumentReference(), "ext1", "ext2"));
    }
}
