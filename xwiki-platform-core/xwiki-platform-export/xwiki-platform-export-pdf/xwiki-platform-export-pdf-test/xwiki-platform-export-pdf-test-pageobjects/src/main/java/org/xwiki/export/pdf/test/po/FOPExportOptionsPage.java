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

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.xwiki.test.ui.po.BasePage;

/**
 * Represents the actions possible on the page used to configure and trigger the old PDF export, based on Apache
 * Formatting Objects Processor (FOP).
 *
 * @version $Id$
 * @since 18.9.0RC1
 */
public class FOPExportOptionsPage extends BasePage
{
    private static final String SUBMITTED_QUERY_ATTRIBUTE = "data-test-submitted-query";

    // The new PDF export modal, which may be present (hidden) on any page, uses the same form id.
    @FindBy(css = "#mainContentArea #pdfExportOptions")
    private WebElement form;

    @FindBy(css = "#mainContentArea #pdfExportOptions .buttons input[type=submit]")
    private WebElement exportButton;

    @FindBy(css = "#mainContentArea #pdfcover")
    private WebElement coverCheckbox;

    @FindBy(css = "#mainContentArea #pdftoc")
    private WebElement tocCheckbox;

    @FindBy(css = "#mainContentArea #pdfheader")
    private WebElement headerCheckbox;

    @FindBy(css = "#mainContentArea #pdffooter")
    private WebElement footerCheckbox;

    @FindBy(css = "#mainContentArea #comments")
    private WebElement commentsCheckbox;

    @FindBy(css = "#mainContentArea #attachments")
    private WebElement attachmentsCheckbox;

    /**
     * Default constructor.
     */
    public FOPExportOptionsPage()
    {
        getDriver().waitUntilElementIsVisible(By.cssSelector("#mainContentArea #pdfExportOptions"));
    }

    /**
     * @param cover whether to generate the cover page
     * @return this page
     */
    public FOPExportOptionsPage setCover(boolean cover)
    {
        return setChecked(this.coverCheckbox, cover);
    }

    /**
     * @param toc whether to generate the table of contents
     * @return this page
     */
    public FOPExportOptionsPage setTableOfContents(boolean toc)
    {
        return setChecked(this.tocCheckbox, toc);
    }

    /**
     * @param header whether to generate the page header
     * @return this page
     */
    public FOPExportOptionsPage setHeader(boolean header)
    {
        return setChecked(this.headerCheckbox, header);
    }

    /**
     * @param footer whether to generate the page footer
     * @return this page
     */
    public FOPExportOptionsPage setFooter(boolean footer)
    {
        return setChecked(this.footerCheckbox, footer);
    }

    /**
     * @param comments whether to include the page comments
     * @return this page
     */
    public FOPExportOptionsPage setComments(boolean comments)
    {
        return setChecked(this.commentsCheckbox, comments);
    }

    /**
     * @param attachments whether to include the images attached to the page
     * @return this page
     */
    public FOPExportOptionsPage setAttachments(boolean attachments)
    {
        return setChecked(this.attachmentsCheckbox, attachments);
    }

    /**
     * Click on the export button and fetch the generated PDF document.
     * <p>
     * The export form is a plain HTML form whose response is the PDF document, which the browser would display or
     * download, leaving the test without access to it. So the submit is intercepted: the request the browser would
     * send is recorded, the browser stays on this page (so the export can be repeated with other options), and the
     * same request is then sent, as the given user, to fetch the PDF document.
     *
     * @param userName the user name used to access the generated PDF document
     * @param password the password used to access the generated PDF document
     * @return the generated PDF document
     * @throws IOException if the PDF export fails
     */
    public PDFDocument export(String userName, String password) throws IOException
    {
        getDriver().executeScript("const form = arguments[0], attribute = arguments[1];"
            + "form.removeAttribute(attribute);"
            + "form.addEventListener('submit', event => {"
            + "  event.preventDefault();"
            + "  form.setAttribute(attribute, new URLSearchParams(new FormData(form)).toString());"
            + "}, {once: true});", this.form, SUBMITTED_QUERY_ATTRIBUTE);
        this.exportButton.click();
        getDriver().waitUntilCondition(driver -> this.form.getDomAttribute(SUBMITTED_QUERY_ATTRIBUTE) != null);

        // The browser used for running the test might be on a different machine than the one running XWiki and the test
        // code itself so we can't always use the same URL as the browser to fetch the PDF file.
        URL pdfURL = new URL(new URL(getUtil().getCurrentExecutor().getHttpClientBaseURL(false)),
            this.form.getDomAttribute("action") + '?' + this.form.getDomAttribute(SUBMITTED_QUERY_ATTRIBUTE));
        return PDFDocument.post(pdfURL, userName, password);
    }

    private FOPExportOptionsPage setChecked(WebElement checkbox, boolean checked)
    {
        if (checkbox.isSelected() != checked) {
            checkbox.click();
        }
        return this;
    }
}
