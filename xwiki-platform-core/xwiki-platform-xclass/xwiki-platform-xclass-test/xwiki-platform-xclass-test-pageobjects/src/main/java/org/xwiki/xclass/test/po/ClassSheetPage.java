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
package org.xwiki.xclass.test.po;

import java.lang.Override;import java.lang.String;import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.xwiki.test.ui.po.InlinePage;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.test.ui.po.editor.ClassEditPage;

/**
 * Represents the sheet used to display information about a XWiki class.
 * 
 * @version $Id$
 * @since 3.4M1
 */
public class ClassSheetPage extends ViewPage
{
    /**
     * The link to view the class template.
     */
    @FindBy(partialLinkText = "View the template document")
    private WebElement templateLink;

    /**
     * The link to view the class sheet.
     */
    @FindBy(partialLinkText = "View the sheet document")
    private WebElement sheetLink;

    /**
     * The link to define the class. This is displayed only if the class doesn't have any properties.
     */
    @FindBy(xpath = "//*[contains(@class, 'warningmessage')]//a[. = 'class editor']")
    private WebElement defineClassLink;

    /**
     * The link to the class editor. This is displayed only if the class has properties.
     */
    @FindBy(linkText = "add or modify the class properties")
    private WebElement editClassLink;

    /**
     * The button used to create the class sheet.
     */
    @FindBy(xpath = "//input[@class = 'button' and @value = 'Create the document sheet']")
    private WebElement createSheetButton;

    /**
     * The link used to bind the class to its sheet.
     */
    @FindBy(xpath = "//*[contains(@class, 'warningmessage')]//a[. = 'Bind the sheet to the class \u00BB']")
    private WebElement bindSheetLink;

    /**
     * The button used to create the class template.
     */
    @FindBy(xpath = "//input[@class = 'button' and @value = 'Create the document template']")
    private WebElement createTemplateButton;

    /**
     * The link used to add an instance of the class to the template document.
     */
    @FindBy(xpath = "//*[contains(@class, 'warningmessage')]//a[contains(., ' object to the template \u00BB')]")
    private WebElement addObjectToTemplateLink;

    /**
     * The text input used to specify the space where to create a new document.
     */
    @FindBy(id = "spaceName")
    private WebElement spaceNameInput;

    /**
     * The text input used to specify the name of the document.
     */
    @FindBy(id = "docName")
    private WebElement documentNameInput;

    /**
     * The button used to create a new document based on the class template.
     */
    @FindBy(xpath = "//input[@class = 'button' and @value = 'Create this document']")
    private WebElement createDocumentButton;

    /**
     * Clicks on the template link and returns the template page
     * 
     * @return the page that represents the class template
     */
    public ViewPage clickTemplateLink()
    {
        templateLink.click();
        return new ViewPage();
    }

    /**
     * Clicks on the link to view the class sheet.
     * 
     * @return the page that represents the class sheet
     */
    public ViewPage clickSheetLink()
    {
        sheetLink.click();
        return new ViewPage();
    }

    /**
     * Clicks on the link to define the class. This link is visible only if the class has no properties yet.
     * 
     * @return the class editor
     */
    public ClassEditPage clickDefineClassLink()
    {
        defineClassLink.click();
        return new ClassEditPage();
    }

    /**
     * Clicks on the link to edit the class. This link is visible only if the class has properties.
     * 
     * @return the class editor
     */
    public ClassEditPage clickEditClassLink()
    {
        editClassLink.click();
        return new ClassEditPage();
    }

    /**
     * @param name the property name
     * @param prettyName the property pretty name
     * @param type the property type
     * @return {@code true} if the sheet lists the specified property, {@code false} otherwise
     */
    public boolean hasProperty(String name, String prettyName, String type)
    {
        // Pretty Name (Name: Type)
        String xpath = String.format("//li[. = '%s (%s: %s)']", prettyName, name, type);
        return getDriver().findElementsWithoutWaiting(By.xpath(xpath)).size() == 1;
    }

    /**
     * Clicks on the button to create a sheet for the class that is being displayed.
     * 
     * @return the current page, after it is reloaded
     */
    public ClassSheetPage clickCreateSheetButton()
    {
        createSheetButton.click();
        // Create a new instance because the page is reloaded.
        return new ClassSheetPage();
    }

    /**
     * Clicks on the link to bind the class to its sheet.
     * 
     * @return the current page, after it is reloaded
     */
    public ClassSheetPage clickBindSheetLink()
    {
        bindSheetLink.click();
        // Create a new instance because the page is reloaded.
        return new ClassSheetPage();
    }

    /**
     * Clicks on the button to create the class template.
     * 
     * @return the current page, after it is reloaded
     */
    public ClassSheetPage clickCreateTemplateButton()
    {
        createTemplateButton.click();
        return new ClassSheetPage();
    }

    /**
     * Clicks on the link to add an instance of the class to the template document.
     * 
     * @return the current page, after it is reloaded
     */
    public ClassSheetPage clickAddObjectToTemplateLink()
    {
        addObjectToTemplateLink.click();
        return new ClassSheetPage();
    }

    /**
     * @return the input used to specify the name of the space where to create the new document
     */
    public WebElement getSpaceNameInput()
    {
        return spaceNameInput;
    }

    /**
     * @return the input used to specify the name of the new document
     */
    public WebElement getDocumentNameInput()
    {
        return documentNameInput;
    }

    /**
     * Clicks the button to create a new document based on the class template.
     * 
     * @return the in-line edit mode for the new document
     */
    public InlinePage clickCreateDocumentButton()
    {
        createDocumentButton.click();
        return new InlinePage();
    }

    /**
     * Creates a new document with the specified name, in the specified space, based on the class template.
     * 
     * @param spaceName the name of the space where to create the new document
     * @param pageName the name of the new document
     * @return the in-line mode for the new document
     */
    public InlinePage createNewDocument(String spaceName, String pageName)
    {
        spaceNameInput.clear();
        spaceNameInput.sendKeys(spaceName);
        documentNameInput.clear();
        documentNameInput.sendKeys(pageName);
        return clickCreateDocumentButton();
    }

    /**
     * @param documentName the name of a document
     * @return {@code true} if the specified document is listed as having an object of the class being viewed,
     *         {@code false} otherwise
     */
    public boolean hasDocument(String documentName)
    {
        // Make sure we look inside the page content and not in some panel like My Recent Modifications.
        String xpath = String.format("//div[@id = 'xwikicontent']//li//a[. = '%s']", documentName);
        return getDriver().findElementsWithoutWaiting(By.xpath(xpath)).size() == 1;
    }

    @Override
    public ClassSheetPage waitUntilPageIsLoaded()
    {
        getDriver().waitUntilElementIsVisible(By.id("HTheclasstemplate"));
        return this;
    }
}
