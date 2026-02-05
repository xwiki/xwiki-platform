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
import java.util.Date;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * Hibernate entity mapping the {@code job_status_summary} table.
 *
 * @version $Id$
 * @since 18.1.0RC1
 */
public class JobStatusSummaryEntity implements Serializable
{
    private static final long serialVersionUID = 1L;

    private String nodeId;

    private String statusKey;

    private String fullJobId;

    private String jobType;

    private String state;

    private Date startDate;

    private Date endDate;

    private boolean serialized;

    private boolean isolated;

    private boolean cancelable;

    private boolean canceled;

    private String blobLocator;

    /**
     * @return the cluster node identifier this job status summary belongs to
     */
    public String getNodeId()
    {
        return this.nodeId;
    }

    /**
     * @param nodeId the cluster node identifier this job status summary belongs to
     */
    public void setNodeId(String nodeId)
    {
        this.nodeId = nodeId;
    }

    /**
     * @return the normalized identifier used as the technical primary key
     */
    public String getStatusKey()
    {
        return this.statusKey;
    }

    /**
     * @param statusKey the normalized identifier used as the technical primary key
     */
    public void setStatusKey(String statusKey)
    {
        this.statusKey = statusKey;
    }

    /**
     * @return the original, unmodified job identifier
     */
    public String getFullJobId()
    {
        return this.fullJobId;
    }

    /**
     * @param fullJobId the original, unmodified job identifier
     */
    public void setFullJobId(String fullJobId)
    {
        this.fullJobId = fullJobId;
    }

    /**
     * @return the type of the job
     */
    public String getJobType()
    {
        return this.jobType;
    }

    /**
     * @param jobType the type of the job
     */
    public void setJobType(String jobType)
    {
        this.jobType = jobType;
    }

    /**
     * @return the state of the job as stored in the summary
     */
    public String getState()
    {
        return this.state;
    }

    /**
     * @param state the state of the job as stored in the summary
     */
    public void setState(String state)
    {
        this.state = state;
    }

    /**
     * @return the job start date
     */
    public Date getStartDate()
    {
        return this.startDate;
    }

    /**
     * @param startDate the job start date
     */
    public void setStartDate(Date startDate)
    {
        this.startDate = startDate;
    }

    /**
     * @return the job end date
     */
    public Date getEndDate()
    {
        return this.endDate;
    }

    /**
     * @param endDate the job end date
     */
    public void setEndDate(Date endDate)
    {
        this.endDate = endDate;
    }

    /**
     * @return {@code true} if the job requested serialization
     */
    public boolean isSerialized()
    {
        return this.serialized;
    }

    /**
     * @param serialized {@code true} if the job requested serialization
     */
    public void setSerialized(boolean serialized)
    {
        this.serialized = serialized;
    }

    /**
     * @return {@code true} if log isolation is enabled
     */
    public boolean isIsolated()
    {
        return this.isolated;
    }

    /**
     * @param isolated {@code true} if log isolation is enabled
     */
    public void setIsolated(boolean isolated)
    {
        this.isolated = isolated;
    }

    /**
     * @return {@code true} when the job can be cancelled
     */
    public boolean isCancelable()
    {
        return this.cancelable;
    }

    /**
     * @param cancelable {@code true} when the job can be cancelled
     */
    public void setCancelable(boolean cancelable)
    {
        this.cancelable = cancelable;
    }

    /**
     * @return {@code true} when the job has been cancelled
     */
    public boolean isCanceled()
    {
        return this.canceled;
    }

    /**
     * @param canceled {@code true} when the job has been cancelled
     */
    public void setCanceled(boolean canceled)
    {
        this.canceled = canceled;
    }

    /**
     * @return the locator of the blob that contains the full serialized status
     */
    public String getBlobLocator()
    {
        return this.blobLocator;
    }

    /**
     * @param blobLocator the locator of the blob that contains the full serialized status
     */
    public void setBlobLocator(String blobLocator)
    {
        this.blobLocator = blobLocator;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) {
            return true;
        }

        if (!(o instanceof JobStatusSummaryEntity that)) {
            return false;
        }

        return new EqualsBuilder().append(getNodeId(), that.getNodeId())
            .append(getStatusKey(), that.getStatusKey())
            .isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(17, 37).append(getNodeId()).append(getStatusKey()).toHashCode();
    }
}
