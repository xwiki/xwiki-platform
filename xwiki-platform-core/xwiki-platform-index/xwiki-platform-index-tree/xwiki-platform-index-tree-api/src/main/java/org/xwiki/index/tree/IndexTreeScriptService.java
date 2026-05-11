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
package org.xwiki.index.tree;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.Strings;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.index.tree.internal.nestedpages.pinned.PinnedChildPagesManager;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.properties.converter.Converter;
import org.xwiki.script.service.ScriptService;
import org.xwiki.stability.Unstable;

/**
 * Script service for index tree operations.
 *
 * @version $Id$
 * @since 17.2.0RC1
 * @since 16.10.5
 * @since 16.4.7
 */
@Unstable
@Component
@Singleton
@Named("index.tree")
public class IndexTreeScriptService implements ScriptService
{
    private static final String DOCUMENT_NODE_ID_PREFIX = EntityType.DOCUMENT.name().toLowerCase() + ":";

    /**
     * The pseudo nodes that are used to group certain types of document children in the tree. They are not mapped to
     * real entities in the document hierarchy. Their node IDs use the reference of their parent document node: e.g.
     * "attachments:wiki:Some.Page".
     */
    private static final List<String> DOCUMENT_PSEUDO_NODE_TYPES =
        List.of("translations:", "attachments:", "classProperties:", "objects:", "addDocument:", "addAttachment:");

    @Inject
    private Logger logger;

    @Inject
    private PinnedChildPagesManager pinnedChildPagesManager;

    @Inject
    @Named("entityTreeNodeId")
    private Converter<EntityReference> entityTreeNodeIdConverter;

    /**
     * Retrieve the list of pinned child pages of the given parent.
     * 
     * @param parent the document for which to find pinned child pages.
     * @return the ordered list of pinned child pages.
     */
    public List<DocumentReference> getPinnedChildPages(DocumentReference parent)
    {
        return this.pinnedChildPagesManager.getPinnedChildPages(parent);
    }

    /**
     * Normalize the given entity tree node id by converting relative entity references to absolute references,
     * resolving them against the current entity reference.
     * 
     * @param nodeId the entity tree node id to normalize
     * @return the normalized node id, or the original node id if it cannot be normalized (i.e. if it doesn't match the
     *         expected format for an entity tree node id)
     * @since 18.4.0RC1
     * @since 17.10.9
     */
    @Unstable
    public String normalizeEntityTreeNodeId(String nodeId)
    {
        String docPseudoNodeType = DOCUMENT_PSEUDO_NODE_TYPES.stream()
            .filter(type -> Strings.CI.startsWith(nodeId, type)).findFirst().orElse(null);
        if (docPseudoNodeType != null) {
            String documentNodeId = DOCUMENT_NODE_ID_PREFIX + nodeId.substring(docPseudoNodeType.length());
            EntityReference documentReference =
                this.entityTreeNodeIdConverter.convert(EntityReference.class, documentNodeId);
            if (documentReference != null) {
                String normalizedDocumentNodeId =
                    this.entityTreeNodeIdConverter.convert(String.class, documentReference);
                return docPseudoNodeType + normalizedDocumentNodeId.substring(DOCUMENT_NODE_ID_PREFIX.length());
            }
        } else {
            EntityReference entityReference = this.entityTreeNodeIdConverter.convert(EntityReference.class, nodeId);
            if (entityReference != null) {
                return this.entityTreeNodeIdConverter.convert(String.class, entityReference);
            }
        }

        this.logger.warn("Failed to normalize the given entity tree node id [{}].", nodeId);
        return nodeId;
    }
}
