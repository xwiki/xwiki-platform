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

import java.io.IOException;
import java.net.URL;
import java.time.Duration;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.xwiki.test.ui.po.BaseModal;
import org.xwiki.test.ui.po.Select;

/**
 * Represents the actions possible on the modal used to configure and trigger the PDF export.
 * 
 * @version $Id$
 * @since 14.4.2
 * @since 14.5
 */
public class PDFExportOptionsModal extends BaseModal
{
    private By exportButtonLocator = By.cssSelector("#pdfExportOptions .modal-footer input[type=submit]");

    @FindBy(id = "pdfTemplate")
    private WebElement templateSelect;

    @FindBy(id = "pdfcover")
    private WebElement coverCheckbox;

    @FindBy(id = "pdftoc")
    private WebElement tocCheckbox;

    @FindBy(id = "pdfheader")
    private WebElement headerCheckbox;

    @FindBy(id = "pdffooter")
    private WebElement footerCheckbox;

    /**
     * Default constructor.
     */
    public PDFExportOptionsModal()
    {
        super(By.id("pdfExportOptions"));

        getDriver().waitUntilCondition(ExpectedConditions.elementToBeClickable(this.exportButtonLocator));
        getDriver().waitUntilCondition(
            ExpectedConditions.numberOfElementsToBe(By.cssSelector("#pdfExportOptions .modal-body.loading"), 0));
    }

    /**
     * @return the select used to specify the PDF template
     */
    public Select getTemplateSelect()
    {
        return new Select(this.templateSelect);
    }

    /**
     * @return the checkbox used to control whether the PDF cover page is generated or not
     */
    public WebElement getCoverCheckbox()
    {
        return this.coverCheckbox;
    }

    /**
     * @return the checkbox used to control whether the PDF Table of Contents page is generated or not
     */
    public WebElement getTocCheckbox()
    {
        return this.tocCheckbox;
    }

    /**
     * @return the checkbox used to control whether the PDF header is generated or not
     */
    public WebElement getHeaderCheckbox()
    {
        return this.headerCheckbox;
    }

    /**
     * @return the checkbox used to control whether the PDF footer is generated or not
     */
    public WebElement getFooterCheckbox()
    {
        return this.footerCheckbox;
    }

    /**
     * Click on the export button and wait for the PDF document to be generated.
     * 
     * @param hostURL the URL that can be used to access the XWiki instance from the test code; this may be different
     *            than the URL used by the browser (e.g. in case the browser is running inside a Docker container)
     * @return the generated PDF document
     * @throws IOException if the PDF export fails
     */
    public PDFDocument export(URL hostURL) throws IOException
    {
        // The user is redirected to the generated PDF when ready, so we need to detect when the current URL changes.
        String currentURL = getDriver().getCurrentUrl();

        getDriver().findElement(this.exportButtonLocator).click();

        // Use a bigger timeout (60s) because the PDF export might have to fetch the headless Chrome Docker image,
        // create the container and start it (if it's the first PDF export).
        // Note that the timeout was previously 30s which was too little for the CI.
        new WebDriverWait(getDriver(), Duration.ofSeconds(60))
            .until((ExpectedCondition<Boolean>) driver -> !currentURL.equals(driver.getCurrentUrl()));

        // The browser used for running the test might be on a different machine than the one running XWiki and the test
        // code itself so we can't always use the same URL as the browser to download the PDF file.
        URL pdfURL = new URL(hostURL, new URL(getDriver().getCurrentUrl()).getPath());
        return new PDFDocument(pdfURL);
    }
}
