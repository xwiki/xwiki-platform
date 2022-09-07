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
import org.openqa.selenium.WebElement;
import org.xwiki.index.tree.test.po.DocumentTreeElement;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.test.ui.po.BaseElement;
import org.xwiki.tree.test.po.TreeNodeElement;

/**
 * Page Object for the image selection modal.
 *
 * @version $Id$
 * @since 14.7RC1
 */
public class ImageDialogSelectModal extends BaseElement
{
    /**
     * Wait until the modal is loaded.
     *
     * @return the current page object
     */
    public ImageDialogSelectModal waitUntilReady()
    {
        getDriver().waitUntilElementIsVisible(By.className("image-selector-modal"));
        return this;
    }

    /**
     * Find and select the given attachment in the document tree.
     *
     * @param attachmentReference the attachment reference of the attachment to select
     */
    public void selectAttachment(AttachmentReference attachmentReference)
    {
        DocumentTreeElement documentTreeElement =
            new DocumentTreeElement(getDriver().findElement(By.cssSelector(".attachments-tree")));
        documentTreeElement.openToAttachment(attachmentReference);
        TreeNodeElement attachmentNode = documentTreeElement.getAttachmentNode(attachmentReference);
        documentTreeElement.clearSelection();
        documentTreeElement.selectNodes(attachmentNode);
        attachmentNode.select();
    }

    /**
     * Click on the select button to select the image and move to the image edition/configuration modal.
     *
     * @return the Page Object instance for the next modal
     */
    public ImageDialogEditModal clickSelect()
    {
        WebElement element = getDriver().findElement(By.cssSelector(".image-selector-modal .btn-primary"));
        getDriver().waitUntilElementIsEnabled(element);
        element.click();
        return new ImageDialogEditModal().waitUntilReady();
    }
}
