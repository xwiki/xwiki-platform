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

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.query.Query;
import org.xwiki.query.QueryFilter;

/**
 * Query filter adding a select clause to also return the document's language. This is useful if you need to adapt
 * your display based on the document's language (for example in search results).
 *
 * @version $Id$
 * @since 5.1M2
 */
@Component
@Named("language")
@Singleton
public class LanguageQueryFilter implements QueryFilter
{
    /**
     * The select clause to extend in order to also return the document's language.
     */
    private static final String SELECT_CLAUSE_TO_EXTEND = "select doc.fullName";

    /**
     * @param statement statement to filter.
     * @return true if the filter can be applied to the passed statement, false otherwise.
     */
    private boolean isFilterable(String statement)
    {
        return statement.startsWith(SELECT_CLAUSE_TO_EXTEND);
    }

    @Override
    public String filterStatement(String statement, String language)
    {
        String result = statement.trim();

        if (Query.HQL.equals(language) && isFilterable(result)) {
            result = SELECT_CLAUSE_TO_EXTEND + ", doc.language" + result.substring(SELECT_CLAUSE_TO_EXTEND.length());
        }

        return result;
    }

    @Override
    public List filterResults(List results)
    {
        return results;
    }
}
