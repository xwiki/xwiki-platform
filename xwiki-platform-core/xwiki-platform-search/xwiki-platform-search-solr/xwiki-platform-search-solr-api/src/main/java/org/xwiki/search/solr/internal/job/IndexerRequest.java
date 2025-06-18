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

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.xwiki.job.AbstractRequest;
import org.xwiki.job.Request;
import org.xwiki.model.reference.EntityReference;

/**
 * The request used to configure {@link IndexerJob}.
 * 
 * @version $Id$
 * @since 5.1RC1
 */
public class IndexerRequest extends AbstractRequest
{
    /**
     * Serialization identifier.
     */
    private static final long serialVersionUID = 1L;

    /**
     * @see #getRootReference()
     */
    private EntityReference rootReference;

    /**
     * @see #isOverwrite()
     */
    private boolean overwrite;

    /**
     * @see #isRemoveMissing()
     */
    private boolean removeMissing = true;

    /**
     * @see #isCleanInvalid()
     */
    private boolean cleanInvalid;

    /**
     * The default constructor.
     */
    public IndexerRequest()
    {
    }

    /**
     * @param request the request to copy
     */
    public IndexerRequest(Request request)
    {
        super(request);
    }

    /**
     * @return the reference from which to work
     */
    public EntityReference getRootReference()
    {
        return this.rootReference;
    }

    /**
     * @param rootReference the reference from which to work
     */
    public void setRootReference(EntityReference rootReference)
    {
        this.rootReference = rootReference;
    }

    /**
     * @return if false documents are indexed only if they don't already exist in Solr index (version is taken into
     *         account), if true all documents are sent and overwrite what's already in Solr if any
     */
    public boolean isOverwrite()
    {
        return this.overwrite;
    }

    /**
     * @param overwrite if false documents are indexed only if they don't already exist in Solr index (version is taken
     *            into account), if true all documents are sent and overwrite what's already in Solr if any
     */
    public void setOverwrite(boolean overwrite)
    {
        this.overwrite = overwrite;
    }

    /**
     * @return if true the Solr document not in database anymore are cleaned, if false nothing is checked
     */
    public boolean isRemoveMissing()
    {
        return this.removeMissing;
    }

    /**
     * @param removeMissing if true the Solr document not in database anymore are cleaned, if false nothing is checked
     */
    public void setRemoveMissing(boolean removeMissing)
    {
        this.removeMissing = removeMissing;
    }

    /**
     * @return if true the invalid Solr document entries are removed, if false nothing is checked
     * @since 17.8.0RC1
     */
    public boolean isCleanInvalid()
    {
        return this.cleanInvalid;
    }

    /**
     * @param cleanInvalid if true the invalid Solr document entries are removed, if false nothing is checked
     * @since 17.8.0RC1
     */
    public void setCleanInvalid(boolean cleanInvalid)
    {
        this.cleanInvalid = cleanInvalid;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        IndexerRequest that = (IndexerRequest) o;

        return new EqualsBuilder()
            .appendSuper(super.equals(o))
            .append(overwrite, that.overwrite)
            .append(removeMissing, that.removeMissing)
            .append(rootReference, that.rootReference)
            .isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(3, 49)
            .appendSuper(super.hashCode())
            .append(rootReference)
            .append(overwrite)
            .append(removeMissing)
            .toHashCode();
    }
}
