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
package org.xwiki.search.solr.internal;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.apache.solr.common.params.MapSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Unit tests for {@link XWikiDismaxQParserPlugin}.
 * 
 * @version $Id$
 * @since 5.3RC1
 */
class XWikiDismaxQParserPluginTest
{
    /**
     * The object being tested.
     */
    private XWikiDismaxQParserPlugin plugin = new XWikiDismaxQParserPlugin();

    @Test
    void extractFieldNames()
    {
        assertEquals(Collections.emptySet(), plugin.extractFieldNames(""));
        assertEquals(Collections.emptySet(), plugin.extractFieldNames("text"));
        assertEquals(Collections.emptySet(), plugin.extractFieldNames("+(^:text"));
        assertEquals(Collections.emptySet(), plugin.extractFieldNames("foo :bar"));

        assertEquals(Collections.singleton("foo"), plugin.extractFieldNames("foo:bar"));
        assertEquals(Collections.singleton("both"), plugin.extractFieldNames("both:(one two)"));
        assertEquals(Collections.singleton("title__"), plugin.extractFieldNames("title__:text"));
        assertEquals(Collections.singleton("title_zh_TW"), plugin.extractFieldNames("title_zh_TW:text"));
        assertEquals(Collections.singleton("property.Blog.BlogPostClass.title"),
            plugin.extractFieldNames("property.Blog.BlogPostClass.title:value"));
        assertEquals(Collections.singleton("property.Blog.Blog..Post$5EClass.title"),
            plugin.extractFieldNames("property.Blog.Blog..Post$5EClass.title:value"));

        assertEquals(
            new HashSet<>(Arrays.asList("abc", "g_h.i", "m$n-o",
                "_\u0103\u00EE\u00E2\u0219\u021B\u00E8\u00E9\u00EA\u00EB")),
            plugin.extractFieldNames("+abc:def AND -g_h.i:jkl AND (m$n-o:pqr OR "
                + "_\u0103\u00EE\u00E2\u0219\u021B\u00E8\u00E9\u00EA\u00EB:stu^3)"));
    }

    @Test
    void withFieldAliases()
    {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("qf", "title^0.4 comment^0.40 date^1.0");
        parameters.put("xwiki.multilingualFields", "title, property.*, foo, comment");
        parameters.put("xwiki.supportedLocales", "en, fr, zh_TW");
        parameters.put("xwiki.typedDynamicFields", "property.*");
        parameters.put("xwiki.dynamicFieldTypes", "boolean, int");

        String query = "title:text AND x:y AND property.Blog.BlogPostClass.summary:wiki AND title_ro:value";
        SolrParams paramsWithAliases = plugin.withFieldAliases(query, new MapSolrParams(parameters));

        assertEquals("title__ title_en title_fr title_zh_TW", paramsWithAliases.get("f.title.qf"));
        assertEquals("property.Blog.BlogPostClass.summary__ property.Blog.BlogPostClass.summary_en "
            + "property.Blog.BlogPostClass.summary_fr property.Blog.BlogPostClass.summary_zh_TW "
            + "property.Blog.BlogPostClass.summary_boolean property.Blog.BlogPostClass.summary_int",
            paramsWithAliases.get("f.property.Blog.BlogPostClass.summary.qf"));

        // Event if this field doesn't appear in the query, it's a default field so it has to have the alias.
        assertEquals("comment__ comment_en comment_fr comment_zh_TW", paramsWithAliases.get("f.comment.qf"));

        // These fields are not declared as multilingual.
        assertNull(paramsWithAliases.get("f.x.qf"));
        assertNull(paramsWithAliases.get("f.title_ro.qf"));

        // This is a default field but it's not declared as multilingual.
        assertNull(paramsWithAliases.get("f.date.qf"));

        // This multilingual field doesn't appear in the query and it's not a default field either.
        assertNull(paramsWithAliases.get("f.foo.qf"));
    }

    @Test
    void withFieldAliasesWhenNoSupportedLocales()
    {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("qf", "comment^0.40");
        parameters.put("xwiki.multilingualFields", "title, comment");

        SolrParams paramsWithAliases = plugin.withFieldAliases("title:text", new MapSolrParams(parameters));

        // Aliases for the ROOT locale.
        assertEquals("title__", paramsWithAliases.get("f.title.qf"));
        assertEquals("comment__", paramsWithAliases.get("f.comment.qf"));
    }

    @Test
    void withFieldAliasesWhenNoMultilingualFields()
    {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("qf", "title^0.4 comment^0.40 date^1.0");
        parameters.put("xwiki.supportedLocales", "en, ro");

        SolrParams paramsWithAliases = plugin.withFieldAliases("title:text", new MapSolrParams(parameters));

        // The existing parameters should have been preserved.
        assertEquals(2, paramsWithAliases.toNamedList().size());
        assertEquals("title^0.4 comment^0.40 date^1.0", paramsWithAliases.get("qf"));
        assertEquals("en, ro", paramsWithAliases.get("xwiki.supportedLocales"));
    }

    @Test
    void withFieldAliasesWhenNoFieldsInQuery()
    {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("qf", "title^0.4 comment^0.40");
        parameters.put("xwiki.multilingualFields", "title, foo");
        parameters.put("xwiki.supportedLocales", "en, fr");
        parameters.put("xwiki.typedDynamicFields", "property.*");
        parameters.put("xwiki.dynamicFieldTypes", "long, date");

        SolrParams paramsWithAliases = plugin.withFieldAliases("text", new MapSolrParams(parameters));

        // 5 existing parameters plus one alias.
        assertEquals(6, paramsWithAliases.toNamedList().size());

        // A default multilingual field.
        assertEquals("title__ title_en title_fr", paramsWithAliases.get("f.title.qf"));

        // Not a multilingual field.
        assertNull(paramsWithAliases.get("f.comment.qf"));

        // Not a default field.
        assertNull(paramsWithAliases.get("f.foo.qf"));
    }
}
