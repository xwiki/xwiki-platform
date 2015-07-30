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

import javax.inject.Inject;

import org.slf4j.Logger;
import org.xwiki.query.Query;
import org.xwiki.query.QueryFilter;

/**
 * Make it easy to create a {@link QueryFilter} that adds a "where" clause.
 *
 * @version $Id$
 * @since 5.1M2
 */
public abstract class AbstractWhereQueryFilter implements QueryFilter
{
    /**
     * SQL where token.
     */
    private static final String WHERE = " where ";

    /**
     * Used to log debug information.
     */
    @Inject
    private Logger logger;

    /**
     * @param statement statement to filter.
     * @return true if the filter can be applied to the passed statement, false otherwise.
     */
    protected boolean isFilterable(String statement)
    {
        // This could be replaced by the following regex: "xwikidocument(\\s)+(as)?(\\s)+doc"
        return statement.indexOf("xwikidocument as doc") > -1 || statement.indexOf("xwikidocument doc") > -1;
    }

    /**
     * Inserts the passed where clause in the passed statement.
     *
     * @param whereClause the clause to insert
     * @param statement the statement in which to insert the clause
     * @param language the language of the statement
     * @return the modified statement that includes the passed where clause
     */
    protected String insertWhereClause(String whereClause, String statement, String language)
    {
        String result = statement.trim();
        String lowerStatement = result.toLowerCase();
        String original = result;

        if (Query.HQL.equals(language) && isFilterable(lowerStatement)) {

            int whereIdx = lowerStatement.indexOf(WHERE);
            int orderByIdx = Math.min(lowerStatement.indexOf(" order by "), Integer.MAX_VALUE);
            int groupByIdx = Math.min(lowerStatement.indexOf(" group by "), Integer.MAX_VALUE);
            // We need to handle the case where there's only one of them and not both (ie. avoid -1)
            orderByIdx = orderByIdx < 0 ? Integer.MAX_VALUE : orderByIdx;
            groupByIdx = groupByIdx < 0 ? Integer.MAX_VALUE : groupByIdx;
            // Get the index of the first or only one
            int orderOrGroupByIdx = Math.min(orderByIdx, groupByIdx);

            if (whereIdx >= 0) {
                // With 'WHERE'
                // We need the index at the end of the " where " part
                whereIdx = whereIdx + WHERE.length();
                int whereEndIdx = Math.min(orderOrGroupByIdx, lowerStatement.length());
                result = result.substring(0, whereEndIdx) + ")" + result.substring(whereEndIdx);
                result =
                    result.substring(0, whereIdx) + whereClause + " and (" + result.substring(whereIdx);
            } else {
                // Without 'WHERE', look for 'ORDER BY' or 'GROUP BY'
                if (orderOrGroupByIdx > 0 && orderOrGroupByIdx < Integer.MAX_VALUE) {
                    // Without 'WHERE', but with 'ORDER BY' and/or 'GROUP BY'
                    result = result.substring(0, orderOrGroupByIdx) + WHERE + whereClause
                        + result.substring(orderOrGroupByIdx);
                } else {
                    // Without 'WHERE', 'ORDER BY' or 'GROUP BY'... This should not happen at all.
                    result = result + WHERE + whereClause;
                }
                // TODO: Take into account GROUP BY, HAVING and other keywords when there's no WHERE in the query
            }
        }

        if (!original.equals(result)) {
            logger.debug("Query [{}] has been transformed into [{}]", original, result);
        }

        return result;
    }
}
