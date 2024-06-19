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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.List;

import javax.inject.Named;
import javax.inject.Provider;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.index.tree.PageHierarchy;
import org.xwiki.index.tree.PageHierarchy.ChildrenQuery;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.rest.internal.ModelFactory;
import org.xwiki.rest.model.jaxb.PageSummary;
import org.xwiki.rest.model.jaxb.Pages;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Unit tests for {@link WikiChildrenResourceImpl}.
 *
 * @version $Id$
 */
@ComponentTest
class WikiChildrenResourceImplTest
{
    @InjectMockComponents
    private WikiChildrenResourceImpl wikiChildrenResource;

    @MockComponent
    @Named("nestedpages")
    private PageHierarchy nestedPageHierarchy;

    @MockComponent
    private ContextualAuthorizationManager contextualAuthorizationManager;

    @MockComponent
    protected Provider<XWikiContext> xcontextProvider;

    @MockComponent
    protected ModelFactory modelFactory;

    @Mock
    private ChildrenQuery childrenQuery;

    private XWikiContext xcontext;

    @Mock
    private XWiki xwiki;

    @Mock
    private UriInfo uriInfo;

    private URI baseURI;

    @BeforeComponent
    void beforeComponent()
    {
        this.xcontext = mock(XWikiContext.class);
        when(this.xcontextProvider.get()).thenReturn(this.xcontext);
    }

    @BeforeEach
    void beforeEach() throws Exception
    {
        FieldUtils.writeField(this.wikiChildrenResource, "uriInfo", this.uriInfo, true);
        this.baseURI = new URI("https://test/");
        when(this.uriInfo.getBaseUri()).thenReturn(this.baseURI);

        when(this.xcontext.getWiki()).thenReturn(this.xwiki);
    }

    @Test
    void getChildrenNoRights() throws Exception
    {
        try {
            this.wikiChildrenResource.getChildren("wiki", 0, 10, "foo");
            fail();
        } catch (WebApplicationException e) {
            assertEquals(Response.Status.FORBIDDEN, e.getResponse().getStatusInfo());
        }
    }

    @Test
    void getChildren() throws Exception
    {
        WikiReference wikiReference = new WikiReference("wiki");
        when(this.contextualAuthorizationManager.hasAccess(Right.VIEW, wikiReference)).thenReturn(true);

        when(this.nestedPageHierarchy.getChildren(wikiReference)).thenReturn(this.childrenQuery);
        when(this.childrenQuery.withOffset(5)).thenReturn(this.childrenQuery);
        when(this.childrenQuery.withLimit(7)).thenReturn(this.childrenQuery);
        when(this.childrenQuery.matching("foo")).thenReturn(this.childrenQuery);

        DocumentReference aliceReference = new DocumentReference("wiki", "Alice", "WebHome");
        DocumentReference bobReference = new DocumentReference("wiki", "Bob", "WebHome");
        DocumentReference carolReference = new DocumentReference("wiki", "Carol", "WebHome");
        when(this.childrenQuery.getDocumentReferences()).thenReturn(List.of(aliceReference, bobReference));
        when(this.contextualAuthorizationManager.hasAccess(Right.VIEW, bobReference)).thenReturn(true);
        when(this.contextualAuthorizationManager.hasAccess(Right.VIEW, carolReference)).thenReturn(true);

        XWikiDocument bobDocument = mock(XWikiDocument.class);
        when(this.xwiki.getDocument(bobReference, this.xcontext)).thenReturn(bobDocument);
        Document bobApiDoc = mock(Document.class);
        when(bobDocument.newDocument(this.xcontext)).thenReturn(bobApiDoc);

        when(this.xwiki.getDocument(carolReference, this.xcontext)).thenThrow(new XWikiException(0, 0, "Carol failed"));

        PageSummary pageSummary = new PageSummary();
        when(this.modelFactory.toRestPageSummary(this.baseURI, bobApiDoc, true)).thenReturn(pageSummary);

        Pages pages = this.wikiChildrenResource.getChildren("wiki", 5, 7, "foo");
        assertEquals(List.of(pageSummary), pages.getPageSummaries());
    }
}
