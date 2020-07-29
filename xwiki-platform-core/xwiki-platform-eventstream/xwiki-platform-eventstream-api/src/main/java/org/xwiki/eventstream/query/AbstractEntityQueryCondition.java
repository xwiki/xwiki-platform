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
package org.xwiki.eventstream.query;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.xwiki.text.XWikiToStringBuilder;

/**
 * A condition related to an entity associated with and event.
 * 
 * @version $Id$
 * @since 12.6
 */
public abstract class AbstractEntityQueryCondition extends QueryCondition
{
    private String statusEntityId;

    /**
     * @param statusEntityId the entity associated with the events
     * @param reversed true if the condition should be reversed
     */
    public AbstractEntityQueryCondition(String statusEntityId, boolean reversed)
    {
        super(reversed);

        this.statusEntityId = statusEntityId;
    }

    /**
     * @return the status entity to filter on or null if not enabled
     */
    public String getStatusEntityId()
    {
        return this.statusEntityId;
    }

    @Override
    public int hashCode()
    {
        HashCodeBuilder builder = new HashCodeBuilder();

        builder.appendSuper(super.hashCode());
        builder.append(getStatusEntityId());

        return builder.build();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == this) {
            return true;
        }

        if (obj instanceof AbstractEntityQueryCondition) {
            AbstractEntityQueryCondition status = (AbstractEntityQueryCondition) obj;

            EqualsBuilder builder = new EqualsBuilder();

            builder.appendSuper(super.equals(obj));
            builder.append(getStatusEntityId(), status.getStatusEntityId());

            return builder.build();
        }

        return false;
    }

    @Override
    public String toString()
    {
        ToStringBuilder builder = new XWikiToStringBuilder(this);

        builder.appendSuper(super.toString());
        builder.append("statusEntityId", getStatusEntityId());

        return builder.build();
    }
}
