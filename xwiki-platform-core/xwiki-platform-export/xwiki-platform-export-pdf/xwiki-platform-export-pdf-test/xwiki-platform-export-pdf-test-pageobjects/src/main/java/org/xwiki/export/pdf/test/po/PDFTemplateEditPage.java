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
import org.xwiki.test.ui.po.InlinePage;

/**
 * Represents the actions possible when editing a PDF template.
 * 
 * @version $Id$
 * @since 14.9RC1
 */
public class PDFTemplateEditPage extends InlinePage
{
    @FindBy(id = "XWiki.PDFExport.TemplateClass_0_cover")
    private WebElement coverTextArea;

    @FindBy(id = "XWiki.PDFExport.TemplateClass_0_toc")
    private WebElement tocTextArea;

    @FindBy(id = "XWiki.PDFExport.TemplateClass_0_header")
    private WebElement headerTextArea;

    @FindBy(id = "XWiki.PDFExport.TemplateClass_0_footer")
    private WebElement footerTextArea;

    /**
     * @return the code that controls the PDF cover
     */
    public String getCover()
    {
        return this.coverTextArea.getAttribute("value");
    }

    /**
     * Sets the code that controls the PDF cover
     * 
     * @param value the new code for the PDF cover
     */
    public void setCover(String value)
    {
        // The value may be large so we set it without typing.
        getDriver().executeScript("arguments[0].value = arguments[1];", this.coverTextArea, value);
    }

    /**
     * @return the code that controls the PDF table of contents
     */
    public String getTableOfContents()
    {
        return this.tocTextArea.getAttribute("value");
    }

    /**
     * Sets the code that controls the PDF table of content
     * 
     * @param value the new code for the PDF table of contents
     */
    public void setTableOfContents(String value)
    {
        // The value may be large so we set it without typing.
        getDriver().executeScript("arguments[0].value = arguments[1];", this.tocTextArea, value);
    }

    /**
     * @return the code that controls the PDF header
     */
    public String getHeader()
    {
        return this.headerTextArea.getAttribute("value");
    }

    /**
     * Sets the code that controls the PDF header.
     * 
     * @param value the new code for the PDF header
     */
    public void setHeader(String value)
    {
        // The value may be large so we set it without typing.
        getDriver().executeScript("arguments[0].value = arguments[1];", this.headerTextArea, value);
    }

    /**
     * @return the code that controls the PDF footer
     */
    public String getFooter()
    {
        return this.footerTextArea.getAttribute("value");
    }

    /**
     * Sets the code that controls the PDF footer.
     * 
     * @param value the new code for the PDF footer
     * @since 14.10
     */
    public void setFooter(String value)
    {
        // The value may be large so we set it without typing.
        getDriver().executeScript("arguments[0].value = arguments[1];", this.footerTextArea, value);
    }
}
