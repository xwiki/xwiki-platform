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

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.query.Query;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * Query filter making sure unique results are retrieved by a {@link org.xwiki.query.Query}. This transformation only
 * works on queries selecting full names of XWikiDocuments.
 *
 * @version $Id$
 * @since 4.1M1
 */
@Component
@Named("unique")
@Singleton
public class UniqueDocumentFilter extends AbstractQueryFilter
{
    /**
     * Used to log debug information.
     */
    @Inject
    private Logger logger;

    /**
     * @param statement statement to filter.
     * @return true if the filter can be applied to the passed statement, false otherwise.
     */
    private boolean isFilterable(String statement)
    {
        return getSelectColumns(statement).contains(FULLNAME_COLUMN)
            && !getSelectColumns(statement).contains("distinct doc.fullName");
    }

    @Override
    public String filterStatement(String statement, String language)
    {
        StringBuilder builder = new StringBuilder();
        String result = statement;
        String original = statement;

        if (Query.HQL.equals(language) && isFilterable(statement)) {
            String prettySeparator = ", ";
            builder.append("select distinct doc.fullName");

            // Put back original select columns.
            List<String> selectColumns = getSelectColumns(statement);
            for (String column : selectColumns) {
                if (!FULLNAME_COLUMN.equals(column)) {
                    builder.append(prettySeparator);
                    builder.append(column);
                }
            }
            // Put the order by columns in the select clause to circumvent HQL limitations (distinct+order by).
            for (String column : getOrderByColumns(statement)) {
                if (!FULLNAME_COLUMN.equals(column) && !selectColumns.contains(column)) {
                    builder.append(prettySeparator);
                    builder.append(column);
                }
            }
            builder.append(" ");
            builder.append(statement.substring(statement.indexOf(" from ")).trim());
            result = builder.toString();
        }

        if (!statement.equals(result)) {
            logger.debug("Query [{}] has been transformed into [{}]", original, result);
        }

        return result;
    }

    @Override
    public List filterResults(List results)
    {
        // If we had to put multiple columns in the select we need to remove them.
        if (results.size() > 0 && results.get(0).getClass().isArray()) {
            List filteredResults = new ArrayList<Object>();
            for (Object result : results) {
                Object[] actualResult = (Object[]) result;
                filteredResults.add(actualResult[0]);
            }

            return filteredResults;
        }

        return results;
    }
}
