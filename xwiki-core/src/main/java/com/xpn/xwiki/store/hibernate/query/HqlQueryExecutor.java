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

import org.hibernate.Session;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.context.Execution;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryExecutor;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.store.XWikiHibernateBaseStore.HibernateCallback;
import com.xpn.xwiki.store.hibernate.HibernateSessionFactory;
import com.xpn.xwiki.util.Util;

/**
 * QueryExecutor implementation for Hibernate Store.
 * 
 * @version $Id$
 * @since 1.6M1
 */
@Component("hql")
public class HqlQueryExecutor implements QueryExecutor, Initializable
{
    /**
     * Session factory needed for register named queries mapping.
     */
    @Requirement
    private HibernateSessionFactory sessionFactory;

    /**
     * Path to hibernate mapping with named queries. Configured via component manager.
     */
    private String mappingPath = "queries.hbm.xml";

    /**
     * Used for access to XWikiContext.
     */
    @Requirement
    private Execution execution;

    /**
     * {@inheritDoc}
     * @see Initializable#initialize()
     */
    public void initialize() throws InitializationException
    {
        this.sessionFactory.getConfiguration().addInputStream(Util.getResourceAsStream(this.mappingPath));
    }

    /**
     * {@inheritDoc}
     */
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
                public List<T> doInHibernate(Session session)
                {
                    org.hibernate.Query hquery = createHibernateQuery(session, query);
                    populateParameters(hquery, query);
                    return hquery.list();
                }
            });
        } catch (XWikiException e) {
            throw new QueryException("Exception while execute query", query, e);
        } finally {
            getContext().setDatabase(olddatabase);
        }
    }

    /**
     * @param session hibernate session
     * @param query Query object
     * @return hibernate query
     */
    protected org.hibernate.Query createHibernateQuery(Session session, Query query)
    {
        return query.isNamed()
            ? session.getNamedQuery(query.getStatement())
            : session.createQuery(query.getStatement());
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
        return (XWikiContext) execution.getContext().getProperty("xwikicontext");
    }
}
