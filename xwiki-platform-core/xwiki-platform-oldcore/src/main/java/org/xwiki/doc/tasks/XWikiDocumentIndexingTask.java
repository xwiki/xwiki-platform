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
package org.xwiki.doc.tasks;

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
public class XWikiDocumentIndexingTask
{
    private long id;

    private long docId;

    private String version;

    private String type;

    private String instanceId;

    private Date timestamp;

    /**
     * Getter for {@link #id}.
     *
     * @return the synthetic id of this deleted attachment. Uniquely identifies a link
     * @since 14.3RC1
     */
    public long getId()
    {
        return this.id;
    }

    /**
     * Setter for {@link #id}.
     *
     * @param id the synthetic id to set. Used only by hibernate
     * @since 14.3RC1
     */
    // This method is private because it is only used reflexively by Hibernate.
    @SuppressWarnings("java:S1144")
    private void setId(long id)
    {
        this.id = id;
    }

    /**
     * @return the document id
     * @since 14.3RC1
     */
    public long getDocId()
    {
        return this.docId;
    }

    /**
     * @param docId the id of the document to be processed
     * @since 14.3RC1
     */
    public void setDocId(long docId)
    {
        this.docId = docId;
    }

    /**
     * @return the version to of the document to be processed
     * @since 14.3RC1
     */
    public String getVersion()
    {
        return this.version;
    }

    /**
     * @param version the version of the document to be processed
     * @since 14.3RC1
     */
    public void setVersion(String version)
    {
        this.version = version;
    }

    /**
     * @return the type of the task to do on the document
     * @since 14.3RC1
     */
    public String getType()
    {
        return this.type;
    }

    /**
     * @param type the type of the task to do on the document
     * @since 14.3RC1
     */
    public void setType(String type)
    {
        // Replace duplicate strings with a canonical version. This is useful to lower the memory footprint when 
        // initializing tasks from sources where each type string is a new object (e.g., from the database), to avoid
        // having a lot of String object with the same task type value (e.g., mention or links). We don't do the same
        // for other field as they are either transient (e.g., instanceId), or more diverse (e.g., version).
        this.type = type.intern();
    }

    /**
     * @return the identifier of the instance that queued the task
     * @since 14.3RC1
     */
    public String getInstanceId()
    {
        return this.instanceId;
    }

    /**
     * @param instanceId the identifier of the instance that queued the task
     * @since 14.3RC1
     */
    public void setInstanceId(String instanceId)
    {
        this.instanceId = instanceId;
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

        XWikiDocumentIndexingTask that = (XWikiDocumentIndexingTask) o;

        return new EqualsBuilder()
            .append(this.id, that.id)
            .append(this.docId, that.docId)
            .append(this.version, that.version)
            .append(this.type, that.type)
            .append(this.instanceId, that.instanceId)
            .isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(17, 37)
            .append(this.id)
            .append(this.docId)
            .append(this.version)
            .append(this.type)
            .append(this.instanceId)
            .toHashCode();
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this)
            .append("id", this.id)
            .append("docId", this.docId)
            .append("version", this.version)
            .append("type", this.type)
            .append("instanceId", this.instanceId)
            .append("timestamp", this.timestamp)
            .toString();
    }
}
