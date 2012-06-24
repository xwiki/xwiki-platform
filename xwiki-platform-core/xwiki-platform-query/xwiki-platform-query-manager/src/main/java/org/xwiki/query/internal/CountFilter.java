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

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.query.Query;

/**
 * Query filter transforming queries in order to make them return the total number of results instead of a list of
 * results. In order to do so a <code>select count()</code> will be inserted and possible <code>order by</code>
 * clauses will be dropped. This transformation will only work with queries selecting full names of XWikiDocuments.
 *
 * For example <code>select doc.fullName from XWikiDocument where doc.space='Main'</code> will be transformed into
 * <code>select count(doc.fullName) from XWikiDocument where doc.space='Main'</code>.
 *
 * @version $Id$
 * @since 4.1M1
 */
@Component
@Named("count")
@Singleton
public class CountFilter extends AbstractQueryFilter
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
        List<String> selectColumns = getSelectColumns(statement);
        return selectColumns.contains(FULLNAME_COLUMN) || selectColumns.contains(DISTINCT_FULLNAME_COLUMN);
    }

    @Override
    public String filterStatement(String statement, String language)
    {
        String result = statement.trim();
        String original = result;

        if (Query.HQL.equals(language) && isFilterable(statement)) {
            String distinct = getSelectColumns(statement).contains(DISTINCT_FULLNAME_COLUMN) ? "distinct " : "";
            result = "select count(" + distinct + "doc.fullName) "
                + result.substring(statement.indexOf("from XWikiDocument"));

            int oidx = result.indexOf("order by ");
            int gidx = result.indexOf("group by ");
            if (oidx > -1) {
                if (gidx > -1 && gidx > oidx) {
                    // There's an order by and a group by after, remove the order by only.
                    result = result.substring(0, oidx) + result.substring(gidx);
                } else {
                    // There's only an order by, remove it.
                    result = result.substring(0, oidx);
                }
            }
        }

        if (!original.equals(result)) {
            logger.debug("Query [{}] has been transformed into [{}]", original, result);
        }

        return result;
    }

    @Override
    public List filterResults(List results)
    {
        return results;
    }
}
