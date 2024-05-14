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
package org.xwiki.index.tree.internal;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.query.QueryException;
import org.xwiki.tree.TreeNodeGroup;

/**
 * Base class for child documents tree node groups.
 *
 * @version $Id$
 * @since 16.4.0RC1
 */
public abstract class AbstractChildDocumentsTreeNodeGroup extends AbstractEntityTreeNode implements TreeNodeGroup
{
    /**
     * Creates a new instance.
     *
     * @param type the type of the tree node group
     */
    protected AbstractChildDocumentsTreeNodeGroup(String type)
    {
        super(type);
    }

    @Override
    public List<String> getChildren(String nodeId, int offset, int limit)
    {
        EntityReference parentReference = resolve(nodeId);
        // The parent entity must be either a wiki or a nested (non-terminal) document.
        if (parentReference != null && canHaveChildDocuments(parentReference)) {
            try {
                return serialize(getChildDocuments(parentReference, offset, limit));
            } catch (QueryException e) {
                this.logger.warn("Failed to retrieve the children of [{}]. Root cause [{}].", nodeId,
                    ExceptionUtils.getRootCauseMessage(e));
            }
        }
        return Collections.emptyList();
    }

    protected abstract List<DocumentReference> getChildDocuments(EntityReference parentReference, int offset, int limit)
        throws QueryException;

    @Override
    public int getChildCount(String nodeId)
    {
        EntityReference parentReference = resolve(nodeId);
        if (parentReference != null && canHaveChildDocuments(parentReference)) {
            try {
                return getChildDocumentsCount(parentReference);
            } catch (QueryException e) {
                this.logger.warn("Failed to count the children of [{}]. Root cause [{}].", nodeId,
                    ExceptionUtils.getRootCauseMessage(e));
            }
        }
        return 0;
    }

    protected abstract int getChildDocumentsCount(EntityReference parentReference) throws QueryException;

    protected abstract boolean canHaveChildDocuments(EntityReference parentReference);
}
