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
package org.xwiki.tag.internal.selector;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.query.Query;
import org.xwiki.query.QueryManager;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.store.XWikiStoreInterface;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Test of {@link UnsafeTagsSelector}.
 *
 * @version $Id$
 * @since 15.0RC1
 * @since 14.4.8
 * @since 14.10.4
 */
@ComponentTest
class UnsafeTagsSelectorTest
{
    @InjectMockComponents
    private UnsafeTagsSelector tagsSelector;

    @MockComponent
    protected Provider<XWikiContext> contextProvider;

    @MockComponent
    protected ContextualAuthorizationManager contextualAuthorizationManager;

    @MockComponent
    @Named("current")
    protected DocumentReferenceResolver<String> stringDocumentReferenceResolver;

    @Mock
    private XWikiContext context;

    @Mock
    private XWiki wiki;

    @Mock
    private XWikiStoreInterface store;

    @Mock
    private QueryManager queryManager;

    @Mock
    private Query query;

    @BeforeEach
    void setUp() throws Exception
    {
        when(this.contextProvider.get()).thenReturn(this.context);
        when(this.context.getWiki()).thenReturn(this.wiki);
        when(this.wiki.getStore()).thenReturn(this.store);
        when(this.store.getQueryManager()).thenReturn(this.queryManager);
        when(this.queryManager.createQuery(anyString(), anyString())).thenReturn(this.query);
        when(this.query.addFilter(any())).thenReturn(this.query);
    }

    /**
     * The stream of arguments contains two {@link Arguments}
     * <ol>
     *     <li>The first one is a {@link List} of {@link String}. The elements represent tags returned from the
     *     database
     *     <li>The second one is a {@link List} of {@link String} of the expected tags
     * </ol>
     *
     * @return a {@link Stream} of arguments.
     */
    public static Stream<Arguments> getAllTagsSource()
    {
        return Stream.of(
            // No results
            Arguments.of(
                List.of(),
                List.of()
            ),
            // A single result, the page is viewable
            Arguments.of(
                List.of("Tag0"),
                List.of("Tag0")
            ),
            // Two pages, both are viewable
            Arguments.of(
                List.of("Page0", "All", "Page1", "all"),
                List.of("All", "all", "Page0", "Page1")
            )
        );
    }

    @ParameterizedTest
    @MethodSource("getAllTagsSource")
    void getAllTags(List<String> values, List<String> expectedTags)
        throws Exception
    {
        when(this.query.<String>execute()).thenReturn(new ArrayList<>(values));
        assertEquals(expectedTags, this.tagsSelector.getAllTags());
        verifyNoInteractions(this.stringDocumentReferenceResolver);
        verifyNoInteractions(this.contextualAuthorizationManager);
    }

    /**
     * The stream of arguments contains two {@link Arguments}
     * <ol>
     *     <li>The first one is a {@link List} of {@link String}. The elements represent tags returned from the
     *     database
     *     <li>The second one is a {@link Map} of {@link String} and {@link Integer} of the expected tags and their
     *     respective count
     * </ol>
     *
     * @return a {@link Stream} of arguments.
     */
    public static Stream<Arguments> getTagCountForQuerySource()
    {
        return Stream.of(
            // No results
            Arguments.of(
                List.of(),
                Map.of()
            ),
            // A single result, the page is viewable
            Arguments.of(
                List.of("Tag0"),
                Map.of("Tag0", 1)
            ),
            Arguments.of(
                List.of("Page0", "All", "Page1", "all"),
                Map.of(
                    "All", 2,
                    "Page0", 1,
                    "Page1", 1
                )
            )
        );
    }

    @ParameterizedTest
    @MethodSource("getTagCountForQuerySource")
    void getTagCountForQuery(List<String> values, Map<String, Integer> expectedTags) throws Exception
    {
        when(this.query.execute()).thenReturn(new ArrayList<>(values));
        assertEquals(expectedTags, this.tagsSelector.getTagCountForQuery(null, null, (List<Object>) null));
        verifyNoInteractions(this.stringDocumentReferenceResolver);
        verifyNoInteractions(this.contextualAuthorizationManager);
    }

    /**
     * The stream of arguments contains two {@link Arguments}
     * <ol>
     *     <li>The first one is a {@link List} of {@link String}. The elements represent document references of pages
     *     containing a given tag
     *     <li>The second one is a {@link Map} of {@link String} and {@link Integer} of the expected document references
     * </ol>
     *
     * @return a {@link Stream} of arguments.
     */
    public static Stream<Arguments> getDocumentsWithTagSource()
    {
        return Stream.of(
            Arguments.of(
                List.of(),
                List.of()
            ),
            Arguments.of(
                List.of("xwiki:Space.Page0", "xwiki:Space.Page1"),
                List.of("xwiki:Space.Page0", "xwiki:Space.Page1")
            )
        );
    }

    @ParameterizedTest
    @MethodSource("getDocumentsWithTagSource")
    void getDocumentsWithTag(List<String> results, List<String> expected) throws Exception
    {
        when(this.query.<String>execute()).thenReturn(results);

        assertEquals(expected, this.tagsSelector.getDocumentsWithTag("Tag0", false));
        verifyNoInteractions(this.stringDocumentReferenceResolver);
        verifyNoInteractions(this.contextualAuthorizationManager);
    }
}
