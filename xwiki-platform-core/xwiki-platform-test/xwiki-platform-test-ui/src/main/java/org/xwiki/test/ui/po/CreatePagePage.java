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

import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.pagefactory.ByChained;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.xwiki.test.ui.po.editor.EditPage;

/**
 * Represents the actions possible on the Create Page template page.
 *
 * @version $Id$
 * @since 3.2M3
 */
public class CreatePagePage extends BaseElement
{
    private static final By errorMessageLocator = By.className("errormessage");

    /**
     * The element that contains the document picker used to select the target document.
     */
    @FindBy(className = "location-picker")
    private WebElement documentPickerElement;

    private DocumentPicker documentPicker;

    private PageTypePicker pageTypePicker = new PageTypePicker();

    @FindBy(id = "terminal")
    private WebElement isTerminalCheckbox;

    @FindBy(css = "form#create input[type='submit'],form#create button[type='submit']")
    private WebElement createButton;

    public static CreatePagePage gotoPage()
    {
        getUtil().gotoPage("Main", "WebHome", "create");
        return new CreatePagePage();
    }

    /**
     * @return the document picker used to select the target document
     */
    public DocumentPicker getDocumentPicker()
    {
        if (this.documentPicker == null) {
            this.documentPicker = new DocumentPicker(this.documentPickerElement);
        }

        return this.documentPicker;
    }

    /**
     * @since 3.2M3
     */
    public int getAvailableTemplateSize()
    {
        return this.pageTypePicker.countAvailableTemplates();
    }

    public List<String> getAvailableTemplates()
    {
        return this.pageTypePicker.getAvailableTemplates();
    }

    public void setTemplate(String template)
    {
        this.pageTypePicker.selectTemplateByValue(template);
    }

    public void setType(String type)
    {
        this.pageTypePicker.selectByValue(type);
    }

    public void clickCreate()
    {
        clickCreate(true);
    }

    /**
     * Click on the create page button.
     * 
     * @param waitForSubmit whether to wait for the form to be submitted or not
     * @since 14.8RC1
     */
    public void clickCreate(boolean waitForSubmit)
    {
        // The form is submitted by JavaScript code if the submit button is clicked while the form has pending
        // asynchronous validations, and in such case Selenium doesn't wait for the form to be submitted.
        if (waitForSubmit) {
            getDriver().addPageNotYetReloadedMarker();
        }

        this.createButton.click();

        if (waitForSubmit) {
            getDriver().waitUntilPageIsReloaded();
        }
    }

    public EditPage createPage(String spaceValue, String pageValue)
    {
        return createPage(spaceValue, pageValue, false);
    }

    public EditPage createPage(String spaceValue, String pageValue, boolean isTerminalPage)
    {
        return createPage(null, spaceValue, pageValue, isTerminalPage);
    }

    /**
     * @since 7.2M3
     */
    public EditPage createPage(String title, String spaceValue, String pageValue, boolean isTerminalPage)
    {
        return createPageFromTemplate(title, spaceValue, pageValue, null, isTerminalPage);
    }

    public EditPage createPageFromTemplate(String spaceValue, String pageValue, String templateValue)
    {
        return createPageFromTemplate(null, spaceValue, pageValue, templateValue);
    }

    public EditPage createPageFromTemplate(String title, String spaceValue, String pageValue, String templateValue)
    {
        return createPageFromTemplate(title, spaceValue, pageValue, templateValue, false);
    }

    /**
     * @since 7.2M1
     */
    public EditPage createPageFromTemplate(String spaceValue, String pageValue, String templateValue,
        boolean isTerminalPage)
    {
        return createPageFromTemplate(null, spaceValue, pageValue, templateValue, isTerminalPage);
    }

    /**
     * @since 7.2M3
     */
    public EditPage createPageFromTemplate(String title, String spaceValue, String pageValue, String templateValue,
        boolean isTerminalPage)
    {
        fillForm(title, spaceValue, pageValue, isTerminalPage);
        if (templateValue != null) {
            setTemplate(templateValue);
        }
        clickCreate();
        return new EditPage();
    }

    /**
     * @param title document title, ignored if {@code null}
     * @param spaceReference document's space reference (parent nested document), ignored if {@code null}
     * @param pageName document's name (space name or page name, depending if terminal or not), ignored if {@code null}
     * @param isTerminalPage true if the new document is terminal, false for non-terminal
     * @since 7.4M2
     */
    public void fillForm(String title, String spaceReference, String pageName, boolean isTerminalPage)
    {
        if (title != null) {
            getDocumentPicker().setTitle(title);
            getDocumentPicker().waitForName(title);
        }

        if (spaceReference != null) {
            getDocumentPicker().setParent(spaceReference);
        }

        if (pageName != null) {
            getDocumentPicker().setName(pageName);
        }

        // Since the default is to not create terminal pages, only set this if the user is asking to create a terminal
        // page. This allows this API to work when using isTerminalPage = false even for simpler users which don't get
        // to see the Terminal option.
        if (isTerminalPage) {
            setTerminalPage(isTerminalPage);
        }
    }

    /**
     * Waits for a global error message in the page.
     *
     * @since 3.2M3
     */
    public void waitForErrorMessage()
    {
        getDriver().waitUntilElementIsVisible(errorMessageLocator);
    }

    /**
     * @return the content of the error message.
     * @since 11.4RC1
     */
    public String getErrorMessage()
    {
        return getDriver().findElement(errorMessageLocator).getText();
    }

    /**
     * Waits for a validation error in a field.
     *
     * @since 3.2M3
     */
    public void waitForFieldErrorMessage()
    {
        getDriver().waitUntilElementIsVisible(new ByChained(By.className("LV_invalid")));
    }

    /**
     * @return true if the page to create should be a terminal page, false otherwise
     * @since 7.2M1
     */
    public boolean isTerminalPage()
    {
        return this.isTerminalCheckbox.isSelected();
    }

    /**
     * @param isTerminalPage true if the page to create is terminal, false otherwise
     * @since 7.2M1
     */
    public void setTerminalPage(boolean isTerminalPage)
    {
        if (isTerminalPage != this.isTerminalCheckbox.isSelected()) {
            this.isTerminalCheckbox.click();
        }
    }

    /**
     * @return true if the choice between terminal or non-terminal document is displayed, false otherwise.
     * @since 7.2M3
     */
    public boolean isTerminalOptionDisplayed()
    {
        List<WebElement> elements = getDriver().findElementsWithoutWaiting(By.id("terminal"));
        return elements.size() > 0 && elements.get(0).isDisplayed();
    }

    /**
     * Wait for the location preview to display the passed path string and throw an exception if the timeout is reached.
     * Note that we need to wait since the Breadcrumb is updated live and asserting its content without waiting would
     * lead to false positives.
     * <p>
     * Note: This method can not be implemented inside {@link BreadcrumbElement} because a change of parent replaces
     * completely the {@link BreadcrumbElement}'s container and thus it becomes stale. To avoid that, at each wait
     * iteration, we lookup the current breadcrumb element and not a cached one.
     * <p>
     * TODO: Reuse {@link DocumentPicker} inside this PO instead of duplicating this method.
     *
     * @param expectedPathString the path string to wait for
     * @since 7.2RC1
     */
    public void waitForLocationPreviewContent(final String expectedPathString)
    {
        // TODO: Ugly hack. Would need to find a better solution
        final StringBuilder currentValue = new StringBuilder();

        try {
            getDriver().waitUntilCondition(new ExpectedCondition<Boolean>()
            {
                @Override
                public Boolean apply(WebDriver driver)
                {
                    try {
                        String value = getDocumentPicker().getLocation().getPathAsString();

                        currentValue.setLength(0);
                        currentValue.append(value);

                        return expectedPathString.equals(value);
                    } catch (Exception e) {
                        return false;
                    }
                }
            });
        } catch (WebDriverException e) {
            // Display a nicer error message than would be displayed otherwise
            throw new WebDriverException(
                String.format("Found [%s], was expecting [%s]", currentValue.toString(), expectedPathString), e);
        }
    }
}
