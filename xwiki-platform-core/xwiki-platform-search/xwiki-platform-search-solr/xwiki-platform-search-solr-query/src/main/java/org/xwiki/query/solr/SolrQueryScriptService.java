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
package org.xwiki.query.solr;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.query.solr.internal.SafeSolrQueryValidator;
import org.xwiki.script.service.ScriptService;
import org.xwiki.search.solr.SolrUtils;
import org.xwiki.stability.Unstable;

/**
 * Helpers to safely insert values in Solr queries from scripts, available as {@code $services.query.solr}.
 * <p>
 * Values that aren't fixed in the script (e.g., values from the request, document or space names) must never be
 * inserted as they are in a Solr query statement or in a Solr parameter that is parsed as a query (e.g., {@code fq}),
 * as they could change the query: match other fields, use other query parsers, nested queries or functions. For
 * example:
 *
 * <pre>
 * {@code
 * #set ($query = $services.query.createQuery("title:$services.query.solr.escape($text)*", 'solr'))
 * #set ($discard = $query.bindValue('fq', "space_exact:$services.query.solr.quote($spaceName)"))
 * }
 * </pre>
 *
 * Request data must also never choose the names of the Solr parameters. Field names that come from the request (e.g.,
 * to filter or to sort the results) must be checked with {@link #isFieldName(String)}.
 *
 * @version $Id$
 * @since 18.10.0RC1
 * @since 18.4.9
 * @since 17.10.16
 * @since 16.10.20
 */
@Component
@Named("query.solr")
@Singleton
@Unstable
public class SolrQueryScriptService implements ScriptService
{
    @Inject
    private SolrUtils solrUtils;

    @Inject
    private SafeSolrQueryValidator validator;

    /**
     * Escapes the given value so that it is matched as a single term: all the characters that have a special meaning
     * in the Solr query syntax (including white spaces and the {@code AND}, {@code OR} and {@code NOT} operators) are
     * escaped, and an empty value is serialized as {@code ""}. Wildcards ({@code *}, {@code ?}) and fuzzy search
     * ({@code ~}) can be appended to the result.
     *
     * @param value the value to escape (dates are serialized in the format of the date fields)
     * @return the escaped value, or {@code null} if the given value is {@code null}
     */
    public String escape(Object value)
    {
        return this.solrUtils.toCompleteFilterQueryString(value);
    }

    /**
     * Quotes the given value so that it is matched as a phrase: the value is enclosed in double quotes, and all the
     * characters it contains that have a special meaning in the Solr query syntax are escaped. On a string field (e.g.,
     * {@code space_exact}), this matches the exact value. On a text field (e.g., {@code title}), this matches the words
     * of the value in the same order.
     *
     * @param value the value to quote (dates are serialized in the format of the date fields)
     * @return the quoted value, or {@code null} if the given value is {@code null}
     * @see SolrUtils#toPhraseQueryString(Object)
     */
    public String quote(Object value)
    {
        return this.solrUtils.toPhraseQueryString(value);
    }

    /**
     * Checks that the given name is a plain field name that Solr doesn't interpret in any way in queries, field lists
     * and sort specifications, e.g., as a function, a document transformer or a parameter reference. All the field
     * names produced by XWiki for the indexed documents and their properties are plain field names.
     *
     * @param name the name to check
     * @return {@code true} if the name is a plain field name, {@code false} otherwise
     */
    public boolean isFieldName(String name)
    {
        return this.validator.isSafeSortField(name);
    }
}
