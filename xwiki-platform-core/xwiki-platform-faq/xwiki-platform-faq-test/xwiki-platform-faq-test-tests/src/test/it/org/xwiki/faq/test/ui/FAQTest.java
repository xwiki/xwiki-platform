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
package org.xwiki.faq.test.ui;

import org.junit.Rule;
import org.junit.Test;
import org.xwiki.faq.test.po.FAQEntryEditPage;
import org.xwiki.faq.test.po.FAQHomePage;
import org.xwiki.panels.test.po.ApplicationsPanel;
import org.xwiki.test.ui.AbstractTest;
import org.xwiki.test.ui.SuperAdminAuthenticationRule;
import org.xwiki.test.ui.po.LiveTableElement;
import org.xwiki.test.ui.po.ViewPage;

import org.junit.Assert;

/**
 * UI tests for the FAQ application.
 *
 * @version $Id$
 * @since 4.3M2
 */
public class FAQTest extends AbstractTest
{
    // Login as superadmin to have delete rights.
    @Rule
    public SuperAdminAuthenticationRule authenticationRule = new SuperAdminAuthenticationRule(getUtil());

    // Note: we use a dot in the page name to verify it's supported by the FAQ application and we use an accent to
    // verify encoding.
    private static final String FAQ_TEST_PAGE = "Test.entrée de FAQ";

    @Test
    public void testFAQ()
    {
        // Delete pages that we create in the test
        getUtil().deletePage(getTestClassName(), FAQ_TEST_PAGE);

        // Navigate to the FAQ app by clicking in the Application Panel.
        // This verifies that the FAQ application is registered in the Applications Panel.
        // It also verifies that the Translation is registered properly.
        ApplicationsPanel applicationPanel = ApplicationsPanel.gotoPage();
        ViewPage vp = applicationPanel.clickApplication("FAQ");

        // Verify we're on the right page!
        Assert.assertEquals(FAQHomePage.getSpace(), vp.getMetaDataValue("space"));
        Assert.assertEquals(FAQHomePage.getPage(), vp.getMetaDataValue("page"));
        FAQHomePage homePage = new FAQHomePage();

        // Add FAQ entry
        FAQEntryEditPage entryPage = homePage.addFAQEntry(FAQ_TEST_PAGE);
        entryPage.setAnswer("content");
        vp = entryPage.clickSaveAndView();

        // Go back to the home page by clicking in the breadcrumb
        vp.clickBreadcrumbLink("FAQ");
        homePage = new FAQHomePage();

        // Assert Livetable:
        // - verify that the Translation has been applied by checking the Translated livetable column name
        // - verify that the Livetable contains our new FAQ entry
        LiveTableElement lt = homePage.getFAQLiveTable();
        Assert.assertTrue(lt.hasRow("Question", FAQ_TEST_PAGE + "?"));
    }
}
