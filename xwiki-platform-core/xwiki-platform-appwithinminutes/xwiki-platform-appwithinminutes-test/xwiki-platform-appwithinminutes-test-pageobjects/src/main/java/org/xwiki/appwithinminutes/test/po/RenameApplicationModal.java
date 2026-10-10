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
package org.xwiki.appwithinminutes.test.po;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.xwiki.test.ui.po.BaseModal;
import org.xwiki.test.ui.po.DocumentPicker;

/**
 * Represents the modal used to rename (and move) an application, opened from the Rename action of the application
 * home page.
 *
 * @version $Id$
 * @since 18.9.0RC1
 */
public class RenameApplicationModal extends BaseModal
{
    private final DocumentPicker locationPicker;

    /**
     * Default constructor, waits for the modal to be displayed.
     */
    public RenameApplicationModal()
    {
        super(By.id("renameAppModal"));
        waitUntilDisplayed();
        this.locationPicker = new DocumentPicker(this.container.findElement(By.className("location-picker")));
    }

    /**
     * @param name the new application name
     * @return this modal
     */
    public RenameApplicationModal setName(String name)
    {
        this.locationPicker.setTitle(name);
        return this;
    }

    /**
     * @param parent the reference of the new parent page, e.g. {@code A.B}, or an empty string to move the application
     *     to the top of the page hierarchy
     * @return this modal
     */
    public RenameApplicationModal setParent(String parent)
    {
        this.locationPicker.setParent(parent);
        return this;
    }

    /**
     * @return the location picker of the modal
     */
    public DocumentPicker getLocationPicker()
    {
        return this.locationPicker;
    }

    /**
     * Clicks the Rename button (once the new location has been validated) and waits for the browser to be redirected
     * to the renamed application home page.
     *
     * @return the renamed application home page
     */
    public ApplicationHomePage clickRename()
    {
        WebElement renameButton = this.container.findElement(By.cssSelector(".modal-footer .btn-primary"));
        getDriver().waitUntilElementIsEnabled(renameButton);
        getDriver().addPageNotYetReloadedMarker();
        renameButton.click();
        // The application pages are renamed one after the other before the redirect, which can take a while.
        int timeout = getDriver().getTimeout();
        getDriver().setTimeout(Math.max(timeout, 60));
        try {
            getDriver().waitUntilPageIsReloaded();
        } finally {
            getDriver().setTimeout(timeout);
        }
        return new ApplicationHomePage();
    }
}
