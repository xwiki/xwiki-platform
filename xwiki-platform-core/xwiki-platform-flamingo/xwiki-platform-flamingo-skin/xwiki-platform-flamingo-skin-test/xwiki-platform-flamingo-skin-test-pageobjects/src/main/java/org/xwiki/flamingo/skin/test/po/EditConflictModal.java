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
package org.xwiki.flamingo.skin.test.po;

import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.xwiki.test.ui.po.BaseModal;
import org.xwiki.test.ui.po.diff.EntityDiff;

/**
 * Represent the modal displayed in case of edit conflict.
 *
 * @since 11.4RC1
 * @version $Id$
 */
public class EditConflictModal extends BaseModal
{
    private static final String MODAL_ID = "previewDiffModal";

    @FindBy(id = "actionReloadRadio")
    private WebElement reloadChoice;

    @FindBy(id = "actionForceSaveRadio")
    private WebElement forceSaveChoice;

    @FindBy(id = "cancelDiffButton")
    private WebElement cancelButton;

    @FindBy(id = "submitDiffButton")
    private WebElement submitButton;

    @FindBy(className = "diff")
    private WebElement diffContainer;

    @FindBy(className = "badge")
    private WebElement version;

    /**
     * Default constructor, wait for the modal to be visible.
     */
    public EditConflictModal()
    {
        super(By.id(MODAL_ID));
        this.getDriver().waitUntilElementIsVisible(By.id(MODAL_ID));
    }

    /**
     * Cancel the modal and wait for it to be closed.
     */
    public void cancelModal()
    {
        this.cancelButton.click();
        try {
            this.waitForClosed();
        } catch (StaleElementReferenceException e) {
            // the JS remove the modal so the element might be stale
        }
    }

    /**
     * Choose the force save option and submit it. Wait for the modal to be closed.
     */
    public void forceSave()
    {
        this.forceSaveChoice.click();
        this.submitButton.click();
        try {
            this.waitForClosed();
        } catch (StaleElementReferenceException e) {
            // the JS remove the modal so the element might be stale
        }

    }

    /**
     * Choose the reload editor option and submit it. Wait for the modal to be closed.
     */
    public void reloadEditor()
    {
        this.reloadChoice.click();
        this.submitButton.click();
        try {
            this.waitForClosed();
        } catch (StaleElementReferenceException e) {
            // the JS remove the modal so the element might be stale
        }
    }

    @Override
    public void close()
    {
        try {
            super.close();
        } catch (StaleElementReferenceException e) {
            // the JS remove the modal so the element might be stale
        }
    }

    /**
     * @return the {@link EntityDiff} corresponding to the diff contained in the modal.
     */
    public EntityDiff getDiff()
    {
        return new EntityDiff(this.diffContainer);
    }

    /**
     * @return the version number used to create the diff.
     */
    public String getVersionDiff()
    {
        return this.version.getText();
    }
}
