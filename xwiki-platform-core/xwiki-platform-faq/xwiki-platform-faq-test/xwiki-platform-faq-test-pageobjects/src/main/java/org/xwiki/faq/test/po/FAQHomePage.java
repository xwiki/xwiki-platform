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

import java.util.Arrays;

import org.openqa.selenium.WebElement;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.test.ui.po.LiveTableElement;
import org.xwiki.test.ui.po.ViewPage;

/**
 * Represents actions that can be done on a FAQ home page.
 *
 * @version $Id$
 * @since 4.3M2
 */
public class FAQHomePage extends ViewPage
{
    private EntityReference homeReference;

    public static final FAQHomePage DEFAULT_FAQ_HOME_PAGE = new FAQHomePage(
        new DocumentReference("xwiki", Arrays.asList("FAQ"), "WebHome"));

    public FAQHomePage(EntityReference homeReference)
    {
        this.homeReference = homeReference;
    }

    /**
     * Opens the home page.
     */
    public void gotoPage()
    {
        getUtil().gotoPage(this.homeReference);
    }

    /**
     * @since 7.2RC1
     */
    public String getSpaces()
    {
        return getUtil().serializeReference(
            this.homeReference.extractReference(EntityType.SPACE).removeParent(new WikiReference("xwiki")));
    }

    public String getPage()
    {
        return this.homeReference.getName();
    }

    /**
     * @param faqName the name of the FAQ entry to add
     * @return the new FAQ entry page
     */
    public FAQEntryEditPage addFAQEntry(String faqName)
    {
        WebElement faqNameField = getDriver().findElementByName("question");
        WebElement faqNameButton = getDriver().findElementByXPath(
            "//div[contains(@class, 'faq-link add-faq')]//input[contains(@class, 'btn btn-success')]");
        faqNameField.clear();
        faqNameField.sendKeys(faqName);
        faqNameButton.click();
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
