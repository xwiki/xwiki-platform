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
package org.xwiki.query.internal;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.query.Query;
import org.xwiki.query.QueryFilter;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link EscapeLikeParametersFilter}.
 *
 * @version $Id$
 * @since 8.4.5
 * @since 9.3RC1
 */
public class EscapeLikeParametersFilterTest
{
    @Rule
    public MockitoComponentMockingRule<EscapeLikeParametersFilter> mocker =
        new MockitoComponentMockingRule<>(EscapeLikeParametersFilter.class);

    private QueryFilter filter;

    @Before
    public void setUp() throws Exception
    {
        this.filter = this.mocker.getComponentUnderTest();
    }

    @Test
    public void filterWithNamedParameterAndNoEscape() throws Exception
    {
        Query query = mock(Query.class);
        when(query.getLanguage()).thenReturn(Query.HQL);
        when(query.getStatement()).thenReturn("select a from b where ref like :reference and doc.space=:space");

        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("reference",
            new DefaultQueryParameter(query).literal("wiki:space1.space\\.2.space!3.").anyChars());
        parameters.put("space", "wiki:space1.space\\.2.space!3.WebHome");
        when(query.getNamedParameters()).thenReturn(parameters);

        Query filteredQuery = this.filter.filterQuery(query);
        assertEquals("SELECT a FROM b WHERE ref LIKE :reference ESCAPE '!' AND doc.space = :space",
            filteredQuery.getStatement());
        assertEquals("wiki:space1.space\\.2.space!!3.%", filteredQuery.getNamedParameters().get("reference"));
        assertEquals("wiki:space1.space\\.2.space!3.WebHome", filteredQuery.getNamedParameters().get("space"));
    }

    @Test
    public void filterWithNamedParameterAndLikeAtEnd() throws Exception
    {
        Query query = mock(Query.class);
        when(query.getLanguage()).thenReturn(Query.HQL);
        when(query.getStatement()).thenReturn("select a from b where ref like :reference");

        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("reference", new DefaultQueryParameter(query).literal("wiki:space1.space\\.2.space!3.WebHome"));
        when(query.getNamedParameters()).thenReturn(parameters);

        Query filteredQuery = this.filter.filterQuery(query);
        assertEquals("SELECT a FROM b WHERE ref LIKE :reference ESCAPE '!'", filteredQuery.getStatement());
        assertEquals("wiki:space1.space\\.2.space!!3.WebHome", filteredQuery.getNamedParameters().get("reference"));
    }

    @Test
    public void filterWithNamedParameterAndEscape() throws Exception
    {
        Query query = mock(Query.class);
        when(query.getLanguage()).thenReturn(Query.HQL);
        when(query.getStatement()).thenReturn("select a from b where ref like :reference escape '|'");

        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("reference", "wiki:space1.space\\.2.space!3.WebHome");
        when(query.getNamedParameters()).thenReturn(parameters);

        Query filteredQuery = this.filter.filterQuery(query);
        assertEquals("SELECT a FROM b WHERE ref LIKE :reference ESCAPE '|'", filteredQuery.getStatement());
        assertEquals("wiki:space1.space\\.2.space!3.WebHome", filteredQuery.getNamedParameters().get("reference"));
    }

    @Test
    public void filterWithPositionalParameterAndNoEscape() throws Exception
    {
        Query query = mock(Query.class);
        when(query.getLanguage()).thenReturn(Query.HQL);
        when(query.getStatement()).thenReturn("select a from b where ref like ? and doc.space=:space");

        Map<Integer, Object> positionalParameters = new LinkedHashMap<>();
        positionalParameters.put(0,
            new DefaultQueryParameter(query).literal("wiki:space1.space\\.2.space!3.WebHome"));
        when(query.getPositionalParameters()).thenReturn(positionalParameters);

        Map<String, Object> namedParameters = new LinkedHashMap<>();
        namedParameters.put("space", "wiki:space1.space\\.2.space!3.WebHome");
        when(query.getNamedParameters()).thenReturn(namedParameters);

        Query filteredQuery = this.filter.filterQuery(query);
        assertEquals("SELECT a FROM b WHERE ref LIKE ? ESCAPE '!' AND doc.space = :space",
            filteredQuery.getStatement());
        assertEquals("wiki:space1.space\\.2.space!!3.WebHome", filteredQuery.getPositionalParameters().get(0));
        assertEquals("wiki:space1.space\\.2.space!3.WebHome", filteredQuery.getNamedParameters().get("space"));
    }

    @Test
    public void filterWithNumberedPositionalParameterAndNoEscape() throws Exception
    {
        Query query = mock(Query.class);
        when(query.getLanguage()).thenReturn(Query.HQL);
        when(query.getStatement()).thenReturn("select a from b where doc.space=?2 and ref like ?1");

        Map<Integer, Object> parameters = new LinkedHashMap<>();
        parameters.put(0,
            new DefaultQueryParameter(query).literal("wiki:space1.space\\.2.space!3.WebHome"));
        parameters.put(1, "wiki:space1.space\\.2.space!3.WebHome");
        when(query.getPositionalParameters()).thenReturn(parameters);

        Query filteredQuery = this.filter.filterQuery(query);
        assertEquals("SELECT a FROM b WHERE doc.space = ?2 AND ref LIKE ?1 ESCAPE '!'", filteredQuery.getStatement());
        assertEquals("wiki:space1.space\\.2.space!!3.WebHome", filteredQuery.getPositionalParameters().get(0));
        assertEquals("wiki:space1.space\\.2.space!3.WebHome", filteredQuery.getPositionalParameters().get(1));
    }

    @Test
    public void filterWithPositionalParameterAndFunction() throws Exception
    {
        Query query = mock(Query.class);
        when(query.getLanguage()).thenReturn(Query.HQL);
        when(query.getStatement()).thenReturn("select a from b where ref like LOWER(?)");

        Map<Integer, Object> positionalParameters = new LinkedHashMap<>();
        positionalParameters.put(0,
            new DefaultQueryParameter(query).literal("wiki:space1.space\\.2.space!3.WebHome"));
        when(query.getPositionalParameters()).thenReturn(positionalParameters);

        Query filteredQuery = this.filter.filterQuery(query);
        assertEquals("SELECT a FROM b WHERE ref LIKE LOWER(?) ESCAPE '!'", filteredQuery.getStatement());
        assertEquals("wiki:space1.space\\.2.space!!3.WebHome", filteredQuery.getPositionalParameters().get(0));
    }

    @Test
    public void filterWhenUsingBangInLike()
    {
        Query query = mock(Query.class);
        when(query.getLanguage()).thenReturn(Query.HQL);
        when(query.getStatement()).thenReturn("select a from b where ref like :likeValue");

        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("likeValue", new DefaultQueryParameter(query).like("%some!thing%"));
        when(query.getNamedParameters()).thenReturn(parameters);

        Query filteredQuery = this.filter.filterQuery(query);
        assertEquals("SELECT a FROM b WHERE ref LIKE :likeValue ESCAPE '!'", filteredQuery.getStatement());
        // The test is here, we verify that the bang character has been escaped from the like value since it's an
        // escape character
        assertEquals("%some!!thing%", filteredQuery.getNamedParameters().get("likeValue"));

    }
}
