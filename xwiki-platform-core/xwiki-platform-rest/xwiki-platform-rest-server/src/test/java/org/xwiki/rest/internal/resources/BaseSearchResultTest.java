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

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.xwiki.rest.internal.resources.KeywordSearchScope.CONTENT;
import static org.xwiki.rest.internal.resources.KeywordSearchScope.OBJECTS;
import static org.xwiki.rest.internal.resources.KeywordSearchScope.TITLE;

/**
 * Unit tests for {@link BaseSearchResult}.
 *
 * @version $Id$
 */
class BaseSearchResultTest
{
    /** Exposes the protected method under test. */
    private static final class TestableSearchResult extends BaseSearchResult
    {
        List<KeywordSearchScope> parse(List<String> scopes)
        {
            return parseSearchScopeStrings(scopes);
        }
    }

    private final TestableSearchResult searchResult = new TestableSearchResult();

    @Test
    void parsesDistinctScopes()
    {
        assertEquals(List.of(TITLE, OBJECTS), searchResult.parse(List.of("title", "objects")));
    }

    @Test
    void deduplicatesRepeatedScopes()
    {
        // Regression: the old code compared a String against a List<KeywordSearchScope> and never
        // de-duplicated, so this returned [CONTENT, CONTENT].
        assertEquals(List.of(CONTENT), searchResult.parse(List.of("content", "content")));
    }

    @Test
    void isCaseInsensitive()
    {
        assertEquals(List.of(TITLE), searchResult.parse(List.of("TiTlE")));
    }

    @Test
    void ignoresNullAndUnknownScopesAndDefaultsToContent()
    {
        assertEquals(List.of(CONTENT), searchResult.parse(Arrays.asList(null, "bogus")));
    }

    @Test
    void emptyInputDefaultsToContent()
    {
        assertEquals(List.of(CONTENT), searchResult.parse(List.of()));
    }
}
