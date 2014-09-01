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
package org.xwiki.test.ui.po;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * Represents the common actions possible on the Copy Page page.
 * 
 * @version $Id$
 * @since 3.2M3
 */
public class CopyPage extends ViewPage
{
    /**
     * The value attribute name.
     */
    private static final String VALUE = "value";

    /**
     * The hidden input containing the space name of the source page.
     */
    @FindBy(id = "sourceSpaceName")
    private WebElement sourceSpaceName;

    /**
     * The hidden input containing the source page name.
     */
    @FindBy(id = "sourcePageName")
    private WebElement sourcePageName;

    /**
     * The select box containing the list of available spaces.
     */
    @FindBy(id = "targetSpaceName")
    private WebElement targetSpaceName;

    /**
     * The text input field to enter the name of the target page.
     */
    @FindBy(id = "targetPageName")
    private WebElement targetPageName;

    /**
     * The copy button.
     */
    @FindBy(xpath = "//input[@class = 'button' and @value = 'Copy']")
    private WebElement copyButton;

    /**
     * @return the name of the space where the source page should be.
     */
    public String getSourceSpaceName() {
        return this.sourceSpaceName.getAttribute(VALUE);
    }

    /**
     * @return the name of the source page.
     */
    public String getSourcePageName() {
        return this.sourcePageName.getAttribute(VALUE);
    }

    /**
     * @return the current name of the space where the page should be copied.
     */
    public String getTargetSpaceName() {
        return this.targetSpaceName.getAttribute(VALUE);
    }

    /**
     * Sets the name of the space where the page should be copied.
     * 
     * @param targetSpaceName the name of the space where the page should be copied
     */
    public void setTargetSpaceName(String targetSpaceName)
    {
        this.targetSpaceName.clear();
        this.targetSpaceName.sendKeys(targetSpaceName);
    }

    /**
     * @return the current name of the target page.
     */
    public String getTargetPageName() {
        return this.targetPageName.getAttribute(VALUE);
    }

    /**
     * Sets the name of the target page.
     * 
     * @param targetPageName the name of the target page
     */
    public void setTargetPageName(String targetPageName)
    {
        this.targetPageName.clear();
        this.targetPageName.sendKeys(targetPageName);
    }

    /**
     * Submit the copy page form.
     * 
     * @return the confirmation page
     */
    public CopyConfirmationPage clickCopyButton()
    {
        this.copyButton.submit();
        return new CopyConfirmationPage();
    }

    /**
     * Submit the copy page form and expect to receive an overwrite warning.
     *
     * @return the overwrite warning page
     */
    public CopyOverwritePromptPage clickCopyButtonExpectingOverwritePrompt()
    {
        this.copyButton.submit();
        return new CopyOverwritePromptPage();
    }
}
