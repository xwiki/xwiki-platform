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
package org.xwiki.ckeditor.test.po.image.select;

import org.openqa.selenium.By;
import org.xwiki.index.tree.test.po.DocumentTreeElement;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.test.ui.po.BaseElement;
import org.xwiki.tree.test.po.TreeNodeElement;

/**
 * Page object for the document tree selection form of the image dialog.
 *
 * @version $Id$
 * @since 15.2RC1
 * @since 14.10.7
 */
public class ImageDialogTreeSelectForm extends BaseElement
{
    /**
     * Find and select the given attachment in the document tree.
     *
     * @param attachmentReference the attachment reference of the attachment to select
     */
    public void selectAttachment(AttachmentReference attachmentReference)
    {
        DocumentTreeElement documentTreeElement =
            new DocumentTreeElement(getDriver().findElement(By.cssSelector(".attachments-tree")));
        documentTreeElement.waitForIt();
        documentTreeElement.openToAttachment(attachmentReference);
        TreeNodeElement attachmentNode = documentTreeElement.getAttachmentNode(attachmentReference);
        documentTreeElement.clearSelection();
        documentTreeElement.selectNodes(attachmentNode);
        attachmentNode.select();
    }
}
