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
package org.xwiki.job.store.internal.entity;

import java.io.Serializable;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * Hibernate entity mapping the {@code job_status_logs} table.
 *
 * @version $Id$
 * @since 18.2.0RC1
 */
public class JobStatusLogEntryEntity implements Serializable
{
    private static final long serialVersionUID = 1L;

    private String nodeId;

    private String statusKey;

    private long lineIndex;

    private int level;

    private long timeStamp;

    private String message;

    private String formattedMessage;

    private String throwableType;

    private String throwableMessage;

    private String logSerialized;

    /**
     * @return the cluster node identifier this log entry belongs to
     */
    public String getNodeId()
    {
        return this.nodeId;
    }

    /**
     * @param nodeId the cluster node identifier this log entry belongs to
     */
    public void setNodeId(String nodeId)
    {
        this.nodeId = nodeId;
    }

    /**
     * @return the status key this log entry belongs to
     */
    public String getStatusKey()
    {
        return this.statusKey;
    }

    /**
     * @param statusKey the status key this log entry belongs to
     */
    public void setStatusKey(String statusKey)
    {
        this.statusKey = statusKey;
    }

    /**
     * @return the zero-based position inside the log tail
     */
    public long getLineIndex()
    {
        return this.lineIndex;
    }

    /**
     * @param lineIndex the zero-based position inside the log tail
     */
    public void setLineIndex(long lineIndex)
    {
        this.lineIndex = lineIndex;
    }

    /**
     * @return the log level of the entry as an ordinal value
     */
    public int getLevel()
    {
        return this.level;
    }

    /**
     * @param level the log level of the entry as an ordinal value
     */
    public void setLevel(int level)
    {
        this.level = level;
    }

    /**
     * @return the timestamp of the log event in milliseconds since epoch
     */
    public long getTimeStamp()
    {
        return this.timeStamp;
    }

    /**
     * @param timeStamp the timestamp of the log event in milliseconds since epoch
     */
    public void setTimeStamp(long timeStamp)
    {
        this.timeStamp = timeStamp;
    }

    /**
     * @return the raw message template
     */
    public String getMessage()
    {
        return this.message;
    }

    /**
     * @param message the raw message template
     */
    public void setMessage(String message)
    {
        this.message = message;
    }

    /**
     * @return the formatted message if available
     */
    public String getFormattedMessage()
    {
        return this.formattedMessage;
    }

    /**
     * @param formattedMessage the formatted message if available
     */
    public void setFormattedMessage(String formattedMessage)
    {
        this.formattedMessage = formattedMessage;
    }

    /**
     * @return the fully qualified throwable class name, if any
     */
    public String getThrowableType()
    {
        return this.throwableType;
    }

    /**
     * @param throwableType the fully qualified throwable class name, if any
     */
    public void setThrowableType(String throwableType)
    {
        this.throwableType = throwableType;
    }

    /**
     * @return the throwable message, if any
     */
    public String getThrowableMessage()
    {
        return this.throwableMessage;
    }

    /**
     * @param throwableMessage the throwable message, if any
     */
    public void setThrowableMessage(String throwableMessage)
    {
        this.throwableMessage = throwableMessage;
    }

    /**
     * @return the XStream-serialized log event payload
     */
    public String getLogSerialized()
    {
        return this.logSerialized;
    }

    /**
     * @param logSerialized the XStream-serialized log event payload
     */
    public void setLogSerialized(String logSerialized)
    {
        this.logSerialized = logSerialized;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) {
            return true;
        }

        if (!(o instanceof JobStatusLogEntryEntity that)) {
            return false;
        }

        return new EqualsBuilder().append(getLineIndex(), that.getLineIndex())
            .append(getNodeId(), that.getNodeId())
            .append(getStatusKey(), that.getStatusKey())
            .isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(17, 37).append(getNodeId())
            .append(getStatusKey())
            .append(getLineIndex())
            .toHashCode();
    }
}
