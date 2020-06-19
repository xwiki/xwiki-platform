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
 * A condition related to the status of an event for a specific entity.
 * 
 * @version $Id$
 * @since 12.5RC1
 */
public class StatusQueryCondition extends QueryCondition
{
    private String statusEntityId;

    private Boolean statusRead;

    /**
     * @param statusEntityId
     * @param statusRead
     * @param reversed
     */
    public StatusQueryCondition(String statusEntityId, Boolean statusRead, boolean reversed)
    {
        super(reversed);

        this.statusEntityId = statusEntityId;
        this.statusRead = statusRead;
    }

    /**
     * @return the status entity to filter on or null if not enabled
     */
    public String getStatusEntityId()
    {
        return this.statusEntityId;
    }

    /**
     * @return indicate if read or unread event should be selected, null if disabled
     */
    public Boolean getStatusRead()
    {
        return this.statusRead;
    }

    @Override
    public int hashCode()
    {
        HashCodeBuilder builder = new HashCodeBuilder();

        builder.appendSuper(super.hashCode());
        builder.append(getStatusEntityId());
        builder.append(getStatusRead());

        return builder.build();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == this) {
            return true;
        }

        if (obj instanceof StatusQueryCondition) {
            StatusQueryCondition status = (StatusQueryCondition) obj;

            EqualsBuilder builder = new EqualsBuilder();

            builder.appendSuper(super.equals(obj));
            builder.append(getStatusEntityId(), status.getStatusEntityId());
            builder.append(getStatusRead(), status.getStatusRead());

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
        builder.append("statusRead", getStatusRead());

        return builder.build();
    }
}
