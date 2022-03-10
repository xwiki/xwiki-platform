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
import org.openqa.selenium.support.FindBy;
import org.xwiki.test.ui.po.InlinePage;

/**
 * Represents some custom actions available on application pages in inline edit mode, since the default save and view
 * and save and continue buttons are overridden in AWM.
 *
 * @version $Id$
 * @since 7.4M2
 */
public class ApplicationEditPage extends InlinePage
{
    @FindBy(id = "wizard-next")
    protected WebElement nextStepButton;

    @FindBy(xpath = "//a[. = 'Previous Step']")
    protected WebElement previousStepButton;

    /**
     * The form used to edit the application class overwrites the save button because it needs to process the submitted
     * data. Otherwise the request is forwarded by the action filter to the save action.
     */
    @FindBy(name = "xaction_save")
    private WebElement saveButton;

    /**
     * @see #saveButton
     */
    @FindBy(name = "xaction_saveandcontinue")
    private WebElement saveAndContinueButton;

    /**
     * Default constructor which doesn't wait.
     *
     * @deprecated since 12.8
     */
    @Deprecated
    public ApplicationEditPage()
    {
        // By default we don't wait for backward compatibility reason.
        this(false, false);
    }

    /**
     * Constructor that allows to wait or not on next step button.
     *
     * @param wait if {@code true} we wait for an element to be visible.
     * @param waitOnXAction if {@code true} the wait is done on a button named {@code xaction_save}, else it's done on
     *                      the id {@code wizard-next}.
     * @since 12.8
     * @since 12.6.3
     */
    public ApplicationEditPage(boolean wait, boolean waitOnXAction)
    {
        super();

        if (wait) {
            By findBy = (waitOnXAction) ? By.name("xaction_save") : By.id("wizard-next");
            getDriver().waitUntilElementIsVisible(findBy);
        }
    }

    @Override
    public WebElement getSaveAndViewButton()
    {
        return saveButton;
    }

    @Override
    public WebElement getSaveAndContinueButton()
    {
        return saveAndContinueButton;
    }
}
