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

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Hold the information about the compound id of a queued task.
 *
 * @version $Id$
 * @see XWikiDocumentIndexingTask
 * @since 14.1RC1
 */
public class XWikiDocumentIndexingTaskId implements Serializable
{
    private static final long serialVersionUID = -7991702269746938433L;

    private long docId;

    private String version;
    
    private String type;

    private String instanceId;

    /**
     * @return the document id
     */
    public long getDocId()
    {
        return this.docId;
    }

    /**
     * @param docId the id of the document to be processed 
     */
    public void setDocId(long docId)
    {
        this.docId = docId;
    }

    /**
     * @return the version to of the document to be processed
     */
    public String getVersion()
    {
        return this.version;
    }

    /**
     * @param version the version of the document to be processed
     */
    public void setVersion(String version)
    {
        this.version = version;
    }

    /**
     * @return the type of the task to do on the document
     */
    public String getType()
    {
        return this.type;
    }

    /**
     * @param type the type of the task to do on the document
     */
    public void setType(String type)
    {
        this.type = type;
    }

    /**
     * @return the identifier of the instance that queued the task
     */
    public String getInstanceId()
    {
        return this.instanceId;
    }

    /**
     * @param instanceId the identifier of the instance that queued the task
     */
    public void setInstanceId(String instanceId)
    {
        this.instanceId = instanceId;
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this)
            .append("docId", this.docId)
            .append("type", this.type)
            .append("version", getVersion())
            .append("instanceId", getInstanceId())
            .toString();
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

        XWikiDocumentIndexingTaskId that = (XWikiDocumentIndexingTaskId) o;

        return new EqualsBuilder()
            .append(this.version, that.version)
            .append(this.docId, that.docId)
            .append(this.type, that.type)
            .append(this.instanceId, that.instanceId)
            .isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(17, 37)
            .append(this.docId)
            .append(this.version)
            .append(this.type)
            .append(this.instanceId)
            .toHashCode();
    }
}
