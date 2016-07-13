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
package org.xwiki.extension.test.po;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.Select;
import org.xwiki.test.ui.po.BaseElement;
import org.xwiki.test.ui.po.ChangesPane;

/**
 * The merge conflict resolution UI.
 * 
 * @version $Id$
 * @since 4.2M1
 */
public class MergeConflictPane extends BaseElement
{
    /**
     * The list box used to specify which version to keep.
     */
    @FindBy(name = "versionToKeep")
    private WebElement versionToKeepSelect;

    /**
     * The list box used to select the 'from' version for comparison.
     */
    @FindBy(name = "original")
    private WebElement fromVersionSelect;

    /**
     * The list box used to select the 'to' version for comparison.
     */
    @FindBy(name = "revised")
    private WebElement toVersionSelect;

    /**
     * The button used to compare the selected versions.
     */
    @FindBy(xpath = "//button[@name = 'extensionAction' and @value = 'diff']")
    private WebElement diffButton;

    /**
     * @return the list box used to specify which version to keep
     */
    public Select getVersionToKeepSelect()
    {
        // We use a custom implementation because the drop down list remains open some times when we select an option.
        return new org.xwiki.test.ui.po.Select(versionToKeepSelect);
    }

    /**
     * @return the list box used to select the 'from' version for comparison
     */
    public Select getFromVersionSelect()
    {
        return new Select(fromVersionSelect);
    }

    /**
     * @return the list box used to select the 'to' version for comparison
     */
    public Select getToVersionSelect()
    {
        return new Select(toVersionSelect);
    }

    /**
     * Clicks the button to show the changes between the selected versions.
     * 
     * @return the new merge conflict pane displaying the changes between the selected versions
     */
    public MergeConflictPane clickShowChanges()
    {
        diffButton.click();
        // Wait as long as the button remains disabled.
        getDriver().waitUntilElementIsVisible(
            By.xpath("//button[@name = 'extensionAction' and @value = 'diff' and not(@disabled)]"));
        return new MergeConflictPane();
    }

    /**
     * @return the changes between the selected versions
     */
    public ChangesPane getChanges()
    {
        return new ChangesPane();
    }
}
