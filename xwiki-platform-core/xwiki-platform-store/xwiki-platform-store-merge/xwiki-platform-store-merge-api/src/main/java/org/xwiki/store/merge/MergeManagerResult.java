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
package org.xwiki.store.merge;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.xwiki.diff.Conflict;
import org.xwiki.logging.LogLevel;
import org.xwiki.logging.LogQueue;

/**
 * This represents the result of a merge operation: it contains both the result of the merge, the possible conflicts
 * that happened during the merge, and the logs.
 * <p>
 * The merge result and the conflict might have distinct types: for example, a merge performed on {@code String}
 * by splitting it by characters, will expect a {@code String} result type, but a {@code Character} conflict type.
 *
 * @param <R> the type of the merge result
 * @param <C> the type of the conflicts
 *
 * @version $Id$
 * @since 11.8RC1
 */
public class MergeManagerResult<R, C>
{
    private final List<Conflict<C>> conflicts;

    private R mergeResult;

    private LogQueue log;

    private boolean modified;

    /**
     * Default constructor.
     */
    public MergeManagerResult()
    {
        this.conflicts = new ArrayList<>();
        this.log = new LogQueue();
        this.modified = false;
    }

    /**
     * @return {@code true} if the merge result version is different than the current one.
     */
    public boolean isModified()
    {
        return this.modified;
    }

    /**
     * @param modified set {@code true} if the merge result is different than the current version.
     */
    public void setModified(boolean modified)
    {
        this.modified = modified;
    }

    /**
     * Add some conflicts returned by a {@link org.xwiki.diff.MergeResult} inside this result.
     * @param conflicts the conflicts to add
     */
    public void addConflicts(List<Conflict<C>> conflicts)
    {
        this.conflicts.addAll(conflicts);
    }

    /**
     * This methods returns all the conflicts that occurred during the merge, and that have been properly recorded.
     * Note that right now the merge mechanism doesn't record all the conflicts as a {@link Conflict} instance:
     * some of the conflicts are only recorded as an error log.
     * @return all the conflicts that occurred during this merge
     */
    public List<Conflict<C>> getConflicts()
    {
        return this.conflicts;
    }

    /**
     * Retrieve the total number of conflicts: both the conflicts recorded as {@link Conflict} and the conflicts
     * recorded only as error logs.
     * @return the total number of conflicts
     * @see #getConflicts()
     * @since 14.10.12
     * @since 15.5RC1
     */
    public int getConflictsNumber()
    {
        // Each conflicts recorded, is recorded with its own error log.
        return (this.log.getLogs(LogLevel.ERROR).size() - this.conflicts.size()) + this.conflicts.size();
    }

    /**
     * Set the result obtained during the merge.
     * @param result the result of the merge operation.
     */
    public void setMergeResult(R result)
    {
        this.mergeResult = result;
    }

    /**
     * @return the resulted object of the merge.
     */
    public R getMergeResult()
    {
        return this.mergeResult;
    }

    /**
     * Specify the log queue to be used: this method should mainly be used when wrapping a result.
     * @param logQueue the log queue to be used.
     * @since 14.10.7
     * @since 15.2RC1
     */
    public void setLog(LogQueue logQueue)
    {
        this.log = logQueue;
    }

    /**
     * @return the log associated to the merge
     */
    public LogQueue getLog()
    {
        return this.log;
    }

    /**
     * @return {@code true} if at least one conflict occured during the merge operation.
     */
    public boolean hasConflicts()
    {
        // TODO: only the list of conflicts should be considered here: the various merge operation should
        // create proper conflicts, and not log errors.
        return !(this.getConflicts().isEmpty() && this.log.getLogs(LogLevel.ERROR).isEmpty());
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

        MergeManagerResult<?, ?> that = (MergeManagerResult<?, ?>) o;

        return new EqualsBuilder()
            .append(modified, that.modified)
            .append(conflicts, that.conflicts)
            .append(mergeResult, that.mergeResult)
            // we cannot compare logs for equality, but we compare hasConflicts since it's using log error emptyness
            .append(this.hasConflicts(), that.hasConflicts())
            .isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(69, 33)
            .append(conflicts)
            .append(mergeResult)
            // we cannot compare logs for equality, but we compare hasConflicts since it's using log error emptyness
            .append(this.hasConflicts())
            .append(modified)
            .toHashCode();
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this)
            .append("conflicts", conflicts)
            .append("mergeResult", mergeResult)
            .append("log", log)
            .append("modified", modified)
            .toString();
    }
}
