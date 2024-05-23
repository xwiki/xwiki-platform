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
package org.xwiki.index.tree.internal.nestedpages;

import javax.inject.Inject;
import javax.inject.Named;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.index.tree.internal.AbstractDocumentTreeNode;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.tree.TreeNode;

/**
 * The document tree node.
 * 
 * @version $Id$
 * @since 8.3M2
 * @since 7.4.5
 */
@Component
@Named("document")
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class DocumentTreeNode extends AbstractDocumentTreeNode
{
    @Inject
    private ContextualAuthorizationManager authorization;

    @Inject
    @Named("addDocument")
    private TreeNode addDocument;

    @Inject
    @Named("pinnedChildPages")
    private TreeNode pinnedChildPages;

    @Inject
    @Named("childDocuments")
    private TreeNode childDocuments;

    @Override
    public void initialize() throws InitializationException
    {
        super.initialize();
        this.childNodes.addTreeNode(this.addDocument, nodeId -> showAddDocument(resolve(nodeId)));
        this.childNodes.addTreeNode(this.pinnedChildPages, nodeId -> canHaveChildDocuments(resolve(nodeId)));
        this.childNodes.addTreeNode(this.childDocuments, nodeId -> canHaveChildDocuments(resolve(nodeId)));
    }

    @Override
    protected EntityReference getParent(DocumentReference documentReference) throws Exception
    {
        if (getDefaultDocumentName().equals(documentReference.getName())) {
            EntityReference parentReference = documentReference.getParent().getParent();
            if (parentReference.getType() == EntityType.SPACE) {
                return new DocumentReference(getDefaultDocumentName(), new SpaceReference(parentReference));
            } else {
                return parentReference;
            }
        } else {
            return new DocumentReference(getDefaultDocumentName(), documentReference.getLastSpaceReference());
        }
    }

    private boolean canHaveChildDocuments(EntityReference parentReference)
    {
        return parentReference != null && parentReference.getType() == EntityType.DOCUMENT
            && getDefaultDocumentName().equals(parentReference.getName());
    }

    private boolean showAddDocument(EntityReference parentReference)
    {
        return Boolean.TRUE.equals(getProperties().get("showAddDocument"))
            && "reference".equals(getProperties().get("hierarchyMode")) && canHaveChildDocuments(parentReference)
            && this.authorization.hasAccess(Right.EDIT, parentReference.getParent());
    }
}
