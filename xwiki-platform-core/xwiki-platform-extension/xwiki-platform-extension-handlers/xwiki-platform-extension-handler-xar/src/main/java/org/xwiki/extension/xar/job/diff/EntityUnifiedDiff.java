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
package org.xwiki.extension.xar.job.diff;

import java.util.LinkedHashMap;
import java.util.List;

import org.xwiki.diff.Delta;
import org.xwiki.diff.display.UnifiedDiffBlock;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.stability.Unstable;

/**
 * Holds the differences, in unified format, between two versions of an entity.
 * 
 * @param <T> the entity type
 * @version $Id$
 * @since 7.0RC1
 */
@Unstable
public class EntityUnifiedDiff<T extends EntityReference> extends
    LinkedHashMap<String, List<UnifiedDiffBlock<String, Character>>>
{
    /**
     * Needed for serialization.
     */
    private static final long serialVersionUID = 1L;

    /**
     * The reference to the previous version of the entity.
     */
    private final T previousReference;

    /**
     * The reference to the next version of the entity.
     */
    private final T nextReference;

    /**
     * Creates a new instance to hold the differences between the specified entity versions.
     * 
     * @param previousReference the reference to the previous version of the entity
     * @param nextReference the reference to the next version of the entity
     */
    public EntityUnifiedDiff(T previousReference, T nextReference)
    {
        this.previousReference = previousReference;
        this.nextReference = nextReference;
    }

    /**
     * @return the reference to the previous version of the entity
     */
    public T getPreviousReference()
    {
        return this.previousReference;
    }

    /**
     * @return the reference to the previous version of the entity
     */
    public T getNextReference()
    {
        return this.nextReference;
    }

    /**
     * @return the reference to the entity whose versions are being compared
     */
    public T getReference()
    {
        return this.previousReference == null ? this.nextReference : this.previousReference;
    }

    /**
     * @return whether the entity has been added, deleted or modified
     */
    public Delta.Type getType()
    {
        if (this.previousReference == null) {
            return Delta.Type.INSERT;
        } else if (this.nextReference == null) {
            return Delta.Type.DELETE;
        } else {
            return Delta.Type.CHANGE;
        }
    }
}
