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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.phase.Composable;

/**
 * This is abstract QueryManager implementation.
 * It uses ComponentManager for create Queries by hint=language.
 * Named queries are not implemented here because they are storage-specific.
 * 
 * @version $Id$
 * @since 1.6M1
 */
public abstract class AbstractQueryManager implements QueryManager, Composable
{
    /**
     * Field for supported languages.
     */
    protected Set<String> languages = new HashSet<String>();

    /**
     * Component manager used for creating queries as components.
     */
    protected ComponentManager componentManager;

    /**
     * {@inheritDoc}
     * @see org.xwiki.component.phase.Composable#compose(org.xwiki.component.manager.ComponentManager)
     */
    public void compose(ComponentManager componentManager)
    {
        this.componentManager = componentManager;
    }

    /**
     * {@inheritDoc}
     */
    public Collection<String> getLanguages()
    {
        return Collections.unmodifiableCollection(this.languages);
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasLanguage(String language)
    {
        return this.languages.contains(language);
    }

    /**
     * {@inheritDoc}
     */
    public Query createQuery(String statement, String language)
    {
        Query query;
        try {
            query = (Query) this.componentManager.lookup(Query.ROLE, language);
        } catch (ComponentLookupException e) {
            throw new RuntimeException(e);
        }
        ((AbstractQuery) query).setStatement(statement);
        ((AbstractQuery) query).setLanguage(language);
        return query;
    }
}
