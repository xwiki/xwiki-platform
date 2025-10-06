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
package org.xwiki.search.solr.internal.rest;

import java.util.List;

import javax.inject.Provider;

import org.apache.commons.lang3.mutable.Mutable;
import org.apache.commons.lang3.mutable.MutableObject;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceProvider;
import org.xwiki.properties.ConverterManager;
import org.xwiki.query.QueryManager;
import org.xwiki.query.SecureQuery;
import org.xwiki.rest.internal.resources.KeywordSearchOptions;
import org.xwiki.rest.internal.resources.KeywordSearchScope;
import org.xwiki.rest.internal.resources.wikis.WikiSearchResourceImpl;
import org.xwiki.search.solr.internal.DefaultSolrUtils;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;
import org.xwiki.user.CurrentUserReference;
import org.xwiki.user.UserProperties;
import org.xwiki.user.UserPropertiesResolver;

import com.xpn.xwiki.XWikiContext;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link WikiSearchResourceImpl}.
 *
 * @version $Id$
 */
@ComponentTest
@ComponentList(DefaultSolrUtils.class)
class SolrKeywordSearchSourceTest
{
    private static final KeywordSearchOptions DEFAULT_OPTIONS = KeywordSearchOptions.builder()
        .wikiName("wiki")
        .searchScopes(List.of(KeywordSearchScope.TITLE))
        .number(10)
        .order("asc")
        .withPrettyNames(Boolean.TRUE)
        .build();

    @InjectMockComponents
    private SolrKeywordSearchSource searchSource;

    @MockComponent
    private Provider<XWikiContext> xwikiContextProvider;

    @MockComponent
    private EntityReferenceProvider defaultEntityReferenceProvider;

    @MockComponent
    private UserPropertiesResolver userPropertiesResolver;

    @MockComponent
    private DocumentReferenceResolver<SolrDocument> solrDocumentDocumentReferenceResolver;

    @MockComponent
    private QueryManager queryManager;

    /**
     * Converter needed for DefaultSolrUtils.
     */
    @MockComponent
    private ConverterManager converterManager;

    @Mock
    private SecureQuery query;

    @Mock
    private UserProperties userProperties;

    private final SolrDocumentList results = new SolrDocumentList();

    private XWikiContext xwikiContext;

    @BeforeComponent
    void setup(MockitoComponentManager componentManager) throws Exception
    {
        componentManager.registerComponent(ComponentManager.class, "context", componentManager);
        this.xwikiContext = mock();
        when(this.xwikiContextProvider.get()).thenReturn(this.xwikiContext);
    }

    @BeforeEach
    void configure() throws Exception
    {
        // Let the context "remember" the set wiki.
        Mutable<String> wikiId = new MutableObject<>("s1");
        when(this.xwikiContext.getWikiId()).then(invocation -> wikiId.getValue());
        doAnswer(invocation -> {
            wikiId.setValue(invocation.getArgument(0));
            return null;
        }).when(this.xwikiContext).setWikiId(ArgumentMatchers.any());

        when(this.userPropertiesResolver.resolve(CurrentUserReference.INSTANCE)).thenReturn(this.userProperties);
        when(this.defaultEntityReferenceProvider.getDefaultReference(EntityType.DOCUMENT))
            .thenReturn(new DocumentReference("xwiki", "Main", "WebHome"));
        when(this.queryManager.createQuery(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(this.query);
        QueryResponse queryResponse = mock();
        when(this.query.execute()).thenReturn(List.of(queryResponse));
        when(queryResponse.getResults()).thenReturn(this.results);
    }

    @Test
    void searchInTitle() throws Exception
    {
        this.searchSource.search("my search", DEFAULT_OPTIONS, mock());
        verify(this.queryManager).createQuery("(title:my\\ search OR title_:search*)^4", "solr");
        verify(this.query).bindValue("fq", List.of("type:DOCUMENT", "hidden:false", "wiki:wiki"));
    }

    @Test
    void searchInContent() throws Exception
    {
        KeywordSearchOptions searchOptions =
            DEFAULT_OPTIONS.but().searchScopes(List.of(KeywordSearchScope.CONTENT)).build();
        this.searchSource.search("my search", searchOptions, mock());
        verify(this.queryManager).createQuery("doccontent:my\\ search", "solr");
    }

    @Test
    void searchInNameAndContent() throws Exception
    {
        KeywordSearchOptions searchOptions =
            DEFAULT_OPTIONS.but().searchScopes(List.of(KeywordSearchScope.NAME, KeywordSearchScope.CONTENT)).build();
        this.searchSource.search("my search", searchOptions, mock());
        verify(this.queryManager).createQuery("spaces:my\\ search OR spaces:search*"
            + " OR (name:my\\ search OR name:search* -name_exact:WebHome)^2"
            + " OR (reference:document\\:wiki\\:my\\ search)^1000"
            + " OR doccontent:my\\ search", "solr");
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void searchRespectsHidden(boolean showHidden) throws Exception
    {
        when(this.userProperties.displayHiddenDocuments()).thenReturn(showHidden);
        this.searchSource.search("my search", DEFAULT_OPTIONS, mock());
        verify(this.query, showHidden ? never() : times(1)).bindValue(eq("fq"),
            ArgumentMatchers.argThat(arg -> arg instanceof List<?> list && list.contains("hidden:false")));
    }
}
