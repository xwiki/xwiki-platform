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

import java.util.ArrayList;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.NotFoundException;
import org.openqa.selenium.StaleElementReferenceException;
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
public class CreatePagePage extends ViewPage
{
    @FindBy(name = "title")
    private WebElement titleTextField;

    @FindBy(name = "spaceReference")
    private WebElement spaceTextField;

    @FindBy(name = "name")
    private WebElement pageTextField;

    @FindBy(id = "terminal")
    private WebElement isTerminalCheckbox;

    @FindBy(css = ".location-picker .breadcrumb")
    private WebElement locationPreviewElement;

    private BreadcrumbElement locationPreview;

    public static CreatePagePage gotoPage()
    {
        getUtil().gotoPage("Main", "WebHome", "create");
        return new CreatePagePage();
    }

    public String getTitle()
    {
        return titleTextField.getAttribute("value");
    }

    public void setTitle(String title)
    {
        getDriver().setTextInputValue(this.titleTextField, title);
    }

    public String getSpace()
    {
        return spaceTextField.getAttribute("value");
    }

    public void setSpace(String space)
    {
        getDriver().setTextInputValue(this.spaceTextField, space);
    }

    public String getPage()
    {
        return pageTextField.getAttribute("value");
    }

    public void setPage(String page)
    {
        getDriver().setTextInputValue(this.pageTextField, page);
    }

    /**
     * @since 3.2M3
     */
    public int getAvailableTemplateSize()
    {
        // When there's no template available a hidden input with a blank value remains.
        return getDriver().findElements(By.name("templateprovider")).size() - 1;
    }

    public List<String> getAvailableTemplates()
    {
        List<String> availableTemplates = new ArrayList<String>();
        List<WebElement> templateInputs = getDriver().findElements(By.name("templateprovider"));
        for (WebElement input : templateInputs) {
            if (input.getAttribute("value").length() > 0) {
                availableTemplates.add(input.getAttribute("value"));
            }
        }

        return availableTemplates;
    }

    public void setTemplate(String template)
    {
        // Select the correct radio element corresponding to the passed template name.
        // TODO: For some reason the following isn't working. Find out why.
        // List<WebElement> templates = getDriver().findElements(
        // new ByChained(By.name("template"), By.tagName("input")));
        List<WebElement> templates = getDriver().findElements(By.name("templateprovider"));
        for (WebElement templateInput : templates) {
            if (templateInput.getAttribute("value").equals(template)) {
                templateInput.click();
                return;
            }
        }
        throw new RuntimeException("Failed to find template [" + template + "]");
    }

    public void clickCreate()
    {
        this.pageTextField.submit();
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
        createPageInternal(title, spaceValue, pageValue, isTerminalPage);
        clickCreate();
        return new EditPage();
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
        createPageInternal(title, spaceValue, pageValue, isTerminalPage);
        setTemplate(templateValue);
        clickCreate();
        return new EditPage();
    }

    private void createPageInternal(String title, String spaceValue, String pageValue, boolean isTerminalPage)
    {
        if (title != null) {
            setTitle(title);
        }

        if (spaceValue != null) {
            setSpace(spaceValue);
        }

        if (pageValue != null) {
            setPage(pageValue);
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
        getDriver().waitUntilElementIsVisible(By.className("errormessage"));
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
        if (isTerminalPage && !this.isTerminalCheckbox.isSelected()) {
            this.isTerminalCheckbox.click();
        } else if (!isTerminalPage && this.isTerminalCheckbox.isSelected()) {
            this.isTerminalCheckbox.click();
        }
    }

    /**
     * @return true if the choice between terminal or non-terminal document is displayed, false otherwise.
     * @since 7.2M3
     */
    public boolean isTerminalOptionDisplayed()
    {
        return getDriver().hasElementWithoutWaiting(By.id("terminal"));
    }

    /**
     * Wait for the Location Preview Breadcrumb to display the passed content and throws an exception if the timeout is
     * reached. Note that we need to wait since the Breadcrumb is udated live and asserting its content without waiting
     * would lead to false positives.
     *
     * @param expectedContent the content to wait for
     * @since 7.2RC1
     */
    public void waitForLocationPreviewContent(final String expectedContent)
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
                        String value = getLocationPreview().getPathAsString();
                        currentValue.setLength(0);
                        currentValue.append(currentValue);
                        return expectedContent.equals(value);
                    } catch (NotFoundException e) {
                        return false;
                    } catch (StaleElementReferenceException e) {
                        // The element was removed from DOM in the meantime
                        return false;
                    }
                }
            });
        } catch (WebDriverException e) {
            // Display a nicer error message than would be displayed otherwise
            throw new WebDriverException(
                String.format("Found [%s], was expecting [%s]", currentValue.toString(), expectedContent), e);
        }
    }

    public BreadcrumbElement getLocationPreview()
    {
        if (this.locationPreview == null) {
            this.locationPreview = new BreadcrumbElement(this.locationPreviewElement);
        }
        return this.locationPreview;
    }
}
