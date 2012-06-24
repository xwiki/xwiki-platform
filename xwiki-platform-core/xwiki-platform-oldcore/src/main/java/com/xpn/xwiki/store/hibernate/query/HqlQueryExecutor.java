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
package com.xpn.xwiki.store.hibernate.query;

import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.hibernate.Session;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.context.Execution;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryExecutor;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.store.XWikiHibernateBaseStore.HibernateCallback;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.store.hibernate.HibernateSessionFactory;
import com.xpn.xwiki.util.Util;
import org.xwiki.query.QueryFilter;

/**
 * QueryExecutor implementation for Hibernate Store.
 * 
 * @version $Id$
 * @since 1.6M1
 */
@Component
@Named("hql")
@Singleton
public class HqlQueryExecutor implements QueryExecutor, Initializable
{
    /**
     * Session factory needed for register named queries mapping.
     */
    @Inject
    private HibernateSessionFactory sessionFactory;

    /**
     * Path to hibernate mapping with named queries. Configured via component manager.
     */
    private String mappingPath = "queries.hbm.xml";

    /**
     * Used for access to XWikiContext.
     */
    @Inject
    private Execution execution;

    /**
     * The bridge to the old XWiki core API, used to access user preferences.
     */
    @Inject
    private DocumentAccessBridge documentAccessBridge;

    @Override
    public void initialize() throws InitializationException
    {
        this.sessionFactory.getConfiguration().addInputStream(Util.getResourceAsStream(this.mappingPath));
    }

    @Override
    public <T> List<T> execute(final Query query) throws QueryException
    {
        String olddatabase = getContext().getDatabase();
        try {
            if (query.getWiki() != null) {
                getContext().setDatabase(query.getWiki());
            }
            return getStore().executeRead(getContext(), true, new HibernateCallback<List<T>>()
            {
                @SuppressWarnings("unchecked")
                @Override
                public List<T> doInHibernate(Session session)
                {
                    org.hibernate.Query hquery = createHibernateQuery(session, query);
                    populateParameters(hquery, query);

                    if (query.getFilters() != null && !query.getFilters().isEmpty()) {
                        List results = hquery.list();
                        for (QueryFilter filter : query.getFilters()) {
                            results = filter.filterResults(results);
                        }

                        return (List<T>) results;
                    } else {
                        return hquery.list();
                    }
                }
            });
        } catch (XWikiException e) {
            throw new QueryException("Exception while execute query", query, e);
        } finally {
            getContext().setDatabase(olddatabase);
        }
    }

    /**
     * Append the required select clause to HQL short query statements. Short statements are the only way for users
     * without programming rights to perform queries. Such statements can be for example:
     * <ul>
     *     <li><code>, BaseObject obj where doc.fullName=obj.name and obj.className='XWiki.MyClass'</code></li>
     *     <li><code>where doc.creationDate > '2008-01-01'</code></li>
     * </ul>
     *
     * @param statement the statement to complete if required.
     * @return the complete statement if it had to be completed, the original one otherwise.
     */
    protected String completeShortFormStatement(String statement)
    {
        String lcStatement = statement.toLowerCase().trim();
        if (lcStatement.startsWith("where") || lcStatement.startsWith(",") || lcStatement.startsWith("order")) {
            return "select doc.fullName from XWikiDocument doc " + statement.trim();
        }

        return statement;
    }

    /**
     * @param session hibernate session
     * @param query Query object
     * @return hibernate query
     */
    protected org.hibernate.Query createHibernateQuery(Session session, Query query)
    {
        org.hibernate.Query hquery;
        String statement = query.getStatement();

        if (!query.isNamed()) {
            // handle short queries
            statement = completeShortFormStatement(statement);

            // Handle query filters
            if (query.getFilters() != null) {
                for (QueryFilter filter : query.getFilters()) {
                    statement = filter.filterStatement(statement, Query.HQL);
                }
            }
            hquery = session.createQuery(statement);
        } else {
            hquery = session.getNamedQuery(query.getStatement());
            if (query.getFilters() != null && !query.getFilters().isEmpty()) {
                // Since we can't modify the hibernate query statement at this point we need to create a new one to
                // apply the query filter. This comes with a performance cost, we could fix it by handling named queries
                // ourselves and not delegate them to hibernate. This way we would always get a statement that we can
                // transform before the execution.
                statement = hquery.getQueryString();
                for (QueryFilter filter : query.getFilters()) {
                    statement = filter.filterStatement(statement, Query.HQL);
                }
                hquery = session.createQuery(statement);
            }
        }

        return hquery;
    }

    /**
     * @param hquery query to populate parameters
     * @param query query from to populate.
     */
    protected void populateParameters(org.hibernate.Query hquery, Query query)
    {
        if (query.getOffset() > 0) {
            hquery.setFirstResult(query.getOffset());
        }
        if (query.getLimit() > 0) {
            hquery.setMaxResults(query.getLimit());
        }
        for (Entry<String, Object> e : query.getNamedParameters().entrySet()) {
            hquery.setParameter(e.getKey(), e.getValue());
        }
        if (query.getPositionalParameters().size() > 0) {
            int start = Collections.min(query.getPositionalParameters().keySet());
            if (start == 0) {
                // jdbc-style positional parameters. "?"
                for (Entry<Integer, Object> e : query.getPositionalParameters().entrySet()) {
                    hquery.setParameter(e.getKey(), e.getValue());
                }
            } else {
                // jpql-style. "?index"
                for (Entry<Integer, Object> e : query.getPositionalParameters().entrySet()) {
                    // hack. hibernate assume "?1" is named parameter, so use string "1".
                    hquery.setParameter("" + e.getKey(), e.getValue());
                }
            }
        }
    }

    /**
     * @return Store component
     */
    protected XWikiHibernateStore getStore()
    {
        return getContext().getWiki().getHibernateStore();
    }

    /**
     * @return XWiki Context
     */
    protected XWikiContext getContext()
    {
        return (XWikiContext) this.execution.getContext().getProperty("xwikicontext");
    }
}
