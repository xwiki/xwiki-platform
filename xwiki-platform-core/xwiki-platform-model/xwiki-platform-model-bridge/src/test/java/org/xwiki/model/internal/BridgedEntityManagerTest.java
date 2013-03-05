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
package org.xwiki.model.internal;

import java.net.URL;
import java.util.Arrays;

import static org.mockito.Mockito.*;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheFactory;
import org.xwiki.cache.CacheManager;
import org.xwiki.cache.config.CacheConfiguration;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.model.DocumentEntity;
import org.xwiki.model.EntityManager;
import org.xwiki.model.ModelRuntimeException;
import org.xwiki.model.ObjectEntity;
import org.xwiki.model.ObjectPropertyEntity;
import org.xwiki.model.SpaceEntity;
import org.xwiki.model.UniqueReference;
import org.xwiki.model.WikiEntity;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.ObjectPropertyReference;
import org.xwiki.model.reference.ObjectReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;

/**
 * Unit tests for {@link BridgedEntityManager}.
 *
 * @version $Id$
 * @since 5.0M1
 */
public class BridgedEntityManagerTest
{
    @Rule
    public MockitoComponentMockingRule<EntityManager> componentManager =
        new MockitoComponentMockingRule<EntityManager>(BridgedEntityManager.class);

    private XWiki xwiki;

    @Before
    public void configure() throws Exception
    {
        // Mock the Entity Cache
        CacheManager cacheManager = this.componentManager.getInstance(CacheManager.class);
        CacheFactory cacheFactory = mock(CacheFactory.class);
        when(cacheManager.getCacheFactory()).thenReturn(cacheFactory);
        Cache cache = mock(Cache.class);
        when(cacheFactory.newCache(any(CacheConfiguration.class))).thenReturn(cache);

        // Set a valid XWiki object in the XWikiContext
        this.xwiki = mock(XWiki.class);
        XWikiContext xcontext = mock(XWikiContext.class);
        when(xcontext.getWiki()).thenReturn(this.xwiki);

        // Set a valid XWikiContext object in the Execution Context
        Execution execution = this.componentManager.getInstance(Execution.class);
        ExecutionContext ec = new ExecutionContext();
        when(execution.getContext()).thenReturn(ec);
        ec.setProperty("xwikicontext", xcontext);
    }

    @Test
    public void getEntityForDocumentThatExists() throws Exception
    {
        DocumentReference documentReference = new DocumentReference("wiki", "space", "page");
        XWikiDocument xdoc = mock(XWikiDocument.class);
        when(xdoc.isNew()).thenReturn(false);
        when(this.xwiki.getDocument(eq(documentReference), any(XWikiContext.class))).thenReturn(xdoc);

        DocumentEntity doc = this.componentManager.getComponentUnderTest().getEntity(new UniqueReference(documentReference));
        Assert.assertNotNull(doc);
        Assert.assertFalse(doc.isNew());
    }

    @Test
    public void getEntityForDocumentWhenStoreException() throws Exception
    {
        DocumentReference documentReference = new DocumentReference("wiki", "space", "page");
        when(this.xwiki.getDocument(eq(documentReference), any(XWikiContext.class))).thenThrow(new XWikiException());

        try {
            this.componentManager.getComponentUnderTest().getEntity(new UniqueReference(documentReference));
            Assert.fail("Should have thrown an exception");
        } catch (ModelRuntimeException expected) {
            Assert.assertEquals("Error loading document [wiki:space.page]", expected.getMessage());
        }
    }

    @Test
    public void getEntityForWikiThatExists() throws Exception
    {
        WikiReference wikiReference = new WikiReference("wiki");
        when(this.xwiki.getServerURL(eq("wiki"), any(XWikiContext.class))).thenReturn(
            new URL("http://whatever/not/null"));

        WikiEntity wikiEntity = this.componentManager.getComponentUnderTest().getEntity(
            new UniqueReference(wikiReference));
        Assert.assertNotNull(wikiEntity);
        Assert.assertFalse(wikiEntity.isNew());
    }

    @Test
    public void getEntityForWikiThatDoesntExists() throws Exception
    {
        WikiReference wikiReference = new WikiReference("wiki");
        when(this.xwiki.getServerURL(eq("wiki"), any(XWikiContext.class))).thenReturn(null);

        WikiEntity wikiEntity = this.componentManager.getComponentUnderTest().getEntity(
            new UniqueReference(wikiReference));
        Assert.assertNull(wikiEntity);
    }

    @Test
    public void hasEntityForDocumentThatExists() throws Exception
    {
        DocumentReference documentReference = new DocumentReference("wiki", "space", "page");
        when(this.xwiki.exists(eq(documentReference), any(XWikiContext.class))).thenReturn(true);

        Assert.assertTrue(this.componentManager.getComponentUnderTest().hasEntity(
            new UniqueReference(documentReference)));
    }

    @Test
    public void hasEntityForWikiThatExists() throws Exception
    {
        WikiReference wikiReference = new WikiReference("wiki");
        when(this.xwiki.getServerURL(eq("wiki"), any(XWikiContext.class))).thenReturn(
            new URL("http://whatever/not/null"));

        Assert.assertTrue(this.componentManager.getComponentUnderTest().hasEntity(new UniqueReference(wikiReference)));
    }

    @Test
    public void hasEntityForWikiThatDoesntExist() throws Exception
    {
        WikiReference wikiReference = new WikiReference("wiki");
        when(this.xwiki.getServerURL(eq("wiki"), any(XWikiContext.class))).thenReturn(null);

        Assert.assertFalse(this.componentManager.getComponentUnderTest().hasEntity(new UniqueReference(wikiReference)));
    }

    @Test
    public void getEntityForObjectThatExists() throws Exception
    {
        DocumentReference documentReference = new DocumentReference("wiki", "space", "page");
        ObjectReference objectReference = new ObjectReference("object", documentReference);

        XWikiDocument xdoc = mock(XWikiDocument.class);
        when(this.xwiki.getDocument(eq(documentReference), any(XWikiContext.class))).thenReturn(xdoc);
        BaseObject baseObject = mock(BaseObject.class);
        when(xdoc.getXObject(objectReference)).thenReturn(baseObject);

        ObjectEntity objectEntity = this.componentManager.getComponentUnderTest().getEntity(
            new UniqueReference(objectReference));
        Assert.assertNotNull(objectEntity);
        Assert.assertFalse(objectEntity.isNew());
    }

    @Test
    public void getEntityForObjectThatDoesntExists() throws Exception
    {
        DocumentReference documentReference = new DocumentReference("wiki", "space", "page");
        ObjectReference objectReference = new ObjectReference("object", documentReference);

        XWikiDocument xdoc = mock(XWikiDocument.class);
        when(this.xwiki.getDocument(eq(documentReference), any(XWikiContext.class))).thenReturn(xdoc);
        when(xdoc.getXObject(objectReference)).thenReturn(null);

        ObjectEntity objectEntity = this.componentManager.getComponentUnderTest().getEntity(
            new UniqueReference(objectReference));
        Assert.assertNull(objectEntity);
    }

    @Test
    public void getEntityForObjectWhenDocumentDoesntExists() throws Exception
    {
        DocumentReference documentReference = new DocumentReference("wiki", "space", "page");
        ObjectReference objectReference = new ObjectReference("object", documentReference);

        XWikiDocument xdoc = mock(XWikiDocument.class);
        when(this.xwiki.getDocument(eq(documentReference), any(XWikiContext.class))).thenReturn(xdoc);
        // This means the doc doesn't exist in the old model
        when(xdoc.isNew()).thenReturn(true);

        ObjectEntity objectEntity = this.componentManager.getComponentUnderTest().getEntity(
            new UniqueReference(objectReference));
        Assert.assertNull(objectEntity);
    }

    @Test
    public void getEntityForSpaceThatExists() throws Exception
    {
        SpaceReference spaceReference = new SpaceReference("space", new WikiReference("wiki"));
        when(this.xwiki.getSpaces(any(XWikiContext.class))).thenReturn(Arrays.asList("space", "otherspace"));

        SpaceEntity spaceEntity = this.componentManager.getComponentUnderTest().getEntity(
            new UniqueReference(spaceReference));
        Assert.assertNotNull(spaceEntity);
        Assert.assertFalse(spaceEntity.isNew());
    }

    @Test
    public void getEntityForObjectPropertyThatExists() throws Exception
    {
        DocumentReference documentReference = new DocumentReference("wiki", "space", "page");
        ObjectReference objectReference = new ObjectReference("object", documentReference);
        ObjectPropertyReference propertyReference = new ObjectPropertyReference("property", objectReference);

        XWikiDocument xdoc = mock(XWikiDocument.class);
        when(this.xwiki.getDocument(eq(documentReference), any(XWikiContext.class))).thenReturn(xdoc);
        BaseObject baseObject = mock(BaseObject.class);
        when(xdoc.getXObject(objectReference)).thenReturn(baseObject);
        BaseProperty baseProperty = mock(BaseProperty.class);
        when(baseObject.get("property")).thenReturn(baseProperty);

        ObjectPropertyEntity objectPropertyEntity = this.componentManager.getComponentUnderTest().getEntity(
            new UniqueReference(propertyReference));
        Assert.assertNotNull(objectPropertyEntity);
        Assert.assertFalse(objectPropertyEntity.isNew());
    }
}
