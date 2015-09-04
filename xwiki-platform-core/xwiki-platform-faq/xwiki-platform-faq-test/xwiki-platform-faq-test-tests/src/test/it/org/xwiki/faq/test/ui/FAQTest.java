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

import java.util.Arrays;

import org.junit.Rule;
import org.junit.Test;
import org.xwiki.faq.test.po.FAQEntryEditPage;
import org.xwiki.faq.test.po.FAQHomePage;
import org.xwiki.model.reference.DocumentReference;
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

    @Test
    public void testFAQ()
    {
        // Note: we use a dot in the page name to verify it's supported by the FAQ application and we use an accent to
        // verify encoding.
        String faqTestPage = "Test.entrée de FAQ";

        // Delete pages that we create in the test
        getUtil().deletePage(getTestClassName(), faqTestPage);

        // Navigate to the FAQ app by clicking in the Application Panel.
        // This verifies that the FAQ application is registered in the Applications Panel.
        // It also verifies that the Translation is registered properly.
        ApplicationsPanel applicationPanel = ApplicationsPanel.gotoPage();
        ViewPage vp = applicationPanel.clickApplication("FAQ");

        // Verify we're on the right page!
        FAQHomePage homePage = FAQHomePage.DEFAULT_FAQ_HOME_PAGE;
        Assert.assertEquals(homePage.getSpaces(), vp.getMetaDataValue("space"));
        Assert.assertEquals(homePage.getPage(), vp.getMetaDataValue("page"));

        // Add FAQ entry
        FAQEntryEditPage entryPage = homePage.addFAQEntry(faqTestPage);
        entryPage.setAnswer("content");
        vp = entryPage.clickSaveAndView();

        // Go back to the home page by clicking in the breadcrumb (this verifies that the new entry has the FAQ home
        // specified in the breadcrumb).
        vp.clickBreadcrumbLink("FAQ");

        // Assert Livetable:
        // - verify that the Translation has been applied by checking the Translated livetable column name
        // - verify that the Livetable contains our new FAQ entry
        LiveTableElement lt = homePage.getFAQLiveTable();
        Assert.assertTrue(lt.hasRow("Question", faqTestPage + "?"));
    }

    /**
     * Verify that it's possible to add a new FAQ altogether, in a different space. Also make sure it works
     * when creating that new FAQ in a Nested Space.
     */
    @Test
    public void testNewFAQAndInNestedSpace()
    {
        String faqTestPage = "NewFAQEntry";
        DocumentReference homeReference =
            new DocumentReference("xwiki", Arrays.asList(getTestClassName(), "Nested"), "WebHome");

        // Delete pages that we create in the test
        getUtil().deletePage(homeReference);

        // Create a new FAQ home page
        getUtil().addObject(homeReference, "FAQCode.FAQHomeClass", "description", "new FAQ");
        // Note: AddObject stays in edit mode so we need to navigate again
        FAQHomePage homePage = new FAQHomePage(homeReference);
        homePage.gotoPage();

        // Add FAQ entry
        FAQEntryEditPage entryPage = homePage.addFAQEntry(faqTestPage);
        entryPage.setAnswer("new content");
        ViewPage vp = entryPage.clickSaveAndView();

        // Go back to the home page by clicking in the breadcrumb (this verifies that the new entry has the FAQ home
        // specified in the breadcrumb).
        vp.clickBreadcrumbLink("Nested");

        // Assert Livetable:
        // - verify that the Translation has been applied by checking the Translated livetable column name
        // - verify that the Livetable contains our new FAQ entry
        LiveTableElement lt = homePage.getFAQLiveTable();
        Assert.assertTrue(lt.hasRow("Question", faqTestPage + "?"));
    }
}
