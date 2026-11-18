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
package org.xwiki.query.solr.internal;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link SafeSolrQueryValidator}.
 *
 * @version $Id$
 */
@ComponentTest
class SafeSolrQueryValidatorTest
{
    private static final Set<String> FIELDS = Set.of("id", "wiki", "spaces", "name");

    @InjectMockComponents
    private SafeSolrQueryValidator validator;

    @ParameterizedTest
    @ValueSource(strings = {
        "type:DOCUMENT",
        "reference:document\\:xwiki\\:Main.WebHome",
        "title:a\\:b\\u0063 \\\\",
        "mimetype:((image/*) OR (text/*))",
        "locale:(\"en\" OR \"\")",
        "+type:DOCUMENT -hidden:true",
        "date:[NOW-1DAY TO NOW]",
        "property.My$20Space.Été.prop_string:value"
    })
    void safeFilterQuery(String filterQuery)
    {
        assertTrue(this.validator.isSafeFilterQuery(filterQuery));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "{!join from=space to=space}type:DOCUMENT",
        "{!tag=type}type:DOCUMENT",
        "{!lucene}type:DOCUMENT",
        "{!term f=space}Main",
        "type:DOCUMENT AND {!term f=space}Main",
        " {!term f=space}Main",
        "space:\\{\\!term",
        "space:\\u007b!term",
        "type:DOCUMENT AND _query_:\"type:DOCUMENT\"",
        "_val_:\"1\"",
        "_v\\al_:\"1\"",
        "\\u005fval_:\"1\""
    })
    void unsafeFilterQuery(String filterQuery)
    {
        assertFalse(this.validator.isSafeFilterQuery(filterQuery));
    }

    @Test
    void nullFilterQuery()
    {
        assertTrue(this.validator.isSafeFilterQuery(null));
    }

    @ParameterizedTest
    @ValueSource(strings = { "name", "title_", "*", "*_string", "title_*", "doc-id", "score",
        "property.My$20Space.Été.prop_string" })
    void safeFieldListItem(String item)
    {
        assertTrue(this.validator.isSafeFieldListItem(item));
    }

    @ParameterizedTest
    @ValueSource(strings = { "", "[docid]", "alias:name", "sum(1,2)", "$qf", "1", "title_*-en" })
    void unsafeFieldListItem(String item)
    {
        assertFalse(this.validator.isSafeFieldListItem(item));
    }

    @Test
    void nullFieldListItem()
    {
        assertFalse(this.validator.isSafeFieldListItem(null));
    }

    @ParameterizedTest
    @ValueSource(strings = { "name", "title_sort", "score", "property.My$20Space.Été.prop_sortString",
        "property.My-App.Code.TagClass.tags_string", "doc-id" })
    void safeSortField(String field)
    {
        assertTrue(this.validator.isSafeSortField(field));
    }

    @ParameterizedTest
    @ValueSource(strings = { "", "sum(1,2)", "$qf", "1", "name asc", "_query_", "_val_", "-name" })
    void unsafeSortField(String field)
    {
        assertFalse(this.validator.isSafeSortField(field));
    }

    @Test
    void nullSortField()
    {
        assertFalse(this.validator.isSafeSortField(null));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "",
        "  ",
        "type,wiki,spaces,name,filename,score",
        "type wiki  spaces,name",
        "*,score",
        "title_*, property.My$20Space.Été.prop_string",
        "spaces:spaces,name:name",
        "title:title_,location:spaces",
        "$qf",
        "title:[value v=1],[explain],count:sum(1,2)",
        "x:[subquery],title_,score",
        "hidden,mimetype,attname,spaceName",
        "[explain],property.My$20Space.Été.prop_string,x:field(property.A$2EB.prop_sortString)"
    })
    void fieldListKeepingFields(String fieldList)
    {
        assertTrue(this.validator.isFieldListKeepingFields(fieldList, FIELDS));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "name:wiki",
        "id,wiki, spaces:title_",
        // An em space, which Solr considers as a white space.
        "x\u2003name:wiki",
        "name :wiki",
        "spaces:[value v=Main]",
        "name:[value v=Page]",
        "title_,{!func key=name}1",
        "{!func key='na\\u006De'}1",
        "{!func key=$k}1",
        "[child fl=$f]",
        "x:sum(1,2),name:title_",
        "[explain],x:$f",
        "[explain],x:sum($f,1)",
        "[value v=1],na${k}",
        "[value v=1],{!func key= $k}1"
    })
    void fieldListOverridingFields(String fieldList)
    {
        assertFalse(this.validator.isFieldListKeepingFields(fieldList, FIELDS));
    }

    @Test
    void nullFieldList()
    {
        assertTrue(this.validator.isFieldListKeepingFields(null, FIELDS));
    }
}
