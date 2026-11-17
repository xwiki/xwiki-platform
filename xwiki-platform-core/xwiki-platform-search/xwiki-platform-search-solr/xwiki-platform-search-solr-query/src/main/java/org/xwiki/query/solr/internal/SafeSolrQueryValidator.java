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

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;

/**
 * Validates Solr query syntax that comes from untrusted input, e.g., from the request, so that it can only match
 * documents based on their own field values: no query parser that evaluates nested queries or functions or that
 * matches documents based on other documents, and no parameter references.
 *
 * @version $Id$
 * @since 18.10.0RC1
 * @since 18.4.9
 * @since 17.10.16
 * @since 16.10.20
 */
@Component(roles = SafeSolrQueryValidator.class)
@Singleton
public class SafeSolrQueryValidator
{
    /**
     * The magic field that makes the standard query parser evaluate a nested query.
     */
    private static final String QUERY_FIELD = "_query_";

    /**
     * The magic field that makes the standard query parser evaluate a function.
     */
    private static final String FUNCTION_FIELD = "_val_";

    /**
     * The syntax that makes Solr use another query parser (local parameters) or evaluate a nested query or a function
     * (magic fields).
     */
    private static final List<String> UNSAFE_QUERY_SYNTAX = List.of("{!", QUERY_FIELD, FUNCTION_FIELD);

    /**
     * The field names that make the standard query parser evaluate a nested query or a function.
     */
    private static final Set<String> MAGIC_FIELDS = Set.of(QUERY_FIELD, FUNCTION_FIELD);

    /**
     * The separators of the items of a field list, when none of the items uses any special syntax. Solr accepts all
     * the characters for which {@link Character#isWhitespace(char)} is true, not only the ASCII white spaces.
     */
    private static final Pattern FIELD_LIST_SEPARATOR = Pattern.compile("[,\\p{javaWhitespace}]+");

    /**
     * A field list item without any special syntax: a field name, a field name pattern or, with a colon, a field name
     * preceded by the name under which it is returned. Solr returns the item either under its own name or, for a
     * function, under its own text.
     */
    private static final String PLAIN_FIELD_LIST_NAME = "[^\\p{javaWhitespace},:\\[\\](){}'\"\\\\]+";

    private static final Pattern PLAIN_FIELD_LIST_ITEM =
        Pattern.compile(String.format("(?:(%1$s):)?(%1$s)", PLAIN_FIELD_LIST_NAME));

    /**
     * The syntax that can make Solr compute a name in a field list: escape sequences, macros and parameter references.
     * Solr only dereferences a parameter when {@code $} starts a value, so a {@code $} that follows a character of a
     * name is accepted: it's how the names of the fields of the properties are encoded (e.g., {@code My$20Space}).
     */
    private static final Pattern FIELD_LIST_COMPUTED_NAME_SYNTAX =
        Pattern.compile("\\\\|\\$\\{|(?<![\\p{L}\\p{N}_.])\\$");

    /**
     * The characters that can be part of a name in a field list.
     */
    private static final String NAME_CHARACTER = "[\\p{L}\\p{N}_]";

    /**
     * The first character of a field name, like Solr accepts it for a plain field name. Field names starting with
     * {@code $} are parameter references.
     */
    private static final String FIELD_NAME_START = "[\\p{javaJavaIdentifierStart}&&[^$]]";

    /**
     * A field name (optionally followed by a dash and more characters) or a field name pattern, like Solr accepts them
     * in the field list without parsing them as a function or a document transformer.
     */
    private static final Pattern FIELD_LIST_ITEM = Pattern.compile(FIELD_NAME_START
        + "[\\p{javaJavaIdentifierPart}.-]*|[\\p{javaJavaIdentifierStart}*&&[^$]][\\p{javaJavaIdentifierPart}.*]*");

    /**
     * A field name like Solr accepts it in the sort order without parsing it as a function.
     */
    private static final Pattern SORT_FIELD = Pattern.compile(FIELD_NAME_START + "[\\p{javaJavaIdentifierPart}.-]*");

    /**
     * Checks a filter query ({@code fq}), which is parsed with the standard query parser. The filter query must not
     * use local parameters or magic fields, anywhere, even escaped.
     *
     * @param filterQuery the filter query to check
     * @return {@code true} if the filter query is safe, {@code false} otherwise
     */
    public boolean isSafeFilterQuery(String filterQuery)
    {
        // Solr removes the escaping from field names and values, so check the unescaped query.
        String unescapedQuery = unescape(StringUtils.defaultString(filterQuery));
        return UNSAFE_QUERY_SYNTAX.stream().noneMatch(unescapedQuery::contains);
    }

    /**
     * Checks an item of the field list ({@code fl}).
     *
     * @param item the field list item to check
     * @return {@code true} if the item is a field name or a field name pattern (using {@code *}), {@code false}
     *     otherwise (e.g., for functions, document transformers and aliases)
     */
    public boolean isSafeFieldListItem(String item)
    {
        return item != null && FIELD_LIST_ITEM.matcher(item).matches();
    }

    /**
     * Checks a field to sort the results on, without the sort direction.
     *
     * @param field the field to check
     * @return {@code true} if the field is a field name, {@code false} otherwise (e.g., for functions and parameter
     *     references)
     */
    public boolean isSafeSortField(String field)
    {
        return field != null && SORT_FIELD.matcher(field).matches() && !MAGIC_FIELDS.contains(field);
    }

    /**
     * Checks that a field list ({@code fl}) can't return other values under the names of the given fields than the
     * values of these fields, e.g., by renaming another field, using a document transformer or a function.
     * <p>
     * A field list using only field names, field name patterns and renames (e.g., {@code title:title_}) is checked
     * exactly. As soon as any other syntax is used, the field list must not contain
     * any of the given field names as a word, nor any parameter reference, macro or escape sequence that could produce
     * one.
     *
     * @param fieldList the field list to check
     * @param fields the names of the fields that must keep their values
     * @return {@code true} if the field list keeps the values of the given fields, {@code false} otherwise
     */
    public boolean isFieldListKeepingFields(String fieldList, Collection<String> fields)
    {
        String trimmedFieldList = StringUtils.trim(fieldList);
        if (StringUtils.isEmpty(trimmedFieldList)) {
            return true;
        }

        boolean plain = true;
        for (String item : FIELD_LIST_SEPARATOR.split(trimmedFieldList)) {
            Matcher matcher = PLAIN_FIELD_LIST_ITEM.matcher(item);
            if (!matcher.matches()) {
                plain = false;
                break;
            }
            String key = matcher.group(1);
            if (key != null && fields.contains(key) && !key.equals(matcher.group(2))) {
                return false;
            }
        }

        return plain || isComplexFieldListKeepingFields(trimmedFieldList, fields);
    }

    private boolean isComplexFieldListKeepingFields(String fieldList, Collection<String> fields)
    {
        return !FIELD_LIST_COMPUTED_NAME_SYNTAX.matcher(fieldList).find()
            && fields.stream().noneMatch(field -> Pattern
                .compile(String.format("(?<!%1$s)%2$s(?!%1$s)", NAME_CHARACTER, Pattern.quote(field)))
                .matcher(fieldList).find());
    }

    private String unescape(String query)
    {
        StringBuilder result = new StringBuilder(query.length());
        int length = query.length();
        int i = 0;
        while (i < length) {
            char character = query.charAt(i++);
            if (character == '\\' && i < length) {
                character = query.charAt(i++);
                // Unicode escape sequence: the letter u followed by 4 hexadecimal digits.
                if (character == 'u' && i + 4 <= length && isHex(query, i, i + 4)) {
                    character = (char) Integer.parseInt(query.substring(i, i + 4), 16);
                    i += 4;
                }
            }
            result.append(character);
        }
        return result.toString();
    }

    private boolean isHex(String value, int start, int end)
    {
        for (int i = start; i < end; i++) {
            if (Character.digit(value.charAt(i), 16) < 0) {
                return false;
            }
        }
        return true;
    }
}
