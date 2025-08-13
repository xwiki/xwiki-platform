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
package org.xwiki.search.solr.internal.search;

import java.util.Comparator;

import jakarta.inject.Inject;

import org.xwiki.search.solr.SolrUtils;
import org.xwiki.search.solr.internal.SolrSchemaUtils;

/**
 * Base class for {@link SearchCoreMigration} implementations.
 * 
 * @version $Id$
 * @since 17.8.0RC1
 */
public abstract class AbstractSearchCoreMigration implements SearchCoreMigration
{
    @Inject
    protected SolrUtils solrUtils;

    @Inject
    protected SolrSchemaUtils solrSchema;

    @Override
    public int compareTo(SearchCoreMigration other)
    {
        return Comparator.comparingLong(SearchCoreMigration::getVersion).compare(this, other);
    }
}
