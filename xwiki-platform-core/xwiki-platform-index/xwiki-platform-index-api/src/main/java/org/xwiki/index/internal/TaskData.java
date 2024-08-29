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
package org.xwiki.index.internal;

import java.io.Serializable;
import java.util.concurrent.CompletableFuture;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Store the information about a task.
 *
 * @version $Id$
 * @since 14.1RC1
 */
public class TaskData implements Serializable
{
    /**
     * Singleton stop instance.
     */
    public static final TaskData STOP = new TaskData(true);

    private static final long serialVersionUID = -1288732853577861921L;

    private long timestamp;

    private String version;

    private long docId;

    private String type;

    private int attempts;

    private boolean stopFlag;

    private String wikiId;

    private final transient CompletableFuture<TaskData> future = new CompletableFuture<>();

    /**
     * Default empty constructor.
     */
    public TaskData()
    {
        // Empty constructor required because a private constructor exists.
    }

    private TaskData(boolean stop)
    {
        this.stopFlag = stop;
        this.timestamp = 0L;
    }

    /**
     * Constructor with the business related information.
     *
     * @param docId the id of the document to analyze
     * @param version the version of the document to analyze
     * @param type the type of the task to execute
     * @param wikiId the id of the wiki in which the task must be executed
     */
    public TaskData(long docId, String version, String type, String wikiId)
    {
        this.docId = docId;
        this.version = version;
        this.type = type;
        this.wikiId = wikiId;
    }

    /**
     * @return the timestamp of the creation of the task
     */
    public long getTimestamp()
    {
        return this.timestamp;
    }

    /**
     * @param timestamp the timestamp of the creation of the task
     * @return the current task
     */
    public TaskData setTimestamp(long timestamp)
    {
        this.timestamp = timestamp;
        return this;
    }

    /**
     * @return the version of the document to analyze
     */
    public String getVersion()
    {
        return this.version;
    }

    /**
     * @param version the version of the document to analyze (e.g., 1.2)
     * @return the current task
     */
    public TaskData setVersion(String version)
    {
        this.version = version;
        return this;
    }

    /**
     * @return the id of the document to analyze
     */
    public long getDocId()
    {
        return this.docId;
    }

    /**
     * @param docId the id of the document to analyze
     * @return the current task
     */
    public TaskData setDocId(long docId)
    {
        this.docId = docId;
        return this;
    }

    /**
     * @return the type of the task to execute
     */
    public String getType()
    {
        return this.type;
    }

    /**
     * @param type the type of the task to execute
     * @return the current task
     */
    public TaskData setType(String type)
    {
        this.type = type;
        return this;
    }

    /**
     * Increase the failed attempts counter.
     */
    public void increaseAttempts()
    {
        this.attempts++;
    }

    /**
     * @return {@code true} if the task is a stop task, {@code false} otherwise. The only instance retuning true of the
     *     {@link #STOP} single instance
     */
    public boolean isStop()
    {
        return this.stopFlag;
    }

    /**
     * @return {@code true} if the task is a stop task or comes from another classload, {@code false} otherwise
     */
    public boolean isDeprecated()
    {
        return this.stopFlag && this != STOP;
    }

    /**
     * @return the identifier of the wiki where the task must be executed
     */
    public String getWikiId()
    {
        return this.wikiId;
    }

    /**
     * @param wikiId the identifier of the wiki where the task must be executed
     * @return the current task
     */
    public TaskData setWikiId(String wikiId)
    {
        this.wikiId = wikiId;
        return this;
    }

    /**
     * @return {@code true} when to many failed attempts have been made, {@code false} otherwise
     */
    public boolean tooManyAttempts()
    {
        // Try 10 times.
        return this.attempts > 9;
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

        TaskData taskData = (TaskData) o;

        return new EqualsBuilder()
            .append(this.version, taskData.version)
            .append(this.stopFlag, taskData.stopFlag)
            .append(this.docId, taskData.docId)
            .append(this.type, taskData.type)
            .append(this.wikiId, taskData.wikiId)
            .isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(17, 37)
            .append(this.version)
            .append(this.docId)
            .append(this.type)
            .append(this.stopFlag)
            .append(this.wikiId)
            .toHashCode();
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this)
            .append("timestamp", this.timestamp)
            .append("docId", this.docId)
            .append("type", this.type)
            .append("attempts", this.attempts)
            .append("stop", this.stopFlag)
            .append("wikiId", this.wikiId)
            .append("version", getVersion())
            .toString();
    }

    /**
     * @return return a continuation called when the task is finished
     */
    public CompletableFuture<TaskData> getFuture()
    {
        return this.future;
    }
}
