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
package org.xwiki.index.tree.test.po;

import java.util.Arrays;
import java.util.List;

import org.openqa.selenium.WebElement;
import org.xwiki.model.internal.reference.DefaultStringEntityReferenceSerializer;
import org.xwiki.model.internal.reference.DefaultSymbolScheme;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
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
    protected DefaultStringEntityReferenceSerializer entityReferenceSerializer =
        new DefaultStringEntityReferenceSerializer(new DefaultSymbolScheme());

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

    @Override
    public DocumentTreeElement clearSelection()
    {
        return (DocumentTreeElement) super.clearSelection();
    }

    /**
     * Opens the tree to the specified space.
     * 
     * @param path the path used to locate the space
     * @return this tree
     */
    public DocumentTreeElement openToSpace(String... path)
    {
        return openTo(getSpaceNodeId(path));
    }

    /**
     * Opens the tree to the specified document.
     * 
     * @param path the path used to locate the document
     * @return this tree
     */
    public DocumentTreeElement openToDocument(String... path)
    {
        return openTo(getDocumentNodeId(path));
    }

    /**
     * Opens the tree to the specified attachment.
     * 
     * @param path the path used to locate the attachment
     * @return this tree
     * @see #openToAttachment(AttachmentReference)
     */
    public DocumentTreeElement openToAttachment(String... path)
    {
        return openTo(getAttachmentNodeId(path));
    }

    /**
     * Open the tree to the specific attachment.
     *
     * @param attachmentReference the attachment reference to open the tree to
     * @return this tree
     * @see #openToAttachment(String...)
     * @since 14.7RC1
     */
    public DocumentTreeElement openToAttachment(AttachmentReference attachmentReference)
    {
        return openTo(getNodeId(attachmentReference));
    }

    @Override
    public DocumentTreeElement waitForIt()
    {
        return (DocumentTreeElement) super.waitForIt();
    }

    /**
     * Waits for the specified document to be selected.
     * 
     * @param path the path used to locate the document to wait for
     * @return this tree
     * @since 7.2
     */
    public DocumentTreeElement waitForDocumentSelected(String... path)
    {
        return (DocumentTreeElement) super.waitForNodeSelected(getDocumentNodeId(path));
    }

    /**
     * @param path the path used to locate the space
     * @return {@code true} if the specified space appears in the tree
     */
    public boolean hasSpace(String... path)
    {
        return hasNode(getSpaceNodeId(path));
    }

    /**
     * @param path the path used to locate the document
     * @return {@code true} if the specified document appears in the tree
     */
    public boolean hasDocument(String... path)
    {
        return hasNode(getDocumentNodeId(path));
    }

    /**
     * @param path the path used to locate the attachment
     * @return {@code true} if the specified attachment is present in the tree
     */
    public boolean hasAttachment(String... path)
    {
        return hasNode(getAttachmentNodeId(path));
    }

    /**
     * @param path the path used to locate the space
     * @return the corresponding space node
     */
    public TreeNodeElement getSpaceNode(String... path)
    {
        return getNode(getSpaceNodeId(path));
    }

    public TreeNodeElement getNode(EntityReference entityReference)
    {
        return getNode(getNodeId(entityReference));
    }

    /**
     * @param path the path used to locate the document
     * @return the corresponding document node
     */
    public TreeNodeElement getDocumentNode(String... path)
    {
        return getNode(getDocumentNodeId(path));
    }

    /**
     * @param path the path used to locate the attachment
     * @return the corresponding attachment node
     * @see #getAttachmentNode(AttachmentReference)
     */
    public TreeNodeElement getAttachmentNode(String... path)
    {
        return getNode(getAttachmentNodeId(path));
    }

    /**
     * @param attachmentReference the attachment reference of the attachment node to get
     * @return the corresponding attachment node
     * @see #getAttachmentNode(String...)
     * @since 14.7RC1
     */
    public TreeNodeElement getAttachmentNode(AttachmentReference attachmentReference)
    {
        return getNode(getNodeId(attachmentReference));
    }

    protected String getNodeId(EntityReference reference)
    {
        return reference.getType().getLowerCase() + ":" + this.entityReferenceSerializer.serialize(reference);
    }

    private String getSpaceNodeId(String... path)
    {
        if (path.length > 0) {
            DocumentReference documentReference =
                new DocumentReference(getUtil().getCurrentWiki(), Arrays.asList(path), "WebHome");
            return getNodeId(documentReference.getParent());
        } else {
            throw new IllegalArgumentException("Incomplete path: it should have at least 1 element (space)");
        }
    }

    private String getDocumentNodeId(String... path)
    {
        if (path.length > 1) {
            List<String> pathElements = Arrays.asList(path);
            List<String> spaces = pathElements.subList(0, path.length - 1);
            String document = path[path.length - 1];
            return getNodeId(new DocumentReference(getUtil().getCurrentWiki(), spaces, document));
        } else {
            throw new IllegalArgumentException("Incomplete path: it should have at least 2 elements (space/page)");
        }
    }

    private String getAttachmentNodeId(String... path)
    {
        if (path.length > 2) {
            List<String> pathElements = Arrays.asList(path);
            List<String> spaces = pathElements.subList(0, path.length - 2);
            String document = path[path.length - 2];
            String fileName = path[path.length - 1];
            return getNodeId(
                new AttachmentReference(fileName, new DocumentReference(getUtil().getCurrentWiki(), spaces, document)));
        } else {
            throw new IllegalArgumentException("Incomplete path: it should have at least 3 elements (space/page/file)");
        }
    }
}
