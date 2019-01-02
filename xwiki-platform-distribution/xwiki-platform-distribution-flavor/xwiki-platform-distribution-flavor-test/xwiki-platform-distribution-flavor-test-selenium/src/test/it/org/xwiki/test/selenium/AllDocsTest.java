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
package org.xwiki.test.selenium;

import org.junit.Assert;
import org.junit.Test;
import org.xwiki.test.selenium.framework.AbstractXWikiTestCase;

/**
 * Verify the table view for AllDocs wiki document.
 *
 * @version $Id$
 */
public class AllDocsTest extends AbstractXWikiTestCase
{
    /**
     * This method makes the following tests :
     * <ul>
     * <li>Validate presence of "Actions" column in table view for administrator.</li>
     * <li>Validate absence of "Actions" column for users without administration rights.</li>
     * <li>Validate input suggest for Page field.</li>
     * <li>Validate input suggest for Space field.</li>
     * <li>Validate input suggest for Last Author field.</li>
     * <li>Validate Copy link action.</li>
     * <li>Validate Rename link action.</li>
     * <li>Validate Delete link action.</li>
     * <li>Validate Rights link action.</li>
     * </ul>
     */
    @Test
    public void testTableViewActions()
    {
        // Validate absence of "Actions" column for users without administration rights and verify there are
        // elements in the table
        open("Main", "AllDocs");
        // We verify we have a least 2 pages displayed
        waitForTextContains("//span[@class='xwiki-livetable-pagination-content']", "1 2");
        if (isAuthenticated()) {
            logout();
        }
        assertElementNotPresent("//td[text()='Actions']");

        // Create a new page that will be used in this test.
        loginAsAdmin();
        String spaceName = this.getClass().getSimpleName();
        String pageName = getTestMethodName();
        open(spaceName, pageName, "edit", "editor=wiki&title=Actions+test");
        clickEditSaveAndContinue();

        // Validate presence of "Actions" column in table view for administrator.
        open("Main", "AllDocs");
        waitForTextContains("//span[@class='xwiki-livetable-pagination-content']", "1 2");
        assertElementPresent("//th[normalize-space(text())='Actions']");

        // Validate input suggest for Location field.
        getSelenium().focus("doc.location");
        getSelenium().typeKeys("doc.location", "ViewActions");
        // Note: We wait on the pagination result since it's the last element updated and it's done after the
        // table rows have been updated so this allows us to wait on it. In the code below "1" represents the
        // displayed pages.
        waitForTextPresent("//span[@class='xwiki-livetable-pagination-content']", "1");
        assertElementPresent("//td[contains(@class, 'doc_location')]//a[text()='" + pageName + "']");

        // Validate input suggest for Last Author field.
        open("Main", "AllDocs");
        waitForTextContains("//span[@class='xwiki-livetable-pagination-content']", "1 2");
        // "doc.author", but selecting the input element contained in the "selectized" suggest control.
        getSelenium().focus("xwiki-livetable-alldocs-filter-4-selectized");
        getSelenium().typeKeys("xwiki-livetable-alldocs-filter-4-selectized", "SomeUnknownAuthor");
        waitForElement("//div[@class='selectize-dropdown-content']/div[contains(@class, 'create')]");
        getSelenium().click("//div[@class='selectize-dropdown-content']/div[contains(@class, 'create')]");
        waitForTextPresent("//span[@class='xwiki-livetable-pagination-content']", "");
        assertElementNotPresent("//td[contains(@class, 'doc_location')]//a[text()='" + pageName + "']");

        // Validate Copy link action.
        open("Main", "AllDocs");
        waitForTextContains("//span[@class='xwiki-livetable-pagination-content']", "1 2");
        getSelenium().focus("doc.location");
        getSelenium().typeKeys("doc.location", "view");
        waitForTextPresent("//span[@class='xwiki-livetable-pagination-content']", "1");
        assertElementPresent("//td[contains(@class, 'doc_location')]//a[text()='" + pageName + "']");
        clickLinkWithLocator("//a[contains(@class, 'actioncopy') and contains(@href, '" + pageName + "')]");
        // The copy page form doesn't allow us to copy to a new space.
        setFieldValue("targetSpaceName", "Sandbox");
        setFieldValue("targetPageName", pageName + "New");
        clickLinkWithLocator("//input[@value='Copy']");
        open("Main", "AllDocs");
        getSelenium().focus("doc.location");
        getSelenium().typeKeys("doc.location", "Sandbox");
        waitForTextPresent("//span[@class='xwiki-livetable-pagination-content']", "1");
        assertElementPresent("//td[contains(@class, 'doc_location')]//a[text()='" + pageName + "New']");

        // Validate Rename link action.
        open("Main", "AllDocs");
        waitForTextContains("//span[@class='xwiki-livetable-pagination-content']", "1 2");
        getSelenium().focus("doc.location");
        getSelenium().typeKeys("doc.location", "actionsNew");
        waitForTextPresent("//span[@class='xwiki-livetable-pagination-content']", "1");
        clickLinkWithLocator("//tbody/tr/td/a[text()='rename']");
        setFieldValue("newPageName", pageName + "NewRenamed");
        clickLinkWithLocator("//input[@value='Rename']");
        open("Main", "AllDocs");
        getSelenium().focus("doc.location");
        getSelenium().typeKeys("doc.location", "renamed");
        waitForTextPresent("//span[@class='xwiki-livetable-pagination-content']", "1");
        assertElementPresent("//td[contains(@class, 'doc_location')]//a[text()='" + pageName + "NewRenamed']");

        // Validate Delete link action.
        open("Main", "AllDocs");
        waitForTextContains("//span[@class='xwiki-livetable-pagination-content']", "1 2");
        getSelenium().focus("doc.location");
        getSelenium().typeKeys("doc.location", "renamed");
        waitForTextPresent("//span[@class='xwiki-livetable-pagination-content']", "1");
        clickLinkWithLocator("//tbody/tr/td/a[text()='delete']");
        clickLinkWithLocator("//button[contains(@class, 'confirm')]");
        assertTextPresent("Done.");
        open("Main", "AllDocs");
        getSelenium().focus("doc.location");
        getSelenium().typeKeys("doc.location", "actions");
        waitForTextPresent("//span[@class='xwiki-livetable-pagination-content']", "1");
        assertElementNotPresent("//td[contains(@class, 'doc_location')]//a[text()='" + pageName + "NewRenamed']");

        // Validate Rights link action.
        open("Main", "AllDocs");
        waitForTextContains("//span[@class='xwiki-livetable-pagination-content']", "1 2");
        getSelenium().focus("doc.location");
        getSelenium().typeKeys("doc.location", pageName);
        waitForTextPresent("//span[@class='xwiki-livetable-pagination-content']", "1");
        clickLinkWithLocator("//tbody/tr/td/a[text()='rights']");
        Assert.assertEquals("Editing access rights for Actions test", getTitle());
    }
}
