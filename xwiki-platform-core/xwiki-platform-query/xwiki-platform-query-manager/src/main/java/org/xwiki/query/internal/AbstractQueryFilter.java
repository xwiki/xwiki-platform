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

import org.apache.commons.collections.ListUtils;
import org.xwiki.query.QueryFilter;

/**
 * Abstract {@link QueryFilter} providing methods to parse Query statements.
 *
 * @version $Id$
 * @since 4.1M1
 */
public abstract class AbstractQueryFilter implements QueryFilter
{
    /**
     * Usual identifier of the document full name column in our queries.
     */
    protected static final String FULLNAME_COLUMN = "doc.fullName";

    /**
     * Distinct document full name column in our queries.
     */
    protected static final String DISTINCT_FULLNAME_COLUMN = "distinct doc.fullName";

    /**
     * Character used to separate columns in select, order by and group by clauses.
     */
    protected static final String COLUMN_SEPARATOR = ",";

    /**
     * Select clause keyword.
     */
    private static final String SELECT = "select ";

    /**
     * From clause keyword.
     */
    private static final String FROM = " from ";

    /**
     * Order by clause keyword.
     */
    private static final String ORDER_BY = " order by ";

    /**
     * Group by clause keyword.
     */
    private static final String GROUP_BY = " group by ";

    /**
     * Get the select columns of a given statement.
     *
     * @param statement the statement to get the select clause for.
     * @return the select clause of the given statement.
     */
    protected List<String> getSelectColumns(String statement)
    {
        List<String> columns = new ArrayList<String>();
        String select = statement.substring(SELECT.length(), statement.indexOf(FROM));
        for (String column : select.split(COLUMN_SEPARATOR)) {
            columns.add(column.trim());
        }

        return columns;
    }

    /**
     * Get the list of columns present in the order by clause. This method is required because HSQLDB only
     * support SELECT DISTINCT SQL statements where the columns present in the order by clause are also present in the
     * select clause.
     *
     * @param statement the statement to evaluate.
     * @return the list of columns to return in the select clause as a string starting with ", " if there are columns or
     *         an empty string otherwise. The returned columns are extracted from the order by clause.
     */
    protected List<String> getOrderByColumns(String statement)
    {
        List<String> columns = new ArrayList<String>();
        int oidx = statement.indexOf(ORDER_BY);

        if (oidx > -1) {
            String fragment = statement.substring(oidx + ORDER_BY.length());
            int gidx = fragment.indexOf(GROUP_BY);
            if (gidx > -1) {
                fragment = fragment.substring(0, gidx);
            }
            fragment = fragment.replaceAll(" desc", "");
            fragment = fragment.replaceAll(" asc", "");

            for (String column : fragment.split(COLUMN_SEPARATOR)) {
                columns.add(column.trim());
            }

            return columns;
        }

        return ListUtils.EMPTY_LIST;
    }
}
