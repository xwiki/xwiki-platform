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

import org.jmock.Expectations;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheFactory;
import org.xwiki.cache.CacheManager;
import org.xwiki.cache.config.CacheConfiguration;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.model.Document;
import org.xwiki.model.ModelException;
import org.xwiki.model.ObjectProperty;
import org.xwiki.model.Space;
import org.xwiki.model.UniqueReference;
import org.xwiki.model.Wiki;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.ObjectPropertyReference;
import org.xwiki.model.reference.ObjectReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.test.AbstractMockingComponentTestCase;
import org.xwiki.test.annotation.AllComponents;
import org.xwiki.test.annotation.MockingRequirement;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.web.Utils;

/**
 * Unit tests for {@link BridgedEntityManager}.
 *
 * @version $Id$
 * @since 4.3M1
 */
@AllComponents
@MockingRequirement(BridgedEntityManager.class)
public class BridgedEntityManagerTest extends AbstractMockingComponentTestCase<BridgedEntityManager>
{
    private XWikiContext context;

    @Before
    public void configure() throws Exception
    {
        // Allow mocking Classes...
        getMockery().setImposteriser(ClassImposteriser.INSTANCE);
        Utils.setComponentManager(getComponentManager());
        this.context = new XWikiContext();
        final XWiki xwiki = getMockery().mock(XWiki.class);
        this.context.setWiki(xwiki);

        final CacheManager cacheManager = getComponentManager().getInstance(CacheManager.class);
        final CacheFactory cacheFactory = getMockery().mock(CacheFactory.class);
        final Execution execution = getComponentManager().getInstance(Execution.class);
        final ExecutionContext executionContext = new ExecutionContext();
        executionContext.setProperty("xwikicontext", this.context);

        getMockery().checking(new Expectations() {{
            oneOf(cacheManager).getCacheFactory();
            will(returnValue(cacheFactory));
            oneOf(cacheFactory).newCache(with(any(CacheConfiguration.class)));
            will(returnValue(getMockery().mock(Cache.class)));
            allowing(execution).getContext();
            will(returnValue(executionContext));
        }});
    }

    @Test
    public void getEntityForDocumentThatExists() throws Exception
    {
        final DocumentReference documentReference = new DocumentReference("wiki", "space", "page");
        final XWikiDocument xdoc = new XWikiDocument(documentReference);
        xdoc.setNew(false);
        getMockery().checking(new Expectations() {{
            oneOf(context.getWiki()).getDocument(documentReference, context);
                will(returnValue(xdoc));
        }});

        Document doc = getMockedComponent().getEntity(new UniqueReference(documentReference));
        Assert.assertNotNull(doc);
        Assert.assertFalse(doc.isNew());
    }

    @Test
    public void getEntityForDocumentThatDoesntExist() throws Exception
    {
        final DocumentReference documentReference = new DocumentReference("wiki", "space", "page");
        final XWikiDocument xdoc = new XWikiDocument(documentReference);
        // This means the doc doesn't exist in the old model
        xdoc.setNew(true);
        getMockery().checking(new Expectations() {{
            oneOf(context.getWiki()).getDocument(documentReference, context);
                will(returnValue(xdoc));
        }});

        Document doc = getMockedComponent().getEntity(new UniqueReference(documentReference));
        Assert.assertNotNull(doc);
        Assert.assertTrue(doc.isNew());
    }

    @Test
    public void getEntityForDocumentWhenStoreException() throws Exception
    {
        final DocumentReference documentReference = new DocumentReference("wiki", "space", "page");
        final XWikiDocument xdoc = new XWikiDocument(documentReference);
        // This means the doc doesn't exist in the old model
        xdoc.setNew(true);
        getMockery().checking(new Expectations() {{
            oneOf(context.getWiki()).getDocument(documentReference, context);
            will(throwException(new XWikiException()));
        }});

        try {
            getMockedComponent().getEntity(new UniqueReference(documentReference));
            Assert.fail("Should have thrown an exception");
        } catch (ModelException expected) {
            Assert.assertEquals("Error loading document [wiki:space.page]", expected.getMessage());
        }
    }

    @Test
    public void getEntityForWikiThatExists() throws Exception
    {
        final WikiReference wikiReference = new WikiReference("wiki");
        getMockery().checking(new Expectations() {{
            oneOf(context.getWiki()).getServerURL("wiki", context);
                will(returnValue(new URL("http://whatever/not/null")));
        }});

        Wiki wiki = getMockedComponent().getEntity(new UniqueReference(wikiReference));
        Assert.assertNotNull(wiki);
        Assert.assertFalse(wiki.isNew());
    }

    @Test
    public void getEntityForWikiThatDoesntExists() throws Exception
    {
        final WikiReference wikiReference = new WikiReference("wiki");
        getMockery().checking(new Expectations() {{
            oneOf(context.getWiki()).getServerURL("wiki", context);
                will(returnValue(null));
        }});

        Wiki wiki = getMockedComponent().getEntity(new UniqueReference(wikiReference));
        Assert.assertNull(wiki);
    }

    @Test
    public void hasEntityForDocumentThatExists() throws Exception
    {
        final DocumentReference documentReference = new DocumentReference("wiki", "space", "page");
        getMockery().checking(new Expectations() {{
            oneOf(context.getWiki()).exists(documentReference, context);
                will(returnValue(true));
        }});

        Assert.assertTrue(getMockedComponent().hasEntity(new UniqueReference(documentReference)));
    }

    @Test
    public void hasEntityForWikiThatExists() throws Exception
    {
        final WikiReference wikiReference = new WikiReference("wiki");
        getMockery().checking(new Expectations() {{
            oneOf(context.getWiki()).getServerURL("wiki", context);
                will(returnValue(new URL("http://whatever/not/null")));
        }});

        Assert.assertTrue(getMockedComponent().hasEntity(new UniqueReference(wikiReference)));
    }

    @Test
    public void hasEntityForWikiThatDoesntExist() throws Exception
    {
        final WikiReference wikiReference = new WikiReference("wiki");
        getMockery().checking(new Expectations() {{
            oneOf(context.getWiki()).getServerURL("wiki", context);
                will(returnValue(null));
        }});

        Assert.assertFalse(getMockedComponent().hasEntity(new UniqueReference(wikiReference)));
    }

    @Test
    public void getEntityForObjectThatExists() throws Exception
    {
        final DocumentReference documentReference = new DocumentReference("wiki", "space", "page");
        final ObjectReference objectReference = new ObjectReference("object", documentReference);
        final XWikiDocument xdoc = getMockery().mock(XWikiDocument.class);
        final BaseObject baseObject = getMockery().mock(BaseObject.class);
        getMockery().checking(new Expectations() {{
            oneOf(context.getWiki()).getDocument(documentReference, context);
                will(returnValue(xdoc));
            oneOf(xdoc).getXObject(objectReference);
                will(returnValue(baseObject));
        }});

        org.xwiki.model.Object object = getMockedComponent().getEntity(new UniqueReference(objectReference));
        Assert.assertNotNull(object);
        Assert.assertFalse(object.isNew());
    }

    @Test
    public void getEntityForObjectThatDoesntExists() throws Exception
    {
        final DocumentReference documentReference = new DocumentReference("wiki", "space", "page");
        final ObjectReference objectReference = new ObjectReference("object", documentReference);
        final XWikiDocument xdoc = getMockery().mock(XWikiDocument.class);
        getMockery().checking(new Expectations() {{
            oneOf(context.getWiki()).getDocument(documentReference, context);
                will(returnValue(xdoc));
            oneOf(xdoc).getXObject(objectReference);
                will(returnValue(null));
        }});

        org.xwiki.model.Object object = getMockedComponent().getEntity(new UniqueReference(objectReference));
        Assert.assertNull(object);
    }

    @Test
    public void getEntityForObjectWhenDocumentDoesntExists() throws Exception
    {
        final DocumentReference documentReference = new DocumentReference("wiki", "space", "page");
        final ObjectReference objectReference = new ObjectReference("object", documentReference);
        final XWikiDocument xdoc = new XWikiDocument(documentReference);
        // This means the doc doesn't exist in the old model
        xdoc.setNew(true);
        getMockery().checking(new Expectations() {{
            oneOf(context.getWiki()).getDocument(documentReference, context);
                will(returnValue(xdoc));
        }});

        org.xwiki.model.Object object = getMockedComponent().getEntity(new UniqueReference(objectReference));
        Assert.assertNull(object);
    }

    @Test
    public void getEntityForSpaceThatExists() throws Exception
    {
        final SpaceReference spaceReference = new SpaceReference("space", new WikiReference("wiki"));

        getMockery().checking(new Expectations() {{
            oneOf(context.getWiki()).getSpaces(context);
            will(returnValue(Arrays.asList("space", "otherspace")));
        }});

        Space space = getMockedComponent().getEntity(new UniqueReference(spaceReference));
        Assert.assertNotNull(space);
        Assert.assertFalse(space.isNew());
    }

    @Test
    public void getEntityForObjectPropertyThatExists() throws Exception
    {
        final DocumentReference documentReference = new DocumentReference("wiki", "space", "page");
        final ObjectReference objectReference = new ObjectReference("object", documentReference);
        final ObjectPropertyReference propertyReference = new ObjectPropertyReference("property", objectReference);
        final XWikiDocument xdoc = getMockery().mock(XWikiDocument.class);
        final BaseObject baseObject = getMockery().mock(BaseObject.class);
        final BaseProperty baseProperty = getMockery().mock(BaseProperty.class);

        getMockery().checking(new Expectations() {{
            oneOf(context.getWiki()).getDocument(documentReference, context);
            will(returnValue(xdoc));
            oneOf(xdoc).getXObject(objectReference);
            will(returnValue(baseObject));
            oneOf(baseObject).get("property");
            will(returnValue(baseProperty));
        }});

        ObjectProperty objectProperty = getMockedComponent().getEntity(new UniqueReference(propertyReference));
        Assert.assertNotNull(objectProperty);
        Assert.assertFalse(objectProperty.isNew());
    }
}
