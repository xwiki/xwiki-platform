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
package org.xwiki.wiki.test.ui;

import org.junit.Rule;
import org.junit.Test;
import org.xwiki.test.ui.AbstractTest;
import org.xwiki.test.ui.SuperAdminAuthenticationRule;
import org.xwiki.test.ui.po.editor.WikiEditPage;
import org.xwiki.wiki.test.po.CreateWikiPage;
import org.xwiki.wiki.test.po.CreateWikiPageStepUser;
import org.xwiki.wiki.test.po.DeleteWikiPage;
import org.xwiki.wiki.test.po.WikiCreationPage;
import org.xwiki.wiki.test.po.WikiHomePage;
import org.xwiki.wiki.test.po.WikiIndexPage;
import org.xwiki.wiki.test.po.WikiLink;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * UI tests for the wiki templates feature of the Wiki application.
 *
 * @version $Id$
 * @since 6.0M1
 */
public class WikiTemplateTest extends AbstractTest
{
    @Rule
    public SuperAdminAuthenticationRule superAdminAuthenticationRule = new SuperAdminAuthenticationRule(getUtil());

    private static final String TEMPLATE_WIKI_ID = "mynewtemplate";
    private static final String TEMPLATE_CONTENT = "Content of the template";

    private void createTemplateWiki() throws Exception
    {
        // Go to the wiki creation wizard
        WikiIndexPage wikiIndexPage = WikiIndexPage.gotoPage();
        CreateWikiPage createWikiPage = wikiIndexPage.createWiki();
        
        // Full the first step
        createWikiPage.setPrettyName("My new template");
        String wikiName = createWikiPage.getComputedName();
        assertEquals(TEMPLATE_WIKI_ID, wikiName);
        createWikiPage.setDescription("This is the template I do for the tests");
        createWikiPage.setIsTemplate(true);
        assertTrue(createWikiPage.isNextStepEnabled());

        // Second step
        CreateWikiPageStepUser createWikiPageStepUser = createWikiPage.goUserStep();
        
        // Creation step
        WikiCreationPage wikiCreationPage = createWikiPageStepUser.create();
        assertEquals("Wiki creation", wikiCreationPage.getStepTitle());
        // Ensure there is no error in the log
        assertFalse(wikiCreationPage.hasLogError());
        
        // Finalization
        WikiHomePage wikiHomePage = wikiCreationPage.finalizeCreation();

        // Go to the created subwiki, and modify the home page content
        wikiHomePage.edit();
        WikiEditPage wikiEditPage = new WikiEditPage();
        wikiEditPage.setContent(TEMPLATE_CONTENT);
        wikiEditPage.clickSaveAndView();
        wikiEditPage.waitUntilPageIsLoaded();

        // Go back to the wiki creation wizard, and verify the template is in the list of templates in the wizard
        createWikiPage = wikiHomePage.createWiki();
        assertTrue(createWikiPage.getTemplateList().contains("mynewtemplate"));

        // Verify the wiki is in the wiki index page.
        wikiIndexPage = WikiIndexPage.gotoPage().waitUntilPageIsLoaded();
        WikiLink wikiLink = wikiIndexPage.getWikiLink("My new template");
        if (wikiLink == null) {
            throw new Exception("The wiki [My new template] is not in the wiki index.");
        }
        assertTrue(wikiLink.getURL().endsWith("/xwiki/wiki/mynewtemplate/view/Main/"));

    }

    private void deleteTemplateWiki() throws Exception
    {
        // Go to the template wiki
        WikiIndexPage wikiIndexPage = WikiIndexPage.gotoPage().waitUntilPageIsLoaded();
        WikiLink templateWikiLink = wikiIndexPage.getWikiLink("My new template");
        if (templateWikiLink == null) {
            throw new Exception("The wiki [My new template] is not in the wiki index.");
        }
        WikiHomePage wikiHomePage = templateWikiLink.click();
        // Delete the wiki
        DeleteWikiPage deleteWikiPage = wikiHomePage.deleteWiki().confirm(TEMPLATE_WIKI_ID);
        assertTrue(deleteWikiPage.hasSuccessMessage());
        // Verify the wiki has been deleted
        wikiIndexPage = WikiIndexPage.gotoPage().waitUntilPageIsLoaded();
        assertNull(wikiIndexPage.getWikiLink("My new template"));
    }
    
    private void createWikiFromTemplate()
    {
        // Go to the wiki creation wizard
        WikiIndexPage wikiIndexPage = WikiIndexPage.gotoPage();
        CreateWikiPage createWikiPage = wikiIndexPage.createWiki();

        // First step
        createWikiPage.setPrettyName("My new wiki");
        String wikiName = createWikiPage.getComputedName();
        assertEquals("mynewwiki", wikiName);
        createWikiPage.setTemplate(TEMPLATE_WIKI_ID);
        createWikiPage.setIsTemplate(false);
        createWikiPage.setDescription("My first wiki");

        // Second step
        CreateWikiPageStepUser createWikiPageStepUser = createWikiPage.goUserStep();
        WikiCreationPage wikiCreationPage = createWikiPageStepUser.create();
        assertEquals("Wiki creation", wikiCreationPage.getStepTitle());
        wikiCreationPage.waitForFinalizeButton(30);
        // Ensure there is no error in the log
        assertFalse(wikiCreationPage.hasLogError());

        // Finalization
        WikiHomePage wikiHomePage = wikiCreationPage.finalizeCreation();

        // Go the created subwiki and verify the content of the main page is the same than in the template
        assertEquals(wikiHomePage.getContent(), TEMPLATE_CONTENT);

        // Delete the wiki
        DeleteWikiPage deleteWikiPage = wikiHomePage.deleteWiki();
        deleteWikiPage = deleteWikiPage.confirm("");
        assertTrue(deleteWikiPage.hasUserErrorMessage());
        assertTrue(deleteWikiPage.hasWikiDeleteConfirmationInput(""));

        deleteWikiPage = deleteWikiPage.confirm("My new wiki");
        assertTrue(deleteWikiPage.hasUserErrorMessage());
        assertTrue(deleteWikiPage.hasWikiDeleteConfirmationInput("My new wiki"));

        deleteWikiPage = deleteWikiPage.confirm("mynewwiki");
        assertTrue(deleteWikiPage.hasSuccessMessage());

        // Verify the wiki has been deleted
        wikiIndexPage = WikiIndexPage.gotoPage().waitUntilPageIsLoaded();
        assertNull(wikiIndexPage.getWikiLink("My new wiki"));
    }

    @Test
    public void createWikiFromTemplateTest() throws Exception
    {
        // Create the template
        createTemplateWiki();
        
        // Create the wiki from the template
        createWikiFromTemplate();
        // Do it twice to check if we can create a wiki with the name of a deleted one
        createWikiFromTemplate();

        // Delete the template wiki
        deleteTemplateWiki();
    }
}
