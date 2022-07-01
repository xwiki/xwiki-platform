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

import org.openqa.selenium.By;
import org.xwiki.test.ui.po.ConfirmationModal;
import org.xwiki.test.ui.po.ViewPage;

/**
 * Represents the actions possible on the restore document confirmation modal. The modal is displayed on the Deleted
 * Pages tab from the Page Index when restoring a document after delete and recreation.
 *
 * @version $Id$
 * @since 14.4.2
 * @since 14.5
 */
public class RestoreDocumentConfirmationModal extends ConfirmationModal
{
    public RestoreDocumentConfirmationModal()
    {
        super(By.id("restoreDocumentConfirmModal"));
    }

    public ViewPage clickReplace()
    {
        getDriver().findElementWithoutWaiting(this.container,
            By.cssSelector(".modal-footer .btn-primary, .modal-footer .btn-danger")).click();
        // We need to wait for the deletion of the old page before the redirect to the restored one will occur.
        getDriver().waitUntilPageIsReloaded();
        return new ViewPage();
    }
}
