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
package com.xpn.xwiki.doc.tasks;

import java.io.Serializable;
import java.util.Date;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Hold the information about a queued task.
 *
 * @version $Id$
 * @since 14.1RC1
 */
public class XWikiDocumentIndexingTask implements Serializable
{
    private static final long serialVersionUID = -2306455239336814283L;

    private XWikiDocumentIndexingTaskId id;

    private Date timestamp;

    /**
     * @return the compound id of the task
     */
    public XWikiDocumentIndexingTaskId getId()
    {
        return this.id;
    }

    /**
     * @param id the compound id of the task
     */
    public void setId(XWikiDocumentIndexingTaskId id)
    {
        this.id = id;
    }

    /**
     * @return the timestamp of the insertion of the task in the queue
     */
    public Date getTimestamp()
    {
        return this.timestamp;
    }

    /**
     * @param timestamp the timestamp of the insertion of the task in the queue
     */
    public void setTimestamp(Date timestamp)
    {
        this.timestamp = timestamp;
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

        XWikiDocumentIndexingTask xWikiTask = (XWikiDocumentIndexingTask) o;
        
        return new EqualsBuilder().append(this.id, xWikiTask.id).isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(17, 37).append(this.id).toHashCode();
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this)
            .append("id", this.id)
            .append("timestamp", this.timestamp)
            .toString();
    }
}
