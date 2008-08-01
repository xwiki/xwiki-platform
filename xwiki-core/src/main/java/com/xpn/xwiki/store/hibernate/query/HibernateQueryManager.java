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

import org.apache.commons.lang.NotImplementedException;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.context.Execution;

import com.xpn.xwiki.store.hibernate.HibernateSessionFactory;
import com.xpn.xwiki.store.query.AbstractQueryManager;
import com.xpn.xwiki.store.query.Query;
import com.xpn.xwiki.util.Util;

/**
 * QueryManager implementation for Hibernate Store.
 * @version $Id$
 * @since 1.6M1
 */
public class HibernateQueryManager extends AbstractQueryManager implements Initializable
{
    /**
     * Session factory needed for register named queries mapping.
     * Injected via component manager.
     */
    private HibernateSessionFactory sessionFactory;

    /**
     * Path to hibernate mapping with named queries.
     * Configured via component manager.
     */
    private String mappingPath = "queries.hbm.xml";

    /**
     * Used for access to store system
     * Injected via component manager.
     */
    private Execution execution;

    /**
     * Default constructor.
     */
    public HibernateQueryManager()
    {
        this.languages.add(Query.HQL);
    }

    /**
     * {@inheritDoc}
     */
    public void initialize() throws InitializationException
    {
        this.sessionFactory.getConfiguration().addInputStream(Util.getResourceAsStream(this.mappingPath));
    }

    /**
     * @return Execution object for access to environment
     */
    protected Execution getExecution()
    {
        return execution;
    }

    /**
     * {@inheritDoc}
     */
    public Query createQuery(String statement, String language)
    {
        if (Query.HQL.equals(language)) {
            return new HqlQuery(statement, getExecution());
        } else {
            throw new NotImplementedException();
        }
    }

    /**
     * {@inheritDoc}
     */
    public Query getNamedQuery(String queryName)
    {
        return new HibernateNamedQuery(queryName, getExecution());
    }
}
