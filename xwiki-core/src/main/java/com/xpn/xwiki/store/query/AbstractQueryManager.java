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
package com.xpn.xwiki.store.query;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * This is abstract QueryManager implementation.
 * 
 * @version $Id$
 * @since 1.6M1
 */
public abstract class AbstractQueryManager implements QueryManager
{
    /**
     * Field for supported languages.
     */
    protected Set<String> languages = new HashSet<String>();

    /**
     * {@inheritDoc}
     */
    public Set<String> getLanguages()
    {
        return Collections.unmodifiableSet(this.languages);
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasLanguage(String language)
    {
        return getLanguages().contains(language);
    }

    /**
     * {@inheritDoc}
     */
    public Query createQuery(String statement, String language) throws QueryException
    {
        if (hasLanguage(language)) {
            return new QueryImpl(statement, language, getExecutor(language));
        } else {
            throw new QueryException("Language [" + language + "] is not supported", null, null);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Query getNamedQuery(String queryName) throws QueryException
    {
        return new QueryImpl(queryName, getExecutor(null));
    }

    /**
     * @param language query language
     * @return QueryExecutor for this language
     */
    protected abstract QueryExecutor getExecutor(String language);
}
