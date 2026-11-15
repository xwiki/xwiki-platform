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
package org.xwiki.rest.internal.resources;

import java.net.URI;
import java.util.List;

import jakarta.inject.Named;
import jakarta.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.localization.LocalizationContext;
import org.xwiki.model.reference.EntityReferenceProvider;
import org.xwiki.query.Query;
import org.xwiki.query.QueryFilter;
import org.xwiki.query.QueryManager;
import org.xwiki.rest.internal.ModelFactory;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWikiContext;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.xwiki.rest.internal.resources.KeywordSearchScope.CONTENT;

/**
 * Unit tests for {@link DatabaseKeywordSearchSource}, focusing on the order-field HQL injection guard and the
 * {@code null} keywords short-circuit.
 *
 * @version $Id$
 */
@ComponentTest
class DatabaseKeywordSearchSourceTest
{
    @InjectMockComponents
    private DatabaseKeywordSearchSource source;

    @MockComponent
    private ContextualAuthorizationManager authorizationManager;

    @MockComponent
    @Named("hidden/space")
    private QueryFilter hiddenSpaceFilter;

    @MockComponent
    @Named("hidden/document")
    private QueryFilter hiddenDocumentFilter;

    @MockComponent
    private LocalizationContext localizationContext;

    @MockComponent
    private ModelFactory modelFactory;

    @MockComponent
    private EntityReferenceProvider defaultEntityReferenceProvider;

    @MockComponent
    private QueryManager queryManager;

    @MockComponent
    @Named("secure")
    private QueryManager secureQueryManager;

    @MockComponent
    private Provider<XWikiContext> contextProvider;

    private Query query;

    @BeforeEach
    void setUp() throws Exception
    {
        XWikiContext context = mock(XWikiContext.class);
        when(this.contextProvider.get()).thenReturn(context);
        when(context.getWikiId()).thenReturn("xwiki");

        // Fluent Query mock: builder methods return the mock, execute() returns no rows.
        this.query = mock(Query.class, RETURNS_SELF);
        when(this.query.execute()).thenReturn(List.of());
        when(this.queryManager.createQuery(any(), any())).thenReturn(this.query);
        when(this.secureQueryManager.createQuery(any(), any())).thenReturn(this.query);
    }

    private KeywordSearchOptions options(String orderField)
    {
        return KeywordSearchOptions.builder()
            .searchScopes(List.of(CONTENT))
            .orderField(orderField)
            .order("asc")
            .number(10)
            .start(0)
            .withPrettyNames(false)
            .isLocaleAware(false)
            .build();
    }

    @Test
    void alphanumericOrderFieldUsesPlainQueryManager() throws Exception
    {
        this.source.search("hello", options("title"), URI.create("http://localhost"));

        verify(this.queryManager).createQuery(any(), eq(Query.HQL));
        verify(this.secureQueryManager, never()).createQuery(any(), any());
    }

    @Test
    void nonAlphanumericOrderFieldUsesSecureQueryManager() throws Exception
    {
        // A non-alphanumeric order field (here containing a dot) must be routed through the secure query manager,
        // which validates the HQL -- this is the injection defense.
        this.source.search("hello", options("doc.creationDate"), URI.create("http://localhost"));

        verify(this.secureQueryManager).createQuery(any(), eq(Query.HQL));
        verify(this.queryManager, never()).createQuery(any(), any());
    }

    @Test
    void nullKeywordsReturnsNoResultsAndRunsNoQuery() throws Exception
    {
        List<?> results = this.source.search(null, options("title"), URI.create("http://localhost"));

        assertTrue(results.isEmpty());
        verify(this.queryManager, never()).createQuery(any(), any());
        verify(this.secureQueryManager, never()).createQuery(any(), any());
    }
}
