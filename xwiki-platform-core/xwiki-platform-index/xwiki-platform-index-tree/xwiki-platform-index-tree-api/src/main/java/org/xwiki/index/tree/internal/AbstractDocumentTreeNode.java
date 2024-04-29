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

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.tree.CompositeTreeNodeGroup;
import org.xwiki.tree.TreeNode;

/**
 * Base class for document tree nodes in various types of hierarchies.
 * 
 * @version $Id$
 * @since 16.4.0RC1
 */
public abstract class AbstractDocumentTreeNode extends AbstractDocumentRelatedTreeNode implements Initializable
{
    @Inject
    protected CompositeTreeNodeGroup childNodes;

    @Inject
    @Named("translations")
    private TreeNode translations;

    @Inject
    @Named("attachments")
    private TreeNode attachments;

    @Inject
    @Named("classProperties")
    private TreeNode classProperties;

    @Inject
    @Named("objects")
    private TreeNode objects;

    /**
     * Default constructor.
     */
    protected AbstractDocumentTreeNode()
    {
        super("document");
    }

    @Override
    public void initialize() throws InitializationException
    {
        this.childNodes.addTreeNode(this.translations, nodeId -> hasChild(this.translations, resolve(nodeId)));
        this.childNodes.addTreeNode(this.attachments, nodeId -> hasChild(this.attachments, resolve(nodeId)));
        this.childNodes.addTreeNode(this.classProperties, nodeId -> hasChild(this.classProperties, resolve(nodeId)));
        this.childNodes.addTreeNode(this.objects, nodeId -> hasChild(this.objects, resolve(nodeId)));
        this.childNodes.setIdGenerator(this::getPseudoChildNodeId);
    }

    @Override
    public int getChildCount(String nodeId)
    {
        return this.childNodes.getChildCount(nodeId);
    }

    @Override
    public List<String> getChildren(String nodeId, int offset, int limit)
    {
        return this.childNodes.getChildren(nodeId, offset, limit);
    }

    private boolean hasChild(TreeNode childNode, EntityReference parentReference)
    {
        return hasChild(childNode, this.defaultEntityReferenceSerializer.serialize(parentReference));
    }

    private boolean hasChild(TreeNode childNode, String serializedDocumentReference)
    {
        String showChild = "show" + StringUtils.capitalize(childNode.getType());
        if (Boolean.TRUE.equals(getProperties().get(showChild))) {
            String nodeId = childNode.getType() + ':' + serializedDocumentReference;
            childNode.getProperties().putAll(getProperties());
            return childNode.getChildCount(nodeId) > 0;
        }
        return false;
    }

    private String getPseudoChildNodeId(String parentNodeId, String pseudoNodeType)
    {
        EntityReference parentReference = resolve(parentNodeId);
        return pseudoNodeType + ':' + this.defaultEntityReferenceSerializer.serialize(parentReference);
    }
}
