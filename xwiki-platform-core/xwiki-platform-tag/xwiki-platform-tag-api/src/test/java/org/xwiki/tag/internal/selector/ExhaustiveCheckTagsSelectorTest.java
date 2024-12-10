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
import org.xwiki.model.reference.DocumentReference;
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
import static org.mockito.Mockito.when;
import static org.xwiki.security.authorization.Right.VIEW;

/**
 * Test of {@link ExhaustiveCheckTagsSelector}.
 *
 * @version $Id$
 * @since 15.0RC1
 * @since 14.4.8
 * @since 14.10.4
 */
@ComponentTest
class ExhaustiveCheckTagsSelectorTest
{
    public static final DocumentReference PAGE0 = new DocumentReference("xwiki", "Space", "Page0");

    public static final DocumentReference PAGE1 = new DocumentReference("xwiki", "Space", "Page1");

    @InjectMockComponents
    private ExhaustiveCheckTagsSelector tagsSelector;

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
        initializeDocumentReferenceResolver();
    }

    /**
     * The stream of arguments contains three {@link Arguments}
     * <ol>
     *     <li>The first one is a {@link List} of {@link Object} arrays. Index 0 is a document reference, and Index 1
     *     is a tag. This corresponds to the values returned by the database. The order is important and the entries
     *     must be sorted by document reference
     *     <li>The second one is a {@link Map} of {@link DocumentReference} and {@link Boolean}. The document reference
     *     corresponds to the serialized forms from the first argument (two document references are available
     *     {@code xwiki:Space.Page0}, and {@code xwiki:Space.Page1})
     *     <li>The third one is a {@link List} of {@link String} of the expected tags
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
                Map.of(),
                List.of()
            ),
            // A single result, the page is viewable
            Arguments.of(
                List.<Object[]>of(
                    new Object[] { "xwiki:Space.Page0", "Tag0" }
                ),
                Map.of(
                    PAGE0, true
                ),
                List.of("Tag0")
            ),
            // A single result, the page is not viewable
            Arguments.of(
                List.<Object[]>of(
                    new Object[] { "xwiki:Space.Page0", "Tag0" }
                ),
                Map.of(
                    PAGE0, false
                ),
                List.of()
            ),
            // Two pages, Page0 is viewable, Page 1 is not
            Arguments.of(
                List.of(
                    new Object[] { "xwiki:Space.Page0", "Page0" },
                    new Object[] { "xwiki:Space.Page0", "All" },
                    new Object[] { "xwiki:Space.Page1", "Page1" },
                    new Object[] { "xwiki:Space.Page1", "all" }
                ),
                Map.of(
                    PAGE0, true,
                    PAGE1, false
                ),
                List.of("All", "Page0")
            ),
            // Two pages, both are viewable
            Arguments.of(
                List.of(
                    new Object[] { "xwiki:Space.Page0", "Page0" },
                    new Object[] { "xwiki:Space.Page0", "All" },
                    new Object[] { "xwiki:Space.Page1", "Page1" },
                    new Object[] { "xwiki:Space.Page1", "all" },
                    new Object[] { "xwiki:Space.Page1", "All" }
                ),
                Map.of(
                    PAGE0, true,
                    PAGE1, true
                ),
                List.of("All", "all", "Page0", "Page1")
            )
        );
    }

    @ParameterizedTest
    @MethodSource("getAllTagsSource")
    void getAllTags(List<Object[]> values, Map<DocumentReference, Boolean> viewRightsMap, List<String> expectedTags)
        throws Exception
    {
        viewRightsMap
            .forEach((key, value) -> when(this.contextualAuthorizationManager.hasAccess(VIEW, key)).thenReturn(value));

        when(this.query.<Object[]>execute()).thenReturn(values);
        assertEquals(expectedTags, this.tagsSelector.getAllTags());
    }

    /**
     * The stream of arguments contains three {@link Arguments}
     * <ol>
     *     <li>The first one is a {@link List} of {@link Object} arrays. Index 0 is a document reference, and Index 1
     *     is a tag. This corresponds to the values returned by the database. The order is important and the entries
     *     must be sorted by document reference
     *     <li>The second one is a {@link Map} of {@link DocumentReference} and {@link Boolean}. The document reference
     *     corresponds to the serialized forms from the first argument (two document references are available
     *     {@code xwiki:Space.Page0}, and {@code xwiki:Space.Page1})
     *     <li>The third one is a {@link Map} of {@link String} and {@link Integer} of the expected tags and their
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
                Map.of(),
                Map.of()
            ),
            // A single result, the page is viewable
            Arguments.of(
                List.<Object[]>of(
                    new Object[] { "xwiki:Space.Page0", "Tag0" }
                ),
                Map.of(
                    PAGE0, true
                ),
                Map.of("Tag0", 1)
            ),
            // A single result, the page is not viewable
            Arguments.of(
                List.<Object[]>of(
                    new Object[] { "xwiki:Space.Page0", "Tag0" }
                ),
                Map.of(
                    PAGE0, false
                ),
                Map.of()
            ),
            // Two pages, Page0 is viewable, Page 1 is not
            Arguments.of(
                List.of(
                    new Object[] { "xwiki:Space.Page0", "Page0" },
                    new Object[] { "xwiki:Space.Page0", "All" },
                    new Object[] { "xwiki:Space.Page1", "Page1" },
                    new Object[] { "xwiki:Space.Page1", "all" }
                ),
                Map.of(
                    PAGE0, true,
                    PAGE1, false
                ),
                Map.of(
                    "All", 1,
                    "Page0", 1
                )
            ),
            // Two pages, both are viewable
            Arguments.of(
                List.of(
                    new Object[] { "xwiki:Space.Page0", "Page0" },
                    new Object[] { "xwiki:Space.Page0", "All" },
                    new Object[] { "xwiki:Space.Page1", "Page1" },
                    new Object[] { "xwiki:Space.Page1", "All" }
                ),
                Map.of(
                    PAGE0, true,
                    PAGE1, true
                ),
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
    void getTagCountForQuery(List<Object[]> values, Map<DocumentReference, Boolean> viewRightsMap,
        Map<String, Integer> expectedTags) throws Exception
    {
        viewRightsMap
            .forEach((key, value) -> when(this.contextualAuthorizationManager.hasAccess(VIEW, key)).thenReturn(value));

        when(this.query.<Object[]>execute()).thenReturn(values);
        assertEquals(expectedTags, this.tagsSelector.getTagCountForQuery(null, null, (List<Object>) null));
    }

    /**
     * The stream of arguments contains three {@link Arguments}
     * <ol>
     *     <li>The first one is a {@link Map} of {@link DocumentReference} and {@link Boolean}. The document reference
     *     corresponds to the serialized forms from the first argument (two document references are available
     *     {@code xwiki:Space.Page0}, and {@code xwiki:Space.Page1})
     *     <li>The second one is a {@link List} of {@link String}. The element are document references of document with
     *     the given tag
     *     <li>The third one is a {@link List} of {@link String} of the expected returned document references
     * </ol>
     *
     * @return a {@link Stream} of arguments.
     */
    public static Stream<Arguments> getDocumentsWithTagSource()
    {
        return Stream.of(
            Arguments.of(
                Map.of(),
                List.of(),
                List.of()
            ),
            Arguments.of(
                Map.of(
                    PAGE0, true,
                    PAGE1, false
                ),
                List.of("xwiki:Space.Page0", "xwiki:Space.Page1"),
                List.of("xwiki:Space.Page0")
            )
        );
    }

    @ParameterizedTest
    @MethodSource("getDocumentsWithTagSource")
    void getDocumentsWithTag(Map<DocumentReference, Boolean> viewRightsMap, List<String> results, List<String> expected)
        throws Exception
    {
        viewRightsMap
            .forEach((key, value) -> when(this.contextualAuthorizationManager.hasAccess(VIEW, key)).thenReturn(value));
        when(this.query.<String>execute()).thenReturn(results);

        assertEquals(expected, this.tagsSelector.getDocumentsWithTag("Tag0", false, false));
    }

    private void initializeDocumentReferenceResolver()
    {
        when(this.stringDocumentReferenceResolver.resolve("xwiki:Space.Page0"))
            .thenReturn(PAGE0);
        when(this.stringDocumentReferenceResolver.resolve("xwiki:Space.Page1"))
            .thenReturn(PAGE1);
    }
}
