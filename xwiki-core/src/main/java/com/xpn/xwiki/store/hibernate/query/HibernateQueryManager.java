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

import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;

import com.xpn.xwiki.store.hibernate.HibernateSessionFactory;
import com.xpn.xwiki.store.query.AbstractQueryManager;
import com.xpn.xwiki.store.query.Query;
import com.xpn.xwiki.util.Util;
import com.xpn.xwiki.web.Utils;

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
     * Default constructor.
     */
    public HibernateQueryManager()
    {
        languages.add(Query.HQL);
    }

    /**
     * {@inheritDoc}
     */
    public void initialize() throws InitializationException
    {
        sessionFactory.getConfiguration().addInputStream(Util.getResourceAsStream(mappingPath));
    }

    /**
     * {@inheritDoc}
     */
    public Query getNamedQuery(String queryName)
    {
        HibernateNamedQuery query = (HibernateNamedQuery) Utils.getComponent(Query.ROLE, HibernateNamedQuery.hint);
        query.setStatement(queryName);
        return query;
    }
}
