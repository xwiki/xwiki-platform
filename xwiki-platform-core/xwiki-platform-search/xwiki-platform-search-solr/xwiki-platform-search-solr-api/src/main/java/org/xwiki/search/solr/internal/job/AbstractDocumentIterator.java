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

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.search.solr.internal.api.SolrConfiguration;
import org.xwiki.text.XWikiToStringBuilder;

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
     * A document related entry.
     * 
     * @version $Id$
     * @since 17.8.0RC1
     */
    public static class DocumentIteratorEntry
    {
        private final WikiReference reference;

        private final long docId;

        private final String version;

        protected DocumentIteratorEntry(WikiReference reference, long docId, String version)
        {
            this.reference = reference;
            this.docId = docId;
            this.version = version;
        }

        /**
         * @return the reference
         */
        public WikiReference getWiki()
        {
            return reference;
        }

        /**
         * @return the docId
         */
        public long getDocId()
        {
            return docId;
        }

        /**
         * @return the version
         */
        public String getVersion()
        {
            return version;
        }

        @Override
        public int hashCode()
        {
            HashCodeBuilder builder = new HashCodeBuilder();

            builder.append(getWiki());
            builder.append(getDocId());
            builder.append(getVersion());

            return builder.build();
        }

        @Override
        public boolean equals(Object obj)
        {
            if (obj instanceof DocumentIteratorEntry otherEntry) {
                if (obj == this) {
                    return true;
                }

                EqualsBuilder builder = new EqualsBuilder();

                builder.append(getWiki(), otherEntry.getWiki());
                builder.append(getDocId(), otherEntry.getDocId());
                builder.append(getVersion(), otherEntry.getVersion());

                return builder.build();
            }

            return false;
        }

        @Override
        public String toString()
        {
            XWikiToStringBuilder builder = new XWikiToStringBuilder(this);

            builder.append("wiki", getWiki());
            builder.append("docId", getDocId());
            builder.append("version", getVersion());

            return builder.build();
        }
    }

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
