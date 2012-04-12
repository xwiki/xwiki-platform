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

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryFilter;

import java.util.List;
import java.util.Map;

/**
 * Query wrapper that allows to set filter from the filter component hint.
 *
 * @version $Id$
 * @since 4.0RC1
 */
public class ScriptQuery implements Query
{
    /**
     * Used to log possible warnings.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(ScriptQuery.class);

    /**
     * Used to retrieve {@link org.xwiki.query.QueryFilter} implementations.
     */
    private ComponentManager componentManager;

    /**
     * The wrapped {@link Query}.
     */
    private Query query;

    /**
     * Constructor.
     *
     * @param query the query object to wrap.
     * @param cm the xwiki component manager.
     */
    public ScriptQuery(Query query, ComponentManager cm)
    {
        this.query = query;
        this.componentManager = cm;
    }

    /**
     * Set a filter in the wrapped query from the filter component hint.
     *
     * @param filter the hint of the component filter to set in the wrapped query.
     * @return this query object.
     */
    public Query setFilter(String filter)
    {
        if (!StringUtils.isBlank(filter)) {
            try {
                QueryFilter queryFilter = this.componentManager.getInstance(QueryFilter.class, filter);
                setFilter(queryFilter);
            } catch (ComponentLookupException e) {
                // We need to avoid throwing exceptions in the wiki if the filter does not exist.
                LOGGER.warn("Failed to load QueryFilter with component hint [{}]", filter);
            }
        } else {
            query.setFilter(null);
        }

        return this;
    }

    @Override
    public String getStatement() {
        return query.getStatement();
    }

    @Override
    public String getLanguage() {
        return query.getLanguage();
    }

    @Override
    public boolean isNamed() {
        return query.isNamed();
    }

    @Override
    public Query setWiki(String wiki) {
        query.setWiki(wiki);
        return this;
    }

    @Override
    public String getWiki() {
        return query.getWiki();
    }

    @Override
    public Query bindValue(String var, Object val) {
        query.bindValue(var, val);
        return this;
    }

    @Override
    public Query bindValue(int index, Object val) {
        query.bindValue(index, val);
        return this;
    }

    @Override
    public Query bindValues(List<Object> values) {
        query.bindValues(values);
        return this;
    }

    @Override
    public Map<String, Object> getNamedParameters() {
        return query.getNamedParameters();
    }

    @Override
    public Map<Integer, Object> getPositionalParameters() {
        return query.getPositionalParameters();
    }

    @Override
    public Query setFilter(QueryFilter filter) {
        query.setFilter(filter);
        return this;
    }

    @Override
    public QueryFilter getFilter() {
        return query.getFilter();
    }

    @Override
    public Query setLimit(int limit) {
        query.setLimit(limit);
        return this;
    }

    @Override
    public Query setOffset(int offset) {
        query.setOffset(offset);
        return this;
    }

    @Override
    public int getLimit() {
        return query.getLimit();
    }

    @Override
    public int getOffset() {
        return query.getOffset();
    }

    @Override
    public <T> List<T> execute() throws QueryException {
        return query.execute();
    }
}