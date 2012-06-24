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
package org.xwiki.query.xwql.internal;

import java.util.List;
import java.util.Map.Entry;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.EntityType;
import org.xwiki.model.ModelContext;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryExecutor;
import org.xwiki.query.QueryFilter;
import org.xwiki.query.QueryManager;

@Component
@Named("xwql")
@Singleton
public class XWQLQueryExecutor implements QueryExecutor
{
    @Inject
    @Named("hql")
    private QueryTranslator translator;

    @Inject
    private ComponentManager componentManager;

    @Inject
    private ModelContext context;

    public QueryManager getQueryManager() throws ComponentLookupException
    {
        // We can't inject QueryManager because of cyclic dependency.
        return this.componentManager.getInstance(QueryManager.class);
    }

    @Override
    public <T> List<T> execute(Query query) throws QueryException
    {
        EntityReference currentEntityReference = this.context.getCurrentEntityReference();

        Query nativeQuery;
        try {
            if (query.getWiki() != null) {
                if (currentEntityReference.getType() == EntityType.WIKI) {
                    this.context.setCurrentEntityReference(new WikiReference(query.getWiki()));
                } else {
                    this.context.setCurrentEntityReference(currentEntityReference.replaceParent(
                        currentEntityReference.extractReference(EntityType.WIKI), new WikiReference(query.getWiki())));
                }
            }

            nativeQuery =
                getQueryManager().createQuery(this.translator.translate(query.getStatement()),
                    this.translator.getOutputLanguage());
            nativeQuery.setLimit(query.getLimit());
            nativeQuery.setOffset(query.getOffset());
            nativeQuery.setWiki(query.getWiki());
            if (query.getFilters() != null) {
                for (QueryFilter filter : query.getFilters()) {
                    nativeQuery.addFilter(filter);
                }
            }
            for (Entry<String, Object> e : query.getNamedParameters().entrySet()) {
                nativeQuery.bindValue(e.getKey(), e.getValue());
            }
            for (Entry<Integer, Object> e : query.getPositionalParameters().entrySet()) {
                nativeQuery.bindValue(e.getKey(), e.getValue());
            }

            return nativeQuery.execute();
        } catch (Exception e) {
            if (e instanceof QueryException) {
                throw (QueryException) e;
            }
            throw new QueryException("Exception while translating [" + query.getStatement() + "] XWQL query to the ["
                + this.translator.getOutputLanguage() + "] language", query, e);
        } finally {
            this.context.setCurrentEntityReference(currentEntityReference);
        }
    }

    public QueryTranslator getTranslator()
    {
        return this.translator;
    }
}
