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

package org.xwiki.ckeditor.test.po;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.xwiki.index.tree.test.po.DocumentTreeElement;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.SpaceReference;

/**
 * Represents an element in the tree modal for selecting links.
 *
 * @version $Id$
 * @since 16.8.0RC1
 */
public class LinkTreeElement extends DocumentTreeElement
{
    /**
     * Creates a new instance that can be used to interact with the document tree represented by the given element.
     *
     * @param element the element that represents the document tree
     */
    public LinkTreeElement(WebElement element)
    {
        super(element);
    }

    private String getCreateDocumentNodeId(EntityReference reference)
    {
        return "addDocument:" + this.entityReferenceSerializer.serialize(reference);
    }

    /**
     * Check if there is a node to create a new page reference.
     * @param reference the parent reference of the creation node
     * @return {@code true} if there's a creation node.
     */
    public boolean hasNewPageCreation(EntityReference reference)
    {
        return this.hasNode(getCreateDocumentNodeId(reference));
    }

    /**
     * Create a new page node under the given origin, using the given name, and then open the created node.
     *
     * @param origin the parent node where to locate the new node
     * @param name the name of the node
     */
    public void createNode(DocumentReference origin, String name)
    {
        String createDocumentNodeId = getCreateDocumentNodeId(origin);
        WebElement originNode = getDriver().findElement(By.id(createDocumentNodeId));
        originNode.findElement(By.tagName("a")).click();
        // We cannot reuse the element
        getDriver().findElement(By.id(createDocumentNodeId))
            .findElement(By.tagName("input"))
            .sendKeys(name, Keys.ENTER);
        SpaceReference targetSpace = new SpaceReference(name, origin.getLastSpaceReference());
        DocumentReference target = new DocumentReference("WebHome", targetSpace);
        getDriver().waitUntilCondition(
            ExpectedConditions.presenceOfElementLocated(By.id(getNodeId(target)))
        );
        openTo(getNodeId(target));
    }
}
