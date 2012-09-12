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

import java.lang.reflect.Type;
import java.net.URL;
import java.util.Arrays;

import org.jmock.Expectations;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.cache.CacheManager;
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

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.PropertyInterface;
import com.xpn.xwiki.test.AbstractBridgedComponentTestCase;

/**
 * Unit tests for {@link BridgedEntityManager}.
 *
 * @version $Id$
 * @since 4.3M1
 */
public class BridgedEntityManagerTest extends AbstractBridgedComponentTestCase
{
    private BridgedEntityManager manager;

    @Before
    public void configure() throws Exception
    {
        final XWiki xwiki = getMockery().mock(XWiki.class);
        getContext().setWiki(xwiki);

        CacheManager cacheManager = getComponentManager().getInstance((Type) CacheManager.class);
        this.manager = new BridgedEntityManager(cacheManager, getContext());
    }

    @Test
    public void getEntityForDocumentThatExists() throws Exception
    {
        final DocumentReference documentReference = new DocumentReference("wiki", "space", "page");
        final XWikiDocument xdoc = new XWikiDocument(documentReference);
        xdoc.setNew(false);
        getMockery().checking(new Expectations() {{
            oneOf(getContext().getWiki()).getDocument(documentReference, getContext());
                will(returnValue(xdoc));
        }});

        Document doc = this.manager.getEntity(new UniqueReference(documentReference));
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
            oneOf(getContext().getWiki()).getDocument(documentReference, getContext());
                will(returnValue(xdoc));
        }});

        Document doc = this.manager.getEntity(new UniqueReference(documentReference));
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
            oneOf(getContext().getWiki()).getDocument(documentReference, getContext());
            will(throwException(new XWikiException()));
        }});

        try {
            this.manager.getEntity(new UniqueReference(documentReference));
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
            oneOf(getContext().getWiki()).getServerURL("wiki", getContext());
                will(returnValue(new URL("http://whatever/not/null")));
        }});

        Wiki wiki = this.manager.getEntity(new UniqueReference(wikiReference));
        Assert.assertNotNull(wiki);
        Assert.assertFalse(wiki.isNew());
    }

    @Test
    public void getEntityForWikiThatDoesntExists() throws Exception
    {
        final WikiReference wikiReference = new WikiReference("wiki");
        getMockery().checking(new Expectations() {{
            oneOf(getContext().getWiki()).getServerURL("wiki", getContext());
                will(returnValue(null));
        }});

        Wiki wiki = this.manager.getEntity(new UniqueReference(wikiReference));
        Assert.assertNull(wiki);
    }

    @Test
    public void hasEntityForDocumentThatExists() throws Exception
    {
        final DocumentReference documentReference = new DocumentReference("wiki", "space", "page");
        getMockery().checking(new Expectations() {{
            oneOf(getContext().getWiki()).exists(documentReference, getContext());
                will(returnValue(true));
        }});

        Assert.assertTrue(this.manager.hasEntity(new UniqueReference(documentReference)));
    }

    @Test
    public void hasEntityForWikiThatExists() throws Exception
    {
        final WikiReference wikiReference = new WikiReference("wiki");
        getMockery().checking(new Expectations() {{
            oneOf(getContext().getWiki()).getServerURL("wiki", getContext());
                will(returnValue(new URL("http://whatever/not/null")));
        }});

        Assert.assertTrue(this.manager.hasEntity(new UniqueReference(wikiReference)));
    }

    @Test
    public void hasEntityForWikiThatDoesntExist() throws Exception
    {
        final WikiReference wikiReference = new WikiReference("wiki");
        getMockery().checking(new Expectations() {{
            oneOf(getContext().getWiki()).getServerURL("wiki", getContext());
                will(returnValue(null));
        }});

        Assert.assertFalse(this.manager.hasEntity(new UniqueReference(wikiReference)));
    }

    @Test
    public void getEntityForObjectThatExists() throws Exception
    {
        final DocumentReference documentReference = new DocumentReference("wiki", "space", "page");
        final ObjectReference objectReference = new ObjectReference("object", documentReference);
        final XWikiDocument xdoc = getMockery().mock(XWikiDocument.class);
        final BaseObject baseObject = getMockery().mock(BaseObject.class);
        getMockery().checking(new Expectations() {{
            oneOf(getContext().getWiki()).getDocument(documentReference, getContext());
                will(returnValue(xdoc));
            oneOf(xdoc).getXObject(objectReference);
                will(returnValue(baseObject));
        }});

        org.xwiki.model.Object object = this.manager.getEntity(new UniqueReference(objectReference));
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
            oneOf(getContext().getWiki()).getDocument(documentReference, getContext());
                will(returnValue(xdoc));
            oneOf(xdoc).getXObject(objectReference);
                will(returnValue(null));
        }});

        org.xwiki.model.Object object = this.manager.getEntity(new UniqueReference(objectReference));
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
            oneOf(getContext().getWiki()).getDocument(documentReference, getContext());
                will(returnValue(xdoc));
        }});

        org.xwiki.model.Object object = this.manager.getEntity(new UniqueReference(objectReference));
        Assert.assertNull(object);
    }

    @Test
    public void getEntityForSpaceThatExists() throws Exception
    {
        final SpaceReference spaceReference = new SpaceReference("space", new WikiReference("wiki"));

        getMockery().checking(new Expectations() {{
            oneOf(getContext().getWiki()).getSpaces(getContext());
            will(returnValue(Arrays.asList("space", "otherspace")));
        }});

        Space space = this.manager.getEntity(new UniqueReference(spaceReference));
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
        final PropertyInterface propertyInterface = getMockery().mock(PropertyInterface.class);

        getMockery().checking(new Expectations() {{
            oneOf(getContext().getWiki()).getDocument(documentReference, getContext());
            will(returnValue(xdoc));
            oneOf(xdoc).getXObject(objectReference);
            will(returnValue(baseObject));
            oneOf(baseObject).get("property");
            will(returnValue(propertyInterface));
        }});

        ObjectProperty objectProperty = this.manager.getEntity(new UniqueReference(propertyReference));
        Assert.assertNotNull(objectProperty);
        Assert.assertFalse(objectProperty.isNew());
    }
}
