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
package org.xwiki.administration.test.po;

import org.openqa.selenium.By;
import org.xwiki.test.ui.po.BaseModal;
import org.xwiki.user.test.po.ProfileEditPage;

/**
 * Represents the user edit modal in the Users section of the administration.
 *
 * @version $Id$
 * @since 17.10.9
 * @since 18.4.0RC1
 */
public class EditUserModal extends BaseModal
{
    private static final class ModalContent extends ProfileEditPage
    {
        @Override
        public void waitUntilPageIsReady()
        {
            // There's no need to wait for any JavaScript here because the group edit form is loaded with AJAX inside
            // the modal body, after the page has been loaded.
        }
    }

    private final ModalContent content = new ModalContent();

    /**
     * Default constructor.
     */
    public EditUserModal()
    {
        super(By.id("editUserModal"));
    }

    /**
     * Wait until the edit form is loaded inside the modal.
     *
     * @return this modal
     */
    public EditUserModal waitUntilReady()
    {
        getDriver().waitUntilElementIsVisible(By.cssSelector("#editUserModal form#edituser"));
        return this;
    }

    /**
     * Wait until an edit confirmation warning is displayed.
     *
     * @return this modal
     */
    public EditUserModal waitUntilEditConfirmationWarningIsDisplayed()
    {
        getDriver().waitUntilElementIsVisible(By.cssSelector("#editUserModal .forceLock"));
        return this;
    }

    /**
     * @return the first name of the user
     */
    public String getFirstName()
    {
        return this.content.getUserFirstName();
    }

    /**
     * Set the first name.
     *
     * @param firstName the first name to set
     * @return this modal
     */
    public EditUserModal setFirstName(String firstName)
    {
        this.content.setUserFirstName(firstName);
        return this;
    }

    /**
     * @return the last name of the user
     */
    public String getLastName()
    {
        return this.content.getUserLastName();
    }

    /**
     * Set the last name.
     *
     * @param lastName the last name to set
     * @return this modal
     */
    public EditUserModal setLastName(String lastName)
    {
        this.content.setUserLastName(lastName);
        return this;
    }

    /**
     * Click the Save button. The modal will be closed and the live data table will be refreshed.
     */
    public void clickSave()
    {
        this.container.findElement(By.cssSelector(".modal-footer .btn-primary")).click();
    }

    /**
     * Click the Cancel button. The modal will be closed.
     */
    public void clickCancel()
    {
        this.container.findElement(By.cssSelector(".modal-footer .btn-default")).click();
    }

    /**
     * Force the edit confirmation warning.
     *
     * @param wait whether to wait for the edit form to be loaded
     * @return this modal
     */
    public EditUserModal forceEditConfirmationWarning(boolean wait)
    {
        this.container.findElement(By.cssSelector(".forceLock")).click();
        if (wait) {
            waitUntilReady();
        }
        return this;
    }
}
