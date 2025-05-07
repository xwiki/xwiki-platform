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
package org.xwiki.search.solr.internal.job;

import javax.inject.Inject;

import org.xwiki.model.reference.EntityReference;
import org.xwiki.search.solr.internal.api.SolrConfiguration;

/**
 * Base class for {@link DocumentIterator}s.
 * 
 * @version $Id$
 * @param <T> the type of data used to determine if a document is up to date
 * @since 5.4.5
 */
public abstract class AbstractDocumentIterator<T> implements DocumentIterator<T>
{
    /**
     * Specifies the root entity whose documents are iterated. If {@code null} then all the documents are iterated.
     */
    protected EntityReference rootReference;

    @Inject
    protected SolrConfiguration solrConfiguration;

    private int limit;

    protected int getLimit()
    {
        // Cache the limit value to avoid possibly changing values during iteration.
        if (this.limit == 0) {
            this.limit = this.solrConfiguration.getSynchronizationBatchSize();
        }

        return this.limit;
    }

    @Override
    public void remove()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setRootReference(EntityReference rootReference)
    {
        this.rootReference = rootReference;
    }
}
