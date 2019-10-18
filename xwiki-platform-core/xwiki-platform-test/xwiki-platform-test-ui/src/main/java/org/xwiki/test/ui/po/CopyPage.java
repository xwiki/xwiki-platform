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
 * <p>
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

    @FindBy(css = "form#copy .breadcrumb")
    private WebElement sourceBreadcrumbElement;

    private BreadcrumbElement sourceBreadcrumb;

    /**
     * The element that contains the document picker used to select the target document.
     */
    @FindBy(className = "location-picker")
    private WebElement documentPickerElement;

    /**
     * The hidden input containing the space name of the source page.
     */
    @FindBy(xpath = "//input[@type='hidden' and @name = 'sourceSpaceName']")
    private WebElement sourceSpaceName;

    /**
     * The hidden input containing the source page name.
     */
    @FindBy(xpath = "//input[@type='hidden' and @name = 'sourcePageName']")
    private WebElement sourcePageName;

    /**
     * The copy button.
     */
    @FindBy(xpath = "//input[@class = 'button' and @value = 'Copy']")
    private WebElement copyButton;

    /**
     * @return the breadcrumb that specified the location of the source document
     * @since 7.2M3
     */
    public BreadcrumbElement getSourceLocation()
    {
        if (this.sourceBreadcrumb == null) {
            this.sourceBreadcrumb = new BreadcrumbElement(this.sourceBreadcrumbElement);
        }
        return this.sourceBreadcrumb;
    }

    /**
     * @return the name of the space where the source page should be.
     */
    public String getSourceSpaceName()
    {
        return this.sourceSpaceName.getAttribute(VALUE);
    }

    /**
     * @return the name of the source page.
     */
    public String getSourcePageName()
    {
        return this.sourcePageName.getAttribute(VALUE);
    }

    /**
     * @return the current name of the space where the page should be copied.
     */
    public String getTargetSpaceName()
    {
        return getDocumentPicker().getParent();
    }

    /**
     * Sets the name of the space where the page should be copied.
     *
     * @param targetSpaceName the name of the space where the page should be copied
     */
    public void setTargetSpaceName(String targetSpaceName)
    {
        getDocumentPicker().setParent(targetSpaceName);
    }

    /**
     * @return the current name of the target page.
     */
    public String getTargetPageName()
    {
        return getDocumentPicker().getName();
    }

    /**
     * Sets the name of the target page.
     *
     * @param targetPageName the name of the target page
     */
    public void setTargetPageName(String targetPageName)
    {
        getDocumentPicker().setName(targetPageName);
    }

    /**
     * Submit the copy page form.
     *
     * @return the confirmation page
     */
    public CopyOrRenameOrDeleteStatusPage clickCopyButton()
    {
        this.copyButton.submit();
        return new CopyOrRenameOrDeleteStatusPage();
    }

    /**
     * Submit the copy page form and expect to receive an overwrite warning.
     *
     * @return the overwrite warning page
     */
    public CopyOverwritePromptPage clickCopyButtonExpectingOverwritePrompt()
    {
        // The WebElement#submit method does not wait anymore for the page to load,
        // cf: https://github.com/SeleniumHQ/selenium/issues/7691
        getDriver().addPageNotYetReloadedMarker();
        this.copyButton.submit();
        getDriver().waitUntilPageIsReloaded();
        return new CopyOverwritePromptPage();
    }

    /**
     * @return the document picker used to select the target document
     */
    public DocumentPicker getDocumentPicker()
    {
        return new DocumentPicker(this.documentPickerElement);
    }
}
