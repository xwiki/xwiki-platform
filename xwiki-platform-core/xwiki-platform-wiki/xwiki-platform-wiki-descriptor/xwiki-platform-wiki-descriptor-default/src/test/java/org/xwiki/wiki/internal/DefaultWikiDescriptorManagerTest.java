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
package org.xwiki.wiki.internal;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryExecutor;
import org.xwiki.query.QueryManager;
import org.xwiki.query.internal.DefaultQuery;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.wiki.WikiDescriptor;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link DefaultWikiDescriptorManager}.
 *
 * @version $Id$
 * @since 5.1M1
 */
public class DefaultWikiDescriptorManagerTest
{
    @Rule
    public MockitoComponentMockingRule<DefaultWikiDescriptorManager> mocker =
        new MockitoComponentMockingRule(DefaultWikiDescriptorManager.class);

    @Test
    public void getByWikiAliasWhenNotInCacheButExists() throws Exception
    {
        // Return "space.page" document name when querying the DB for a XWiki.XWikiServerClass matching the alias
        QueryManager queryManager = this.mocker.getInstance(QueryManager.class);
        QueryExecutor queryExecutor = mock(QueryExecutor.class);
        when(queryManager.createQuery("where doc.object(XWiki.XWikiServerClass).server = :wikiAlias and "
            + "doc.name like 'XWikiServer%'", Query.XWQL)).thenReturn(
            new DefaultQuery("statement", "language", queryExecutor));
        when(queryExecutor.<String>execute(any(Query.class))).thenReturn(Arrays.asList("space.page"));

        // Convert the returned document name represented as a String into a Document Reference
        DocumentReferenceResolver<String> resolver =
            this.mocker.getInstance(DocumentReferenceResolver.TYPE_STRING, "current");
        DocumentReference reference = new DocumentReference("xwiki", "space", "page");
        when(resolver.resolve("space.page")).thenReturn(reference);

        // Get the XWikiDocument for the Document Reference
        Execution execution = this.mocker.getInstance(Execution.class);
        XWikiContext xcontext = mock(XWikiContext.class);
        com.xpn.xwiki.XWiki xwiki = mock(com.xpn.xwiki.XWiki.class);
        when(xcontext.getWiki()).thenReturn(xwiki);
        when(xcontext.getMainXWiki()).thenReturn("xwiki");
        ExecutionContext ec = new ExecutionContext();
        ec.setProperty("xwikicontext", xcontext);
        when(execution.getContext()).thenReturn(ec);
        XWikiDocument document = mock(XWikiDocument.class);
        when(xwiki.getDocument(eq(reference), any(XWikiContext.class))).thenReturn(document);

        // Get all XWiki.XWikiServerClass XObjects to pass to the Wiki Descriptor Builder
        List<BaseObject> baseObjects = Arrays.asList(mock(BaseObject.class));
        when(document.getXObjects(any(EntityReference.class))).thenReturn(baseObjects);

        // Get a WikiDescriptor from the Wiki Descriptor Builder
        WikiDescriptorBuilder wikiDescriptorBuilder = this.mocker.getInstance(WikiDescriptorBuilder.class);
        WikiDescriptor descriptor = new WikiDescriptor("wikiid", "wikialias");
        when(wikiDescriptorBuilder.build(anyListOf(BaseObject.class), any(XWikiDocument.class),
            any(XWikiContext.class))).thenReturn(descriptor);

        assertEquals(descriptor, this.mocker.getComponentUnderTest().getByWikiAlias("wikialias"));

        // Verify that calling getByWikiId() doesn't call XWiki.getDocument() since getByWikiAlias should have put the
        // descriptor in the wikiId cache too.
        assertEquals(descriptor, this.mocker.getComponentUnderTest().getByWikiId("wikiid"));

        DocumentReference dr = new DocumentReference("xwiki", "XWiki", "XWikiServerWikiid");
        verify(xwiki, never()).getDocument(eq(dr), any(XWikiContext.class));
    }

    @Test
    public void getByWikiAliasWhenInCache() throws Exception
    {
        // Descriptor is in cache...
        WikiDescriptor descriptor = new WikiDescriptor("wikiid", "wikialias");
        this.mocker.getComponentUnderTest().set(descriptor);

        assertEquals(descriptor, this.mocker.getComponentUnderTest().getByWikiAlias("wikialias"));
    }

    @Test
    public void getByWikiAliasWhenNotInCacheAndItDoesntExist() throws Exception
    {
        // No result when querying the DB for a XWiki.XWikiServerClass matching the alias
        QueryManager queryManager = this.mocker.getInstance(QueryManager.class);
        QueryExecutor queryExecutor = mock(QueryExecutor.class);
        when(queryManager.createQuery("where doc.object(XWiki.XWikiServerClass).server = :wikiAlias and "
            + "doc.name like 'XWikiServer%'", Query.XWQL)).thenReturn(
            new DefaultQuery("statement", "language", queryExecutor));
        when(queryExecutor.<String>execute(any(Query.class))).thenReturn(Collections.EMPTY_LIST);

        // Get the main wiki
        Execution execution = this.mocker.getInstance(Execution.class);
        XWikiContext xcontext = mock(XWikiContext.class);
        when(xcontext.getMainXWiki()).thenReturn("xwiki");
        ExecutionContext ec = new ExecutionContext();
        ec.setProperty("xwikicontext", xcontext);
        when(execution.getContext()).thenReturn(ec);

        assertNull(this.mocker.getComponentUnderTest().getByWikiAlias("wikialias"));
    }

    @Test
    public void getByWikiIdWhenNotInCacheButExists() throws Exception
    {
        // Get the XWikiDocument for the Document Reference
        Execution execution = this.mocker.getInstance(Execution.class);
        XWikiContext xcontext = mock(XWikiContext.class);
        com.xpn.xwiki.XWiki xwiki = mock(com.xpn.xwiki.XWiki.class);
        when(xcontext.getWiki()).thenReturn(xwiki);
        when(xcontext.getMainXWiki()).thenReturn("xwiki");
        ExecutionContext ec = new ExecutionContext();
        ec.setProperty("xwikicontext", xcontext);
        when(execution.getContext()).thenReturn(ec);
        XWikiDocument document = mock(XWikiDocument.class);
        DocumentReference reference = new DocumentReference("xwiki", "XWiki", "XWikiServerWikiid");
        when(xwiki.getDocument(eq(reference), any(XWikiContext.class))).thenReturn(document);
        when(document.isNew()).thenReturn(false);

        // Get all XWiki.XWikiServerClass XObjects to pass to the Wiki Descriptor Builder
        List<BaseObject> baseObjects = Arrays.asList(mock(BaseObject.class));
        when(document.getXObjects(any(EntityReference.class))).thenReturn(baseObjects);

        // Get a WikiDescriptor from the Wiki Descriptor Builder
        WikiDescriptorBuilder wikiDescriptorBuilder = this.mocker.getInstance(WikiDescriptorBuilder.class);
        WikiDescriptor descriptor = new WikiDescriptor("wikiid", "wikialias");
        when(wikiDescriptorBuilder.build(anyListOf(BaseObject.class), any(XWikiDocument.class),
            any(XWikiContext.class))).thenReturn(descriptor);

        assertEquals(descriptor, this.mocker.getComponentUnderTest().getByWikiId("wikiid"));

        // Verify that calling getByWikiAlias() doesn't need any database calls (since we don't mock the Query Manager
        // if it were calling it we would get an error) since getByWikiId() has put the descriptor in the wikiAlias
        // cache too.
        assertEquals(descriptor, this.mocker.getComponentUnderTest().getByWikiAlias("wikialias"));
    }

    @Test
    public void getByWikiIdWhenInCache() throws Exception
    {
        // Descriptor is in cache...
        WikiDescriptor descriptor = new WikiDescriptor("wikiid", "wikialias");
        this.mocker.getComponentUnderTest().set(descriptor);

        assertEquals(descriptor, this.mocker.getComponentUnderTest().getByWikiId("wikiid"));
    }

    @Test
    public void getByWikiIdWhenNotInCacheAndItDoesntExist() throws Exception
    {
        // Get the XWikiDocument for the Document Reference but mark it as new (meaning that it doesn't exist)
        Execution execution = this.mocker.getInstance(Execution.class);
        XWikiContext xcontext = mock(XWikiContext.class);
        com.xpn.xwiki.XWiki xwiki = mock(com.xpn.xwiki.XWiki.class);
        when(xcontext.getWiki()).thenReturn(xwiki);
        when(xcontext.getMainXWiki()).thenReturn("xwiki");
        ExecutionContext ec = new ExecutionContext();
        ec.setProperty("xwikicontext", xcontext);
        when(execution.getContext()).thenReturn(ec);
        XWikiDocument document = mock(XWikiDocument.class);
        DocumentReference reference = new DocumentReference("xwiki", "XWiki", "XWikiServerWikiid");
        when(xwiki.getDocument(eq(reference), any(XWikiContext.class))).thenReturn(document);
        when(document.isNew()).thenReturn(true);

        assertNull(this.mocker.getComponentUnderTest().getByWikiId("wikiid"));
    }

    @Test
    public void getAll() throws Exception
    {
        QueryManager queryManager = this.mocker.getInstance(QueryManager.class);
        QueryExecutor queryExecutor = mock(QueryExecutor.class);
        when(queryManager.createQuery("from doc.object(XWiki.XWikiServerClass) as descriptor and "
            + "doc.name like 'XWikiServer%'", Query.XWQL)).thenReturn(
            new DefaultQuery("statement", "language", queryExecutor));
        when(queryExecutor.<String>execute(any(Query.class))).thenReturn(Arrays.asList("space1.page1", "space2.page2"));

        // Convert the returned document names represented as Strings into Document References
        DocumentReferenceResolver<String> resolver =
            this.mocker.getInstance(DocumentReferenceResolver.TYPE_STRING, "current");
        DocumentReference reference1 = new DocumentReference("xwiki", "space1", "page1");
        when(resolver.resolve("space1.page1")).thenReturn(reference1);
        DocumentReference reference2 = new DocumentReference("xwiki", "space2", "page2");
        when(resolver.resolve("space2.page2")).thenReturn(reference2);

        // Get the XWikiDocuments for the Document References
        Execution execution = this.mocker.getInstance(Execution.class);
        XWikiContext xcontext = mock(XWikiContext.class);
        com.xpn.xwiki.XWiki xwiki = mock(com.xpn.xwiki.XWiki.class);
        when(xcontext.getWiki()).thenReturn(xwiki);
        when(xcontext.getMainXWiki()).thenReturn("xwiki");
        ExecutionContext ec = new ExecutionContext();
        ec.setProperty("xwikicontext", xcontext);
        when(execution.getContext()).thenReturn(ec);
        XWikiDocument document1 = mock(XWikiDocument.class);
        when(xwiki.getDocument(eq(reference1), any(XWikiContext.class))).thenReturn(document1);
        XWikiDocument document2 = mock(XWikiDocument.class);
        when(xwiki.getDocument(eq(reference2), any(XWikiContext.class))).thenReturn(document2);

        // Get all XWiki.XWikiServerClass XObjects to pass to the Wiki Descriptor Builder
        List<BaseObject> baseObjects = Arrays.asList(mock(BaseObject.class));
        when(document1.getXObjects(any(EntityReference.class))).thenReturn(baseObjects);
        when(document2.getXObjects(any(EntityReference.class))).thenReturn(baseObjects);

        // Get a WikiDescriptor from the Wiki Descriptor Builder
        WikiDescriptorBuilder wikiDescriptorBuilder = this.mocker.getInstance(WikiDescriptorBuilder.class);
        WikiDescriptor descriptor1 = new WikiDescriptor("wikiid1", "wikialias1");
        WikiDescriptor descriptor2 = new WikiDescriptor("wikiid2", "wikialias2");
        when(wikiDescriptorBuilder.build(anyListOf(BaseObject.class), any(XWikiDocument.class),
            any(XWikiContext.class))).thenReturn(descriptor1, descriptor2);

        Collection<WikiDescriptor> descriptors = this.mocker.getComponentUnderTest().getAll();
        assertEquals(2, descriptors.size());

        // Verify all descriptors were put in cache
        assertEquals(descriptor1, this.mocker.getComponentUnderTest().getByWikiAlias("wikialias1"));
        assertEquals(descriptor2, this.mocker.getComponentUnderTest().getByWikiAlias("wikialias2"));
        assertEquals(descriptor1, this.mocker.getComponentUnderTest().getByWikiId("wikiid1"));
        assertEquals(descriptor2, this.mocker.getComponentUnderTest().getByWikiId("wikiid2"));
    }
}
