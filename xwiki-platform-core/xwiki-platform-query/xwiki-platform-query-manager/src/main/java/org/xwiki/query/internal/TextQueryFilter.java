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
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;

/**
 * Enables text filtering on the selected columns. Don't forget to bind the value of the {@code text} query parameter.
 * 
 * @version $Id$
 * @since 9.8RC1
 */
@Component
@Named("text")
@Singleton
public class TextQueryFilter extends AbstractWhereQueryFilter
{
    private static final Pattern COLUMN_SEPARATOR = Pattern.compile("\\s*,\\s*");

    @Override
    public String filterStatement(String statement, String language)
    {
        String constraint = getFilterableColumns(statement).stream()
            .map(column -> "lower(str(" + column + ")) like lower(:text)").collect(Collectors.joining(" or "));
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

    private List<String> getFilterableColumns(String statement)
    {
        List<String> columns = new ArrayList<>();
        String selectClause =
            statement.substring("select ".length(), StringUtils.indexOfIgnoreCase(statement, " from ")).trim();
        selectClause = StringUtils.removeStartIgnoreCase(selectClause, "distinct ");
        for (String column : COLUMN_SEPARATOR.split(selectClause)) {
            // Remove the column alias.
            String columnWithoutAlias = column;
            int aliasPosition = column.lastIndexOf(" as ");
            if (aliasPosition > 0) {
                columnWithoutAlias = column.substring(0, aliasPosition).trim();
                String alias = column.substring(aliasPosition + 4).trim();
                if (alias.startsWith("unfilterable")) {
                    continue;
                }
            }
            columns.add(columnWithoutAlias);
        }
        return columns;
    }
}
