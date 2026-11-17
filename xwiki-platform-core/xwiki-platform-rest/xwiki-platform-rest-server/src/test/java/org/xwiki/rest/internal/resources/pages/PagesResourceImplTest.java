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
package org.xwiki.rest.internal.resources.pages;

import java.time.Duration;
import java.util.List;

import javax.inject.Named;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryFilter;
import org.xwiki.rest.internal.ModelFactory;
import org.xwiki.rest.model.jaxb.PageSummary;
import org.xwiki.rest.model.jaxb.Pages;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.test.reference.ReferenceComponentList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link PagesResourceImpl}.
 *
 * @version $Id$
 */
@OldcoreTest
@ReferenceComponentList
class PagesResourceImplTest
{
    private static final String WIKI = "xwiki";

    private static final String SPACE = "Space";

    @InjectMockComponents
    private PagesResourceImpl pagesResource;

    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    @MockComponent
    private ModelFactory modelFactory;

    @MockComponent
    @Named("hidden")
    private QueryFilter hiddenFilter;

    @Mock
    private UriInfo uriInfo;

    @Mock(answer = Answers.RETURNS_SELF)
    private Query query;

    @BeforeEach
    void setUp() throws Exception
    {
        FieldUtils.writeField(this.pagesResource, "uriInfo", this.uriInfo, true);

        when(this.oldcore.getMockContextualAuthorizationManager().hasAccess(eq(Right.VIEW), any())).thenReturn(true);

        when(this.oldcore.getQueryManager().getNamedQuery("getSpaceDocsName")).thenReturn(this.query);

        when(this.modelFactory.toRestPageSummary(any(), any(), anyBoolean())).then(this::createPageSummary);
    }

    @Test
    void getPagesWithoutParentFilter() throws Exception
    {
        savePages();

        assertEquals(List.of("xwiki:Space.Parent", "xwiki:Space.Child"), getPageIds(null));
    }

    @Test
    void getPagesFilteredByParentRegularExpression() throws Exception
    {
        savePages();

        assertEquals(List.of("xwiki:Space.Child"), getPageIds("xwiki:Space\\.P.*"));
        assertEquals(List.of(), getPageIds("xwiki:Other\\..*"));
    }

    @Test
    void getPagesWithoutParent() throws Exception
    {
        savePages();

        assertEquals(List.of("xwiki:Space.Parent"), getPageIds("null"));
    }

    @Test
    void getPagesWithInvalidParentFilter() throws Exception
    {
        WebApplicationException exception = assertThrows(WebApplicationException.class, () -> getPageIds("["));

        assertEquals(400, exception.getResponse().getStatus());
        assertEquals("Invalid parentId filter.", exception.getResponse().getEntity());
        verify(this.query, never()).execute();
    }

    @Test
    void getPagesWithTooExpensiveParentFilter() throws Exception
    {
        savePages();

        WebApplicationException exception = assertThrows(WebApplicationException.class,
            () -> assertTimeoutPreemptively(Duration.ofSeconds(10), () -> getPageIds("(.|.|.|.){30}Z")));

        assertEquals(400, exception.getResponse().getStatus());
        assertEquals("The parentId filter is too expensive to evaluate.", exception.getResponse().getEntity());
    }

    @Test
    void getPagesWithSlowPagesAndCheapParentFilter() throws Exception
    {
        savePages();
        // Make the processing of the first page alone longer than the parent filter time budget.
        doAnswer(invocation -> {
            // This sleep is intentional and is meant to simulate a slowness on a part of the execution that
            // shouldn't be taken into account by the regex time execution budget.
            Thread.sleep(1100);
            return createPageSummary(invocation);
        }).when(this.modelFactory).toRestPageSummary(any(), any(), anyBoolean());

        assertEquals(List.of("xwiki:Space.Parent", "xwiki:Space.Child"), getPageIds(".*"));
    }

    @Test
    void getPagesWithParentFilterTooDeepToEvaluate() throws Exception
    {
        String longName = "x".repeat(100_000);
        savePage(longName, null);
        savePage("Child", longName);
        when(this.query.execute()).thenReturn(List.of("Child"));

        WebApplicationException exception =
            assertThrows(WebApplicationException.class, () -> getPageIds("xwiki:Space\\.(x|y)*"));

        assertEquals(400, exception.getResponse().getStatus());
        assertEquals("The parentId filter is too expensive to evaluate.", exception.getResponse().getEntity());
    }

    private void savePages() throws Exception
    {
        savePage("Parent", null);
        savePage("Child", "Parent");
        when(this.query.execute()).thenReturn(List.of("Parent", "Child"));
    }

    private void savePage(String name, String parent) throws Exception
    {
        XWikiDocument document = new XWikiDocument(new DocumentReference(WIKI, SPACE, name));
        if (parent != null) {
            document.setParentReference(new LocalDocumentReference(SPACE, parent));
        }
        this.oldcore.getSpyXWiki().saveDocument(document, this.oldcore.getXWikiContext());
    }

    private PageSummary createPageSummary(InvocationOnMock invocation)
    {
        PageSummary pageSummary = new PageSummary();
        pageSummary.setId(invocation.<Document>getArgument(1).getPrefixedFullName());
        return pageSummary;
    }

    private List<String> getPageIds(String parentFilterExpression) throws Exception
    {
        Pages pages = this.pagesResource.getPages(WIKI, SPACE, 0, null, parentFilterExpression, null, false);
        return pages.getPageSummaries().stream().map(PageSummary::getId).toList();
    }
}
