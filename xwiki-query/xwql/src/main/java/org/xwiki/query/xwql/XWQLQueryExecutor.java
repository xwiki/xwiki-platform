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
package org.xwiki.query.xwql;

import java.util.List;
import java.util.Map.Entry;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryExecutor;
import org.xwiki.query.QueryManager;

@Component("xwql")
public class XWQLQueryExecutor implements QueryExecutor
{
    @Requirement("hql")
    private QueryTranslator translator;

    @Requirement
    private ComponentManager componentManager;

    public QueryManager getQueryManager() throws ComponentLookupException
    {
        // we can't inject QueryManager because of cyclic dependency.
        return (QueryManager) componentManager.lookup(QueryManager.class);
    }

    public <T> List<T> execute(Query query) throws QueryException
    {
        Query nativeQuery;
        try {
            nativeQuery = getQueryManager().createQuery(
                translator.translate(query.getStatement()),
                translator.getOutputLanguage());
            nativeQuery.setLimit(query.getLimit());
            nativeQuery.setOffset(query.getOffset());
            nativeQuery.setWiki(query.getWiki());
            for (Entry<String, Object> e : query.getNamedParameters().entrySet()) {
                nativeQuery.bindValue(e.getKey(), e.getValue());
            }
            for (Entry<Integer, Object> e : query.getPositionalParameters().entrySet()) {
                nativeQuery.bindValue(e.getKey(), e.getValue());
            }
            return nativeQuery.execute();
        } catch (Exception e) {
            if (e instanceof QueryException)
                throw (QueryException)e;
            throw new QueryException("Exception while translate XWQL query to " + translator.getOutputLanguage(), query, e);
        }
    }

    public QueryTranslator getTranslator()
    {
        return translator;
    }
}
