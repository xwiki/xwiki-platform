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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;

/**
 * Base class for tree nodes that are linked to an XWiki document (e.g. attachments, translations, objects).
 * 
 * @version $Id$
 * @since 16.4.0RC1
 */
public abstract class AbstractDocumentRelatedTreeNode extends AbstractEntityTreeNode
{
    /**
     * Creates a new node with the specified type.
     * 
     * @param nodeType the type of document related node
     */
    protected AbstractDocumentRelatedTreeNode(String nodeType)
    {
        super(nodeType);
    }

    @Override
    public List<String> getChildren(String nodeId, int offset, int limit)
    {
        EntityReference documentReference = resolve(nodeId);
        if (documentReference != null && documentReference.getType() == EntityType.DOCUMENT) {
            try {
                return getChildren(new DocumentReference(documentReference), offset, limit);
            } catch (Exception e) {
                this.logger.warn("Failed to retrieve the children of [{}]. Root cause [{}].", nodeId,
                    ExceptionUtils.getRootCauseMessage(e));
            }
        }
        return Collections.emptyList();
    }

    protected List<String> getChildren(DocumentReference documentReference, int offset, int limit) throws Exception
    {
        return Collections.emptyList();
    }

    @Override
    public int getChildCount(String nodeId)
    {
        EntityReference documentReference = resolve(nodeId);
        if (documentReference != null && documentReference.getType() == EntityType.DOCUMENT) {
            try {
                return getChildCount(new DocumentReference(documentReference));
            } catch (Exception e) {
                this.logger.warn("Failed to count the children of [{}]. Root cause [{}].", nodeId,
                    ExceptionUtils.getRootCauseMessage(e));
            }
        }
        return 0;
    }

    protected int getChildCount(DocumentReference documentReference) throws Exception
    {
        return 0;
    }

    @Override
    public String getParent(String nodeId)
    {
        EntityReference documentReference = resolve(nodeId);
        if (documentReference != null && documentReference.getType() == EntityType.DOCUMENT) {
            try {
                return serialize(getParent(new DocumentReference(documentReference)));
            } catch (Exception e) {
                this.logger.warn("Failed to retrieve the parent of [{}]. Root cause [{}].", nodeId,
                    ExceptionUtils.getRootCauseMessage(e));
            }
        }
        return null;
    }

    protected EntityReference getParent(DocumentReference documentReference) throws Exception
    {
        // Most pseudo-document nodes are children of a real document node with the same reference.
        return documentReference;
    }

    @Override
    protected EntityReference resolve(String nodeId)
    {
        String prefix = getType() + ':';
        if (StringUtils.startsWith(nodeId, prefix)) {
            return super.resolve("document:" + nodeId.substring(prefix.length()));
        }
        return null;
    }
}
