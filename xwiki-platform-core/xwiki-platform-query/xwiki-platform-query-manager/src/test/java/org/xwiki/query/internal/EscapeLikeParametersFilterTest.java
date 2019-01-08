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

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.xwiki.query.Query;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParser;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link EscapeLikeParametersFilter}.
 *
 * @version $Id$
 * @since 8.4.5
 * @since 9.3RC1
 */
@ComponentTest
public class EscapeLikeParametersFilterTest
{
    @InjectMockComponents
    private EscapeLikeParametersFilter filter;

    @Test
    public void filterWithNamedParameterAndNoEscape()
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
    public void filterWithNamedParameterAndLikeAtEnd()
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
    public void filterWithNamedParameterAndEscape()
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
    public void filterWithPositionalParameterAndNoEscape()
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
    public void filterWithNumberedPositionalParameterAndNoEscape()
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
    public void filterWithPositionalParameterAndFunction()
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

    @Test
    public void namedParametersLikeAPICustomEscape()
    {
        String statement = "select doc.fullName from XWikiDocument doc where "
            + "doc.fullName like :fullName escape '/' and doc.author like :author escape '!'";
        Query query = mock(Query.class);
        when(query.getLanguage()).thenReturn(Query.HQL);
        when(query.getStatement()).thenReturn(statement);

        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("fullName", new DefaultQueryParameter(null).literal("Foo//Bar"));
        parameters.put("author", new DefaultQueryParameter(null).like("Bar!").anychar());

        when(query.getNamedParameters()).thenReturn(parameters);

        Query filteredQuery = this.filter.filterQuery(query);
        String modifiedStatement = "SELECT doc.fullName FROM XWikiDocument doc WHERE "
            + "doc.fullName LIKE :fullName ESCAPE '/' AND doc.author LIKE :author ESCAPE '!'";
        assertEquals(modifiedStatement, filteredQuery.getStatement());

        assertTrue(filteredQuery.getPositionalParameters().isEmpty());

        Map<String, Object> expectedMap = new LinkedHashMap<>();
        expectedMap.put("author", "Bar!!_");

        // Limitation, only standard '!' is currently escaped automatically
        expectedMap.put("fullName", "Foo//Bar");

        assertEquals(expectedMap, filteredQuery.getNamedParameters());
    }

    @Test
    public void positionalParametersLikeAPICustomEscape()
    {
        String statement = "select doc.fullName from XWikiDocument doc where "
            + "doc.fullName like ? and doc.author like ? escape '!'";
        Query query = mock(Query.class);
        when(query.getLanguage()).thenReturn(Query.HQL);
        when(query.getStatement()).thenReturn(statement);

        Map<Integer, Object> parameters = new LinkedHashMap<>();
        parameters.put(0, new DefaultQueryParameter(null).literal("Foo!"));
        parameters.put(1, new DefaultQueryParameter(null).like("Bar!").anychar());

        when(query.getPositionalParameters()).thenReturn(parameters);

        Query filteredQuery = this.filter.filterQuery(query);
        String modifiedStatement = "SELECT doc.fullName FROM XWikiDocument doc WHERE "
            + "doc.fullName LIKE ? ESCAPE '!' AND doc.author LIKE ? ESCAPE '!'";
        assertEquals(modifiedStatement, filteredQuery.getStatement());

        assertTrue(filteredQuery.getNamedParameters().isEmpty());

        Map<Integer, Object> expectedMap = new LinkedHashMap<>();
        expectedMap.put(0, "Foo!!");
        expectedMap.put(1, "Bar!!_");

        assertEquals(expectedMap, filteredQuery.getPositionalParameters());
    }

    @Test
    public void positionalAndNamedParameters()
    {
        String statement = "select doc.fullName from XWikiDocument doc where "
            + "doc.fullName like :fullName escape '/' and "
            + "doc.author like ? or "
            + "doc.version = :version and "
            + "doc.space like ? escape '!'";

        Query query = mock(Query.class);
        when(query.getLanguage()).thenReturn(Query.HQL);
        when(query.getStatement()).thenReturn(statement);

        Map<String, Object> namedParameters = new LinkedHashMap<>();
        namedParameters.put("fullName", new DefaultQueryParameter(null).literal("Space//Content").anyChars());
        namedParameters.put("version", "1.1");
        when(query.getNamedParameters()).thenReturn(namedParameters);

        Map<Integer, Object> positionalParameters = new LinkedHashMap<>();
        positionalParameters.put(0, new DefaultQueryParameter(null).like("Bar!"));
        positionalParameters.put(1, new DefaultQueryParameter(null).like("%Foo!%"));
        when(query.getPositionalParameters()).thenReturn(positionalParameters);

        Query filteredQuery = this.filter.filterQuery(query);
        String modifiedStatement = "SELECT doc.fullName FROM XWikiDocument doc WHERE "
            + "doc.fullName LIKE :fullName ESCAPE '/' AND "
            + "doc.author LIKE ? ESCAPE '!' OR "
            + "doc.version = :version AND "
            + "doc.space LIKE ? ESCAPE '!'";

        assertEquals(modifiedStatement, filteredQuery.getStatement());

        Map<String, Object> expectedNamedMap = new LinkedHashMap<>();
        expectedNamedMap.put("fullName", "Space//Content%");
        expectedNamedMap.put("version", "1.1");

        assertEquals(expectedNamedMap, filteredQuery.getNamedParameters());

        Map<Integer, Object> expectedPositionalMap = new LinkedHashMap<>();
        expectedPositionalMap.put(0, "Bar!!");
        expectedPositionalMap.put(1, "%Foo!!%");

        assertEquals(expectedPositionalMap, filteredQuery.getPositionalParameters());
    }

    @Test
    public void notLike()
    {
        String statement = "select doc.fullName from XWikiDocument doc where doc.fullName not like ?";
        Query query = mock(Query.class);
        when(query.getLanguage()).thenReturn(Query.HQL);
        when(query.getStatement()).thenReturn(statement);
        Map<Integer, Object> positionalParameters = new HashMap<>();
        positionalParameters.put(0, new DefaultQueryParameter(null).like("Foo.%"));

        when(query.getPositionalParameters()).thenReturn(positionalParameters);

        Query query1 = this.filter.filterQuery(query);
        String expectedStatement = "select doc.fullName from XWikiDocument doc where doc.fullName not like ? escape '!'";
        assertEquals(expectedStatement.toLowerCase(), query1.getStatement().toLowerCase());

        statement = "select doc.fullName from XWikiDocument doc where (doc.fullName not like ?)";
        query = mock(Query.class);
        when(query.getLanguage()).thenReturn(Query.HQL);
        when(query.getStatement()).thenReturn(statement);
        positionalParameters = new HashMap<>();
        positionalParameters.put(0, new DefaultQueryParameter(null).like("Foo.%"));

        when(query.getPositionalParameters()).thenReturn(positionalParameters);

        query1 = this.filter.filterQuery(query);
        expectedStatement = "select doc.fullName from XWikiDocument doc where (doc.fullName not like ? escape '!')";
        assertEquals(expectedStatement.toLowerCase(), query1.getStatement().toLowerCase());

        statement = "select doc.fullName from XWikiDocument doc where not (doc.fullName not like ? and "
            + "doc.fullName like ?)";
        query = mock(Query.class);
        when(query.getLanguage()).thenReturn(Query.HQL);
        when(query.getStatement()).thenReturn(statement);
        positionalParameters = new HashMap<>();
        positionalParameters.put(0, new DefaultQueryParameter(null).like("Foo.%"));

        when(query.getPositionalParameters()).thenReturn(positionalParameters);

        query1 = this.filter.filterQuery(query);
        expectedStatement = "select doc.fullName from XWikiDocument doc where not (doc.fullName not like ? escape '!' "
            + "and doc.fullName like ? escape '!')";
        assertEquals(expectedStatement.toLowerCase(), query1.getStatement().toLowerCase());

        statement = "select doc.fullName from XWikiDocument doc where ((doc.fullName not like ? and doc.fullName "
            + "not like ?) or doc.fullName not like ?) limit 5";
        query = mock(Query.class);
        when(query.getLanguage()).thenReturn(Query.HQL);
        when(query.getStatement()).thenReturn(statement);
        positionalParameters = new HashMap<>();
        positionalParameters.put(0, new DefaultQueryParameter(null).like("Foo.%"));
        positionalParameters.put(1, new DefaultQueryParameter(null).like("B.%"));
        positionalParameters.put(2, new DefaultQueryParameter(null).like("H.%"));

        when(query.getPositionalParameters()).thenReturn(positionalParameters);

        query1 = this.filter.filterQuery(query);
        expectedStatement = "select doc.fullName from XWikiDocument doc where ((doc.fullName not like ? escape '!' "
            + "and doc.fullName not like ? escape '!') or doc.fullName not like ? escape '!') limit 5";
        assertEquals(expectedStatement.toLowerCase(), query1.getStatement().toLowerCase());
    }
}
