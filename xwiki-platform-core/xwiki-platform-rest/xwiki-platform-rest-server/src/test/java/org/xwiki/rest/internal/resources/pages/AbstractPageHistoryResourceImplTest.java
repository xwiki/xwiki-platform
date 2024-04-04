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

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

import javax.inject.Named;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.suigeneris.jrcs.rcs.Version;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.rest.XWikiResource;
import org.xwiki.rest.XWikiRestException;
import org.xwiki.rest.model.jaxb.History;
import org.xwiki.rest.model.jaxb.HistorySummary;
import org.xwiki.security.authorization.AccessDeniedException;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import com.xpn.xwiki.doc.rcs.XWikiRCSNodeId;
import com.xpn.xwiki.web.Utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Abstract base class for tests for page history with and without translation.
 *
 * @version $Id$
 */
public abstract class AbstractPageHistoryResourceImplTest
{
    protected static final String WIKI_NAME = "wiki";

    protected static final String PAGE_NAME = "page";

    protected static final int START = 8;

    protected static final int LIMIT = 11;

    private static final List<String> SPACE_NAMES = List.of("parent", "space");

    protected static final String SPACE_URL = String.join("/spaces/", SPACE_NAMES);

    private static final DocumentReference DOCUMENT_REFERENCE =
        new DocumentReference(WIKI_NAME, SPACE_NAMES, PAGE_NAME);

    private static final DocumentReference USER_REFERENCE = new DocumentReference("xwiki", "XWiki", "user");

    private static final String AUTHOR = "author";

    private static final String COMMENT = "comment";

    @MockComponent
    private QueryManager queryManager;

    @MockComponent
    private ContextualAuthorizationManager contextualAuthorizationManager;

    @MockComponent
    @Named("context")
    private ComponentManager contextComponentManager;

    @Mock
    private UriInfo uriInfo;

    protected abstract History getTranslationHistory() throws XWikiRestException;

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

        injectURIInfo();
    }

    abstract void injectURIInfo() throws IllegalAccessException;

    protected void injectURIInfo(XWikiResource resource) throws IllegalAccessException
    {
        FieldUtils.writeField(resource, "uriInfo", this.uriInfo, true);
    }

    @Test
    void getPageTranslationHistoryAccessDenied() throws AccessDeniedException, XWikiRestException
    {
        doThrow(new AccessDeniedException(Right.VIEW, DOCUMENT_REFERENCE, USER_REFERENCE))
            .when(this.contextualAuthorizationManager).checkAccess(Right.VIEW, DOCUMENT_REFERENCE);

        WebApplicationException exception = assertThrows(WebApplicationException.class, this::getTranslationHistory);

        assertEquals(401, exception.getResponse().getStatus());
    }

    @Test
    void getPageTranslationHistory() throws XWikiRestException, QueryException
    {
        Query mockQuery = mock();
        when(this.queryManager.createQuery(any(), eq(Query.XWQL))).thenReturn(mockQuery);
        when(mockQuery.bindValue(any(), any())).thenReturn(mockQuery);
        when(mockQuery.setOffset(anyInt())).thenReturn(mockQuery);
        when(mockQuery.setLimit(anyInt())).thenReturn(mockQuery);
        when(mockQuery.setWiki(any())).thenReturn(mockQuery);
        XWikiRCSNodeId nodeId = new XWikiRCSNodeId(DOCUMENT_REFERENCE.getWikiReference(), 42, new Version(2, 3));
        Instant now = Instant.now();
        String spaceName = String.join(".", SPACE_NAMES);
        when(mockQuery.execute()).thenReturn(Collections.singletonList(
            new Object[] { spaceName, PAGE_NAME, nodeId, Timestamp.from(now), AUTHOR, COMMENT }));
        History history = getTranslationHistory();
        // Verify that the query was correctly constructed.
        verify(mockQuery).bindValue("space", spaceName);
        verify(mockQuery).bindValue("name", PAGE_NAME);
        String language = getLanguage();
        if (language != null) {
            verify(mockQuery).bindValue("language", language);
        }
        verify(mockQuery).setOffset(START);
        verify(mockQuery).setLimit(LIMIT);
        verify(mockQuery).setWiki(WIKI_NAME);
        // Verify that the history summary was correctly constructed.
        assertEquals(1, history.getHistorySummaries().size());
        HistorySummary historySummary = history.getHistorySummaries().get(0);
        assertEquals("parent.space", historySummary.getSpace());
        assertEquals(PAGE_NAME, historySummary.getName());
        assertEquals(2, historySummary.getMajorVersion());
        assertEquals(3, historySummary.getMinorVersion());
        // Compare the modified date to the current time.
        assertEquals(now.getEpochSecond(), historySummary.getModified().getTime().getTime() / 1000);
        assertEquals(AUTHOR, historySummary.getModifier());
        assertEquals(COMMENT, historySummary.getComment());
        assertEquals(language, historySummary.getLanguage());
    }

    protected abstract String getLanguage();
}
