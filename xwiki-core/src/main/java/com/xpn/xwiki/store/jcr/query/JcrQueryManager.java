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
package com.xpn.xwiki.store.jcr.query;

import java.util.ResourceBundle;

import org.apache.commons.lang.NotImplementedException;
import org.xwiki.context.Execution;

import com.xpn.xwiki.store.query.AbstractQueryManager;
import com.xpn.xwiki.store.query.Query;

/**
 * QueryManager implementation for Java Content Repository v1.0.
 * @version $Id$
 * @since 1.6M1
 */
public class JcrQueryManager extends AbstractQueryManager
{
    /**
     * Used for get named queries.
     */
    ResourceBundle queriesBundle = ResourceBundle.getBundle("JcrQueries");

    /**
     * Used for creating JcrQuery.
     */
    Execution execution;

    /**
     * @return Execution object, used for access to store system.
     */
    protected Execution getExecution()
    {
        return execution;
    }

    /**
     * Default constructor.
     */
    public JcrQueryManager()
    {
        languages.add(Query.XPATH);
    }

    /**
     * {@inheritDoc}
     */
    public Query createQuery(String statement, String language)
    {
        if (hasLanguage(language)) {
            return new JcrQuery(statement, language, getExecution());
        } else {
            throw new NotImplementedException();
        }
    }

    /**
     * {@inheritDoc}
     */
    public Query getNamedQuery(String queryName)
    {
        String statement = this.queriesBundle.getString(queryName);
        return createQuery(statement, Query.XPATH);
    }
}
