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

import org.xwiki.diff.Conflict;
import org.xwiki.logging.LogQueue;
import org.xwiki.stability.Unstable;

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
@Unstable
public class MergeManagerResult<R, C>
{
    private final List<Conflict<C>> conflicts;

    private R mergeResult;

    private final LogQueue log;

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
     * @return all the conflicts occured during this merge
     */
    public List<Conflict<C>> getConflicts()
    {
        return this.conflicts;
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
        return !this.getConflicts().isEmpty();
    }
}
