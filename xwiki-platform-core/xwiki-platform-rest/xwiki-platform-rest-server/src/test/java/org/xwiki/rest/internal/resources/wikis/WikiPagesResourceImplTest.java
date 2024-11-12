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
package org.xwiki.rest.internal.resources.wikis;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import javax.inject.Named;
import javax.inject.Provider;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.rest.XWikiRestException;
import org.xwiki.rest.model.jaxb.PageSummary;
import org.xwiki.rest.model.jaxb.Pages;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.web.Utils;
import com.xpn.xwiki.web.XWikiURLFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link WikiPagesResourceImpl}.
 *
 * @version $Id$
 * @since 16.10.0RC1
 */
@ComponentTest
class WikiPagesResourceImplTest
{
    @InjectMockComponents
    private WikiPagesResourceImpl wikiPagesResource;

    @MockComponent
    private QueryManager queryManager;

    @MockComponent
    private ContextualAuthorizationManager contextualAuthorizationManager;

    @MockComponent
    @Named("context")
    private ComponentManager contextComponentManager;
    
    @MockComponent
    @Named("local")
    private EntityReferenceSerializer<String> localEntityReferenceSerializer;

    @MockComponent
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @Mock
    private UriInfo uriInfo;

    @Mock
    private XWikiURLFactory urlFactory;

    private XWikiContext context;

    @BeforeEach
    void setUp(MockitoComponentManager componentManager)
        throws ComponentLookupException, URISyntaxException, IllegalAccessException
    {
        Utils.setComponentManager(componentManager);

        // Because XWikiResource injects the context component manager, it exists as a mock, and we thus need to mock
        // its behavior - otherwise it would just be ignored.
        when(this.contextComponentManager.getInstance(any()))
            .thenAnswer(invocation -> componentManager.getInstance(invocation.getArgument(0)));
        when(this.contextComponentManager.getInstance(any(), any()))
            .thenAnswer(
                invocation -> componentManager.getInstance(invocation.getArgument(0), invocation.getArgument(1)));

        when(this.uriInfo.getBaseUri()).thenReturn(new URI("https://test/"));
        FieldUtils.writeField(this.wikiPagesResource, "uriInfo", this.uriInfo, true);
        Provider<XWikiContext> contextProvider = componentManager.getInstance(XWikiContext.TYPE_PROVIDER);
        this.context = contextProvider.get();
        when(this.context.getURLFactory()).thenReturn(urlFactory);
    }

    @Test
    void getPages() throws QueryException, MalformedURLException, XWikiRestException
    {
        String wikiName = "foo";
        int start = 42;
        String name = "Bar";
        String space = "";
        String author = "buz";
        int number = 3;

        Query mockQuery = mock(Query.class);
        when(queryManager.createQuery(any(), eq(Query.XWQL))).thenReturn(mockQuery);
        when(mockQuery.setOffset(anyInt())).thenReturn(mockQuery);
        when(mockQuery.setLimit(anyInt())).thenReturn(mockQuery);
        when(mockQuery.setWiki(any())).thenReturn(mockQuery);

        XWikiDocument mockDoc1 = mock(XWikiDocument.class, "doc1");
        XWikiDocument mockDoc2 = mock(XWikiDocument.class, "doc2");
        XWikiDocument mockDoc3 = mock(XWikiDocument.class, "doc3");
        when(mockQuery.execute()).thenReturn(List.of(mockDoc1, mockDoc2, mockDoc3));

        DocumentReference doc1Ref = new DocumentReference("foo", "Space1", "Doc1");
        DocumentReference doc2Ref = new DocumentReference("foo", "Space2", "Doc2");
        DocumentReference doc3Ref = new DocumentReference("foo", "Space3", "Doc3");
        when(mockDoc1.getDocumentReference()).thenReturn(doc1Ref);
        when(mockDoc2.getDocumentReference()).thenReturn(doc2Ref);
        when(mockDoc3.getDocumentReference()).thenReturn(doc3Ref);

        when(this.contextualAuthorizationManager.hasAccess(Right.VIEW, doc1Ref)).thenReturn(true);
        when(this.contextualAuthorizationManager.hasAccess(Right.VIEW, doc2Ref)).thenReturn(false);
        when(this.contextualAuthorizationManager.hasAccess(Right.VIEW, doc3Ref)).thenReturn(true);

        String doc1Id = "doc1ID";
        String doc1FullName = "foo:Space1.Doc1";
        String doc1Space = "Space1";
        String doc1Title = "doc1Title";
        String doc1Parent = "doc1Parent";
        String doc1URI = "https://test/xwiki/bin/view/Space1/Doc1";
        URL doc1AbsoluteURL = new URL(doc1URI);
        String doc1RelativeURL = "/xwiki/bin/view/Space1/Doc1";
        
        when(this.localEntityReferenceSerializer.serialize(doc1Ref)).thenReturn(doc1FullName);
        when(this.entityReferenceSerializer.serialize(doc1Ref)).thenReturn(doc1Id);
        when(mockDoc1.getSpace()).thenReturn(doc1Space);
        when(mockDoc1.getParent()).thenReturn(doc1Parent);
        when(mockDoc1.getTitle()).thenReturn(doc1Title);
        when(this.urlFactory.createExternalURL(doc1Space,"Doc1","view", null, null,this.context))
            .thenReturn(doc1AbsoluteURL);
        when(this.urlFactory.getURL(doc1AbsoluteURL, this.context)).thenReturn(doc1RelativeURL);

        String doc3Id = "doc3ID";
        String doc3FullName = "foo:Space3.Doc3";
        String doc3Space = "Space3";
        String doc3Title = "doc3Title";
        String doc3Parent = "doc3Parent";
        String doc3URI = "https://test/xwiki/bin/view/Space3/Doc3";
        URL doc3AbsoluteURL = new URL(doc3URI);
        String doc3RelativeURL = "/xwiki/bin/view/Space3/Doc3";

        when(this.localEntityReferenceSerializer.serialize(doc3Ref)).thenReturn(doc3FullName);
        when(this.entityReferenceSerializer.serialize(doc3Ref)).thenReturn(doc3Id);
        when(mockDoc3.getSpace()).thenReturn(doc3Space);
        when(mockDoc3.getParent()).thenReturn(doc3Parent);
        when(mockDoc3.getTitle()).thenReturn(doc3Title);
        when(this.urlFactory.createExternalURL(doc3Space,"Doc3","view", null, null,this.context))
            .thenReturn(doc3AbsoluteURL);
        when(this.urlFactory.getURL(doc3AbsoluteURL, this.context)).thenReturn(doc3RelativeURL);

        Pages pages = this.wikiPagesResource.getPages(wikiName, start, name, space, author, number);
        List<PageSummary> pageSummaries = pages.getPageSummaries();
        assertEquals(2, pageSummaries.size());

        PageSummary doc1Summary = pageSummaries.get(0);
        assertEquals(doc1Id, doc1Summary.getId());
        assertEquals(doc1FullName, doc1Summary.getFullName());
        assertEquals(doc1Space, doc1Summary.getSpace());
        assertEquals(doc1Title, doc1Summary.getTitle());
        assertEquals(doc1Parent, doc1Summary.getParent());
        assertEquals(doc1Ref.getName(),doc1Summary.getName());
        assertEquals(wikiName, doc1Summary.getWiki());
        assertEquals(doc1AbsoluteURL.toString(), doc1Summary.getXwikiAbsoluteUrl());
        assertEquals(doc1RelativeURL, doc1Summary.getXwikiRelativeUrl());

        PageSummary doc3Summary = pageSummaries.get(1);
        assertEquals(doc3Id, doc3Summary.getId());
        assertEquals(doc3FullName, doc3Summary.getFullName());
        assertEquals(doc3Space, doc3Summary.getSpace());
        assertEquals(doc3Title, doc3Summary.getTitle());
        assertEquals(doc3Parent, doc3Summary.getParent());
        assertEquals(doc3Ref.getName(),doc3Summary.getName());
        assertEquals(wikiName, doc3Summary.getWiki());
        assertEquals(doc3AbsoluteURL.toString(), doc3Summary.getXwikiAbsoluteUrl());
        assertEquals(doc3RelativeURL, doc3Summary.getXwikiRelativeUrl());

        verify(this.queryManager).createQuery("select doc from XWikiDocument as doc where (upper(doc.contentAuthor) "
            + "like :author and upper(doc.fullName) like :name )", Query.XWQL);
        verify(mockQuery).setWiki(wikiName);
        verify(mockQuery).setOffset(start);
        verify(mockQuery).setLimit(number);
        verify(mockQuery).bindValue("name", "%BAR%");
        verify(mockQuery).bindValue("author", "%BUZ%");
    }
}