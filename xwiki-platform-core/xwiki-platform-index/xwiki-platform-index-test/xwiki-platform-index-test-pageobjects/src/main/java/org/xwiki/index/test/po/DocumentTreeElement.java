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
package org.xwiki.index.test.po;

import org.openqa.selenium.WebElement;
import org.xwiki.model.internal.reference.DefaultStringEntityReferenceSerializer;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.tree.test.po.TreeElement;
import org.xwiki.tree.test.po.TreeNodeElement;

/**
 * Page object used to interact with the document tree.
 * 
 * @version $Id$
 * @since 6.3RC1
 */
public class DocumentTreeElement extends TreeElement
{
    private DefaultStringEntityReferenceSerializer entityReferenceSerializer =
        new DefaultStringEntityReferenceSerializer();

    /**
     * Creates a new instance that can be used to interact with the document tree represented by the given element.
     * 
     * @param element the element that represents the document tree
     */
    public DocumentTreeElement(WebElement element)
    {
        super(element);
    }

    @Override
    public DocumentTreeElement openTo(String nodeId)
    {
        return (DocumentTreeElement) super.openTo(nodeId);
    }

    /**
     * Opens the tree to the specified document.
     * 
     * @param space the space name
     * @param name the document name
     * @return this tree
     */
    public DocumentTreeElement openToDocument(String space, String name)
    {
        return openTo(getDocumentNodeId(space, name));
    }

    /**
     * Opens the tree to the specified attachment.
     * 
     * @param space the space name
     * @param document the document name
     * @param fileName the file name
     * @return this tree
     */
    public DocumentTreeElement openToAttachment(String space, String document, String fileName)
    {
        return openTo(getAttachmentNodeId(space, document, fileName));
    }

    @Override
    public DocumentTreeElement waitForIt()
    {
        return (DocumentTreeElement) super.waitForIt();
    }

    /**
     * @param name the space name
     * @return {@code true} if the specified space appears in the tree
     */
    public boolean hasSpace(String name)
    {
        return hasNode(getSpaceNodeId(name));
    }

    /**
     * @param space the space name
     * @param name the document name
     * @return {@code true} if the specified document appears in the tree
     */
    public boolean hasDocument(String space, String name)
    {
        return hasNode(getDocumentNodeId(space, name));
    }

    /**
     * @param space the space name
     * @param document the document name
     * @param fileName the file name
     * @return {@code true} if the specified attachment is present in the tree
     */
    public boolean hasAttachment(String space, String document, String fileName)
    {
        return hasNode(getAttachmentNodeId(space, document, fileName));
    }

    /**
     * @param name the space name
     * @return the corresponding space node
     */
    public TreeNodeElement getSpaceNode(String name)
    {
        return getNode(getSpaceNodeId(name));
    }

    /**
     * @param space the space name
     * @param name the document name
     * @return the corresponding document node
     */
    public TreeNodeElement getDocumentNode(String space, String name)
    {
        return getNode(getDocumentNodeId(space, name));
    }

    /**
     * @param space the space node
     * @param document the document name
     * @param fileName the file name
     * @return the corresponding attachment node
     */
    public TreeNodeElement getAttachmentNode(String space, String document, String fileName)
    {
        return getNode(getAttachmentNodeId(space, document, fileName));
    }

    private String getNodeId(EntityReference reference)
    {
        return reference.getType().getLowerCase() + ":" + this.entityReferenceSerializer.serialize(reference);
    }

    private String getSpaceNodeId(String name)
    {
        return getNodeId(new SpaceReference(name, new WikiReference("xwiki")));
    }

    private String getDocumentNodeId(String space, String name)
    {
        return getNodeId(new DocumentReference("xwiki", space, name));
    }

    private String getAttachmentNodeId(String space, String document, String fileName)
    {
        return getNodeId(new AttachmentReference(fileName, new DocumentReference("xwiki", space, document)));
    }
}
