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

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.query.Query;
import org.xwiki.query.QueryFilter;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * Query filter making sure unique results are retrieved by a {@link org.xwiki.query.Query}.
 *
 * @version $Id$
 * @since 4.1M1
 */
@Component
@Named("unique")
@Singleton
public class UniqueDocumentFilter implements QueryFilter
{
    /**
     * Select part to find and replace if it is present.
     */
    private static final String SELECT_CLAUSE = " doc.fullname from xwikidocument ";

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
        return statement.indexOf(SELECT_CLAUSE) > -1 && statement.indexOf("distinct doc.fullname") == -1;
    }

    @Override
    public String filterStatement(String statement, String language)
    {
        String result = statement.trim();
        String lowerStatement = result.toLowerCase();
        String original = result;

        if (Query.HQL.equals(language) && isFilterable(lowerStatement)) {
            int idx = lowerStatement.indexOf(SELECT_CLAUSE);
            result = result.substring(0, idx) + " distinct doc.fullName from XWikiDocument "
                    + result.substring(idx + 33);
        }

        if (!original.equals(result)) {
            logger.debug("Query [{}] has been transformed into [{}]", original, result);
        }

        return result;
    }
}
