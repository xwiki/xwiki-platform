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
package com.xpn.xwiki.doc;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.ObjectReferenceResolver;
import org.xwiki.query.Query;
import org.xwiki.query.QueryFilter;
import org.xwiki.query.QueryManager;
import org.xwiki.rendering.syntax.SyntaxFactory;
import org.xwiki.test.mockito.MockitoComponentManagerRule;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.web.Utils;

/**
 * Unit tests for {@link XWikiDocument}.
 * 
 * @version $Id$
 */
public class XWikiDocumentMockitoTest
{
    /**
     * A component manager that allows us to register mock components.
     */
    @Rule
    public MockitoComponentManagerRule mocker = new MockitoComponentManagerRule();

    /**
     * The object being tested.
     */
    private XWikiDocument document;

    /**
     * A mock {@link XWikiContext};
     */
    private XWikiContext context = mock(XWikiContext.class);

    @Before
    public void setUp() throws Exception
    {
        mocker.registerMockComponent(DocumentReferenceResolver.TYPE_STRING, "current");
        mocker.registerMockComponent(DocumentReferenceResolver.TYPE_STRING, "explicit");
        mocker.registerMockComponent(DocumentReferenceResolver.TYPE_STRING, "currentmixed");
        mocker.registerMockComponent(DocumentReferenceResolver.TYPE_REFERENCE, "current");
        mocker.registerMockComponent(DocumentReferenceResolver.TYPE_REFERENCE, "explicit");
        mocker.registerMockComponent(ObjectReferenceResolver.TYPE_REFERENCE, "current");
        mocker.registerMockComponent(EntityReferenceResolver.TYPE_STRING, "xclass");
        mocker.registerMockComponent(EntityReferenceResolver.TYPE_STRING, "relative");
        mocker.registerMockComponent(EntityReferenceSerializer.TYPE_STRING, "compact");
        mocker.registerMockComponent(EntityReferenceSerializer.TYPE_STRING, "default");
        mocker.registerMockComponent(EntityReferenceSerializer.TYPE_STRING, "compactwiki");
        mocker.registerMockComponent(EntityReferenceSerializer.TYPE_STRING, "local");
        mocker.registerMockComponent(EntityReferenceSerializer.TYPE_STRING, "uid");
        mocker.registerMockComponent(EntityReferenceSerializer.TYPE_STRING, "local/uid");
        mocker.registerMockComponent(SyntaxFactory.class);

        Utils.setComponentManager(mocker);
        document = new XWikiDocument(new DocumentReference("wiki", "Space", "Page"));
    }

    @Test
    public void getChildrenReferences() throws Exception
    {
        XWikiStoreInterface store = mock(XWikiStoreInterface.class);
        document.setStore(store);

        QueryManager queryManager = mock(QueryManager.class);
        when(store.getQueryManager()).thenReturn(queryManager);

        Query query = mock(Query.class);
        when(queryManager.createQuery(anyString(), eq(Query.XWQL))).thenReturn(query);

        QueryFilter hiddenFilter = mocker.registerMockComponent(QueryFilter.class, "hidden");

        when(query.setLimit(7)).thenReturn(query);

        List<Object[]> result = Arrays.asList(new Object[] {"X", "y"}, new Object[] {"A", "b"});
        when(query.<Object[]> execute()).thenReturn(result);

        List<DocumentReference> childrenReferences = document.getChildrenReferences(7, 3, context);

        verify(query).addFilter(hiddenFilter);
        verify(query).setLimit(7);
        verify(query).setOffset(3);

        Assert.assertEquals(2, childrenReferences.size());
        Assert.assertEquals(new DocumentReference("wiki", "X", "y"), childrenReferences.get(0));
        Assert.assertEquals(new DocumentReference("wiki", "A", "b"), childrenReferences.get(1));
    }
}
