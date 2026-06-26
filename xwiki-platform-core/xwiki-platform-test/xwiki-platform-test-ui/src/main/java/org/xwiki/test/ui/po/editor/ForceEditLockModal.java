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
package org.xwiki.test.ui.po.editor;

import org.openqa.selenium.By;
import org.xwiki.test.ui.po.ConfirmationModal;

/**
 * Represents the modal to force editing a document inline when there are warnings, like about edit locks but also
 * about other things like required rights or the document an extension document.
 *
 * @version $Id$
 * @since 17.10.9
 * @since 18.4.0RC1
 */
public class ForceEditLockModal extends ConfirmationModal
{
    /**
     * Default constructor.
     */
    public ForceEditLockModal()
    {
        super(By.className("force-edit-lock-modal"));
    }

    @Override
    public void clickOk()
    {
        getDriver().findElementWithoutWaiting(this.container, By.cssSelector(".modal-footer .btn-warning")).click();
        this.waitForClosed();
    }
}
