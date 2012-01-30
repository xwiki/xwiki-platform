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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryExecutor;

/**
 * The default QueryExecutor, this uses The HqlQueryExecutor since XWikiHibernateStore is the default store. Wrapping
 * the HQL executor with this allows the main store hint in xwiki.cfg to be used to select the QueryExecutor.
 * 
 * @version $Id$
 * @since 3.2M2
 */
@Component
@Singleton
public class DefaultQueryExecutor implements QueryExecutor
{
    /** The Hibernate HQL query executor. */
    @Inject
    @Named("hql")
    private QueryExecutor executor;

    @Override
    public <T> List<T> execute(final Query query) throws QueryException
    {
        return this.executor.execute(query);
    }
}
