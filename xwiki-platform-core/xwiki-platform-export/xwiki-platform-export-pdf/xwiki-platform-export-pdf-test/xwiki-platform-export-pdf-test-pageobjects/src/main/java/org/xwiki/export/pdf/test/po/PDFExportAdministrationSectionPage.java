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
package org.xwiki.export.pdf.test.po;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.xwiki.administration.test.po.AdministrationSectionPage;
import org.xwiki.test.ui.po.Select;
import org.xwiki.test.ui.po.SuggestInputElement;

/**
 * Represents the actions possible on the PDF export administration section.
 * 
 * @version $Id$
 * @since 14.9RC1
 */
public class PDFExportAdministrationSectionPage extends AdministrationSectionPage
{
    /**
     * The number of seconds to wait for the Chrome Docker container to be ready for use. We need a bigger timeout
     * because the PDF generator might have to fetch the headless Chrome Docker image, create the container and start it
     * (on its first initialization, after a configuration change or after a restart).
     */
    public static final int CHROME_INIT_TIMEOUT = 60;

    private static final String SECTION_ID = "export.pdf";

    @FindBy(className = "pdfGeneratorStatus")
    private WebElement generatorStatus;

    @FindBy(id = "XWiki.PDFExport.ConfigurationClass_0_templates")
    private WebElement templatesSelect;

    @FindBy(id = "pdfGenerator")
    private WebElement generatorSelect;

    @FindBy(id = "XWiki.PDFExport.ConfigurationClass_0_chromeDockerContainerName")
    private WebElement chromeDockerContainerName;

    @FindBy(id = "XWiki.PDFExport.ConfigurationClass_0_dockerNetwork")
    private WebElement dockerNetworkInput;

    @FindBy(id = "XWiki.PDFExport.ConfigurationClass_0_xwikiURI")
    private WebElement xwikiURIInput;

    @FindBy(id = "XWiki.PDFExport.ConfigurationClass_0_chromeRemoteDebuggingPort")
    private WebElement chromeRemoteDebuggingPortInput;

    @FindBy(linkText = "Reset")
    private WebElement resetButton;

    /**
     * Open the PDF export administration section.
     * 
     * @return the PDF export administration section
     */
    public static PDFExportAdministrationSectionPage gotoPage()
    {
        AdministrationSectionPage.gotoPage(SECTION_ID);
        return new PDFExportAdministrationSectionPage();
    }

    /**
     * Default constructor.
     */
    public PDFExportAdministrationSectionPage()
    {
        super(SECTION_ID, true);
    }

    /**
     * @param wait whether to wait for the status to be updated
     * @return the status of the PDF generator
     */
    public String getGeneratorStatus(boolean wait)
    {
        if (wait) {
            getDriver().waitUntilCondition(
                ExpectedConditions.not(
                    ExpectedConditions.attributeContains(this.generatorStatus, "class", "pdfGeneratorStatus-checking")),
                CHROME_INIT_TIMEOUT);
        }
        return this.generatorStatus.getText();
    }

    /**
     * @return the input used to specify the list of PDF templates
     */
    public SuggestInputElement getTemplatesInput()
    {
        return new SuggestInputElement(this.templatesSelect);
    }

    /**
     * @return the select element used to specify the PDF generator
     */
    public Select getGeneratorSelect()
    {
        return new Select(this.generatorSelect);
    }

    /**
     * @return the name of the Chrome Docker container
     */
    public String getChromeDockerContainerName()
    {
        return this.chromeDockerContainerName.getAttribute("value");
    }

    /**
     * Sets the name of the Chrome Docker container.
     * 
     * @param value the new name for the Chrome Docker container
     */
    public void setChromeDockerContainerName(String value)
    {
        this.chromeDockerContainerName.clear();
        this.chromeDockerContainerName.sendKeys(value);
    }

    /**
     * @return whether the Chrome Docker container name is valid
     */
    public boolean isChromeDockerContainerNameValid()
    {
        return isValid(this.chromeDockerContainerName);
    }

    /**
     * @return the value of the Docker network configuration
     */
    public String getDockerNetwork()
    {
        return this.dockerNetworkInput.getAttribute("value");
    }

    /**
     * Set the value of the Docker network configuration.
     * 
     * @param value the new Docker network to use
     */
    public void setDockerNetwork(String value)
    {
        this.dockerNetworkInput.clear();
        this.dockerNetworkInput.sendKeys(value);
    }

    /**
     * @return whether the current Docker network value is valid
     */
    public boolean isDockerNetworkValid()
    {
        return isValid(this.dockerNetworkInput);
    }

    /**
     * @return the value of the XWiki URI configuration
     */
    public String getXWikiURI()
    {
        return this.xwikiURIInput.getAttribute("value");
    }

    /**
     * Sets the value of the XWiki URI configuration
     *
     * @param value the new XWiki URI value
     */
    public void setXWikiURI(String value)
    {
        this.xwikiURIInput.clear();
        this.xwikiURIInput.sendKeys(value);
    }

    /**
     * @return the port used to connect to the Chrome web browser
     */
    public String getChromeRemoteDebuggingPort()
    {
        return this.chromeRemoteDebuggingPortInput.getAttribute("value");
    }

    /**
     * Sets the port used to connect to the Chrome web browser.
     * 
     * @param value the new port value
     */
    public void setChromeRemoteDebuggingPort(String value)
    {
        this.chromeRemoteDebuggingPortInput.clear();
        this.chromeRemoteDebuggingPortInput.sendKeys(value);
    }

    /**
     * @return whether the specified port is valid not not
     */
    public boolean isChromeRemoteDebuggingPortValid()
    {
        return isValid(this.chromeRemoteDebuggingPortInput);
    }

    /**
     * Restore the default configuration.
     */
    public PDFExportAdministrationSectionPage reset()
    {
        getDriver().addPageNotYetReloadedMarker();
        this.resetButton.click();
        getDriver().switchTo().alert().accept();
        getDriver().waitUntilPageIsReloaded();
        return new PDFExportAdministrationSectionPage();
    }

    private boolean isValid(WebElement element)
    {
        return (Boolean) getDriver().executeScript("return arguments[0].validity.valid;", element);
    }
}
