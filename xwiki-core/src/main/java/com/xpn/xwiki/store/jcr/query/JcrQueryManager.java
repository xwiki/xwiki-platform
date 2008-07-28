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

import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;

import com.xpn.xwiki.store.query.AbstractQueryManager;
import com.xpn.xwiki.store.query.Query;

/**
 * QueryManager implementation for Java Content Repository v1.0.
 * @version $Id$
 * @since 1.6M1
 */
public class JcrQueryManager extends AbstractQueryManager implements Initializable
{
    /**
     * Used for get named queries.
     */
    ResourceBundle queriesBundle;

    /**
     * {@inheritDoc}
     */
    public void initialize() throws InitializationException
    {
        this.queriesBundle = ResourceBundle.getBundle("JcrQueries");
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
