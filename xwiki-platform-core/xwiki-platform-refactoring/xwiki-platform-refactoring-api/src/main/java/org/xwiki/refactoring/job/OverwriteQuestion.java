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
package org.xwiki.refactoring.job;

import org.xwiki.model.reference.EntityReference;
import org.xwiki.stability.Unstable;

/**
 * Question asked when an entity with the same name is found during a copy or move operation and we don't know whether
 * to overwrite or keep the existing entity.
 * 
 * @version $Id$
 * @since 7.2M1
 */
@Unstable
public class OverwriteQuestion
{
    /**
     * The entity being copied or moved.
     */
    private final EntityReference source;

    /**
     * An entity with the same name that exists at the destination.
     */
    private final EntityReference destination;

    /**
     * Whether to overwrite or not the destination entity with the one being copied or moved.
     */
    private boolean overwrite = true;

    /**
     * Whether this question will be asked again or not if another pair of entities with the same name is found.
     */
    private boolean askAgain = true;

    /**
     * Ask whether to overwrite or not the destination entity with the source entity.
     * 
     * @param source the entity being copied or moved
     * @param destination an entity with the same name that exists at the destination
     */
    public OverwriteQuestion(EntityReference source, EntityReference destination)
    {
        this.source = source;
        this.destination = destination;
    }

    /**
     * @return the entity that is being copied or moved
     */
    public EntityReference getSource()
    {
        return source;
    }

    /**
     * @return an entity with the same name that exists at the destination
     */
    public EntityReference getDestination()
    {
        return destination;
    }

    /**
     * @return {@code true} to overwrite the destination entity with the one being copied or moved, {@code false} to
     *         keep the destination entity
     */
    public boolean isOverwrite()
    {
        return overwrite;
    }

    /**
     * Sets whether to overwrite or not the destination entity with the one being copied or moved.
     * 
     * @param overwrite {@code true} to overwrite, {@code false} to keep
     */
    public void setOverwrite(boolean overwrite)
    {
        this.overwrite = overwrite;
    }

    /**
     * @return whether this question will be asked again or not if another pair of entities with the same name is found
     */
    public boolean isAskAgain()
    {
        return askAgain;
    }

    /**
     * Sets whether this question will be asked again or not if another pair of entities with the same name is found.
     * 
     * @param askAgain {@code true} to ask again, {@code false} to perform the same action for the following entities,
     *            during the current operation
     */
    public void setAskAgain(boolean askAgain)
    {
        this.askAgain = askAgain;
    }
}
