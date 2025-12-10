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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.Strings;
import org.xwiki.component.annotation.Component;

/**
 * Enables text filtering on the selected columns. Don't forget to bind the value of the {@code text} query parameter.
 * <p>
 * Columns aliased with an alias starting with {@code unfilterable} won't be considered for text filtering. String
 * and clob columns should be aliased with an alias starting with {@code string} to avoid casting them to string which
 * can lead to issues on some databases.
 * </p>
 *
 * @version $Id$
 * @since 9.8RC1
 */
@Component
@Named("text")
@Singleton
public class TextQueryFilter extends AbstractWhereQueryFilter
{
    private static final Pattern SELECT = Pattern.compile("^\\s*select\\s+", Pattern.CASE_INSENSITIVE);

    private static final Pattern DISTINCT = Pattern.compile("^distinct\\s+", Pattern.CASE_INSENSITIVE);

    private static final Pattern AS = Pattern.compile("\\s+as\\s+", Pattern.CASE_INSENSITIVE);

    private static final Pattern FROM = Pattern.compile("\\s+from\\s+", Pattern.CASE_INSENSITIVE);

    private static final Pattern COLUMN_SEPARATOR = Pattern.compile("\\s*,\\s*");

    private record ColumnAlias(String column, String alias)
    {
    }

    @Override
    public String filterStatement(String statement, String language)
    {
        String constraint = getFilterableColumnsAndAliases(statement).stream()
            .map(column -> {
                if (Strings.CS.startsWith(column.alias(), "string")) {
                    // Don't cast string columns to string as this will result in problems on some databases that
                    // don't support casting clob values to strings - this cast is only needed for non-string columns.
                    return "lower(" + column.column() + ") like lower(:text)";
                }
                return "lower(str(" + column.column() + ")) like lower(:text)";
            }).collect(Collectors.joining(" or "));
        if (constraint.isEmpty()) {
            return statement;
        } else {
            return insertWhereClause('(' + constraint + ')', statement, language);
        }
    }

    @Override
    public List filterResults(List results)
    {
        // We don't filter the results.
        return results;
    }

    @Override
    protected boolean isFilterable(String statement)
    {
        return true;
    }

    private List<ColumnAlias> getFilterableColumnsAndAliases(String statement)
    {
        Matcher selectMatcher = SELECT.matcher(statement);
        Matcher fromMatcher = FROM.matcher(statement);

        if (!selectMatcher.lookingAt() || !fromMatcher.find()) {
            return Collections.emptyList();
        }

        List<ColumnAlias> columns = new ArrayList<>();
        String selectClause = statement.substring(selectMatcher.end(), fromMatcher.start());
        // Skip 'distinct' in the select clause.
        selectClause = DISTINCT.matcher(selectClause).replaceFirst("");
        for (String column : COLUMN_SEPARATOR.split(selectClause)) {
            // Remove the column alias.
            String columnWithoutAlias = column;
            Matcher asMatcher = AS.matcher(column);
            if (asMatcher.find()) {
                columnWithoutAlias = column.substring(0, asMatcher.start());
                String alias = column.substring(asMatcher.end());
                if (alias.startsWith("unfilterable")) {
                    continue;
                }
                columns.add(new ColumnAlias(columnWithoutAlias, alias));
            } else {
                columns.add(new ColumnAlias(columnWithoutAlias, null));
            }
        }
        return columns;
    }
}
