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

import java.util.List;
import java.util.Map.Entry;

import org.hibernate.Session;
import org.xwiki.context.Execution;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.store.XWikiHibernateBaseStore.HibernateCallback;
import com.xpn.xwiki.store.query.AbstractQuery;
import com.xpn.xwiki.store.query.Query;

/**
 * Query implementation for Hibernate Query Language.
 * @since 1.6M1
 * @version $Id$
 */
public class HqlQuery extends AbstractQuery
{
    /**
     * Used for access to store system.
     */
    private Execution execution;

    /**
     * @param statement query statement
     * @param execution Execution object for access to store system
     */
    public HqlQuery(String statement, Execution execution)
    {
        super(statement, Query.HQL);
        this.execution = execution;
    }

    /**
     * @return Execution object for access to store system.
     */
    protected Execution getExecution()
    {
        return execution;
    }

    /**
     * @param session Hibernate Session
     * @return Hibernate Query object
     */
    protected org.hibernate.Query createQuery(Session session) 
    {
        return session.createQuery(getStatement());
    }

    /**
     * {@inheritDoc}
     */
    public <T> List<T> execute() throws XWikiException
    {
        final XWikiContext context = (XWikiContext) getExecution().getContext().getProperty("xwikicontext");
        final XWikiHibernateStore store = (XWikiHibernateStore) context.getWiki().getNotCacheStore();
        String olddatabase = context.getDatabase();        
        try {
            if (getWiki() != null) {
                context.setDatabase(getWiki());
            }
            return store.executeRead(context, true, new HibernateCallback<List<T>>() {
                @SuppressWarnings("unchecked")
                public List<T> doInHibernate(Session session) throws XWikiException {
                    org.hibernate.Query query = createQuery(session);
                    if (getOffset() > 0) {
                        query.setFirstResult(getOffset());
                    }
                    if (getLimit() > 0) {
                        query.setMaxResults(getLimit());
                    }
                    for (Entry<String, Object> e : getParameters().entrySet()) {
                        query.setParameter(e.getKey(), e.getValue());
                    }
                    return query.list();                
                }
            });
        } finally {
            context.setDatabase(olddatabase);
        }
    }
}
