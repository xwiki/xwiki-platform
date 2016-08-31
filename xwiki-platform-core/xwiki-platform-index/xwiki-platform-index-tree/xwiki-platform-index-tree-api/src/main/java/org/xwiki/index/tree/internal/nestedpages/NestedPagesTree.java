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
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.tree.AbstractCompositeTree;
import org.xwiki.tree.Tree;
import org.xwiki.tree.TreeNode;

/**
 * The tree of nested pages.
 * 
 * @version $Id$
 * @since 8.3M2, 7.4.5
 */
@Component(roles = {Tree.class})
@Named("nestedPages")
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class NestedPagesTree extends AbstractCompositeTree implements Initializable
{
    @Inject
    @Named("farm")
    private TreeNode farmNode;

    @Inject
    @Named("wiki")
    private TreeNode wikiNode;

    @Inject
    @Named("document")
    private TreeNode documentNode;

    @Inject
    @Named("addDocument")
    private TreeNode addDocumentNode;

    @Inject
    @Named("translations")
    private TreeNode translationsNode;

    @Inject
    @Named("translation")
    private TreeNode translationNode;

    @Inject
    @Named("attachments")
    private TreeNode attachmentsNode;

    @Inject
    @Named("attachment")
    private TreeNode attachmentNode;

    @Inject
    @Named("addAttachment")
    private TreeNode addAttachmentNode;

    @Inject
    @Named("classProperties")
    private TreeNode classPropertiesNode;

    @Inject
    @Named("classProperty")
    private TreeNode classPropertyNode;

    @Inject
    @Named("objects")
    private TreeNode objectsNode;

    @Inject
    @Named("objectsOfType")
    private TreeNode objectsOfTypeNode;

    @Inject
    @Named("object")
    private TreeNode objectNode;

    @Inject
    @Named("objectProperty")
    private TreeNode objectPropertyNode;

    @Override
    public void initialize() throws InitializationException
    {
        this.treeNodeByNodeType.put("farm", this.farmNode);
        this.treeNodeByNodeType.put("wiki", this.wikiNode);
        this.treeNodeByNodeType.put("document", this.documentNode);
        this.treeNodeByNodeType.put("addDocument", this.addDocumentNode);
        this.treeNodeByNodeType.put("translations", this.translationsNode);
        this.treeNodeByNodeType.put("translation", this.translationNode);
        this.treeNodeByNodeType.put("attachments", this.attachmentsNode);
        this.treeNodeByNodeType.put("attachment", this.attachmentNode);
        this.treeNodeByNodeType.put("addAttachment", this.addAttachmentNode);
        this.treeNodeByNodeType.put("classProperties", this.classPropertiesNode);
        this.treeNodeByNodeType.put("classProperty", this.classPropertyNode);
        this.treeNodeByNodeType.put("objects", this.objectsNode);
        this.treeNodeByNodeType.put("objectsOfType", this.objectsOfTypeNode);
        this.treeNodeByNodeType.put("object", this.objectNode);
        this.treeNodeByNodeType.put("objectProperty", this.objectPropertyNode);
    }
}
