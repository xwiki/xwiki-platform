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
package org.xwiki.faq.test.po;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.xwiki.test.ui.po.LiveTableElement;
import org.xwiki.test.ui.po.ViewPage;

/**
 * Represents actions that can be done on the FAQ.WebHome page.
 *
 * @version $Id$
 * @since 4.3M2
 */
public class FAQHomePage extends ViewPage
{
    @FindBy(name = "question")
    private WebElement faqNameField;

    @FindBy(xpath = "//div[@class = 'faq-link add-faq']//input[@class = 'button']")
    private WebElement faqNameButton;

    /**
     * Opens the home page.
     */
    public static FAQHomePage gotoPage()
    {
        getUtil().gotoPage(getSpace(), getPage());
        return new FAQHomePage();
    }

    public static String getSpace()
    {
        return "FAQ";
    }

    public static String getPage()
    {
        return "WebHome";
    }

    /**
     * @param faqName the name of the FAQ entry to add
     * @return the new FAQ entry page
     */
    public FAQEntryEditPage addFAQEntry(String faqName)
    {
        this.faqNameField.clear();
        this.faqNameField.sendKeys(faqName);
        this.faqNameButton.click();
        return new FAQEntryEditPage();
    }

    /**
     * @return the FAQ livetable element
     */
    public LiveTableElement getFAQLiveTable()
    {
        LiveTableElement lt = new LiveTableElement("faqs");
        lt.waitUntilReady();
        return lt;
    }
}
