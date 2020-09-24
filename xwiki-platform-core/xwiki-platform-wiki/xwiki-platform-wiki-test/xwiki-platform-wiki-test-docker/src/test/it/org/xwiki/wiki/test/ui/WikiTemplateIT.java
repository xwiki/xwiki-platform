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

import org.junit.jupiter.api.Test;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.integration.junit.LogCaptureConfiguration;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.editor.WikiEditPage;
import org.xwiki.wiki.test.po.CreateWikiPage;
import org.xwiki.wiki.test.po.CreateWikiPageStepUser;
import org.xwiki.wiki.test.po.DeleteWikiPage;
import org.xwiki.wiki.test.po.WikiCreationPage;
import org.xwiki.wiki.test.po.WikiHomePage;
import org.xwiki.wiki.test.po.WikiIndexPage;
import org.xwiki.wiki.test.po.WikiLink;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * UI tests for the wiki templates feature of the Wiki application.
 *
 * @version $Id$
 */
@UITest(
    properties = {
        // The Notifications module contributes a Hibernate mapping that needs to be added to hibernate.cfg.xml.
        "xwikiDbHbmCommonExtraMappings=notification-filter-preferences.hbm.xml",
    },
    extraJARs = {
        // It's currently not possible to install a JAR contributing a Hibernate mapping file as an Extension. Thus
        // we need to provide the JAR inside WEB-INF/lib. See https://jira.xwiki.org/browse/XWIKI-8271
        "org.xwiki.platform:xwiki-platform-notifications-filters-default",
        // It's currently not possible to install a JAR contributing a Hibernate mapping file as an Extension. Thus
        // we need to provide the JAR inside WEB-INF/lib. See https://jira.xwiki.org/browse/XWIKI-8271
        "org.xwiki.platform:xwiki-platform-eventstream-store-hibernate",
        // The Solr store is not ready yet to be installed as an extension. We need it since the Tag UI requires
        // Notifications, as otherwise even streams won't have a store.
        "org.xwiki.platform:xwiki-platform-eventstream-store-solr",
        // Needed to be in WEB-INF/lib since it's used by Struts which is a core feature (and thus in the webapp CL).
        // Otherwise it gets provisioned as an extension and is not available from the webapp CL.
        "org.xwiki.platform:xwiki-platform-extension-distribution"
    }
)
class WikiTemplateIT
{
    private static final String TEMPLATE_WIKI_ID = "mynewtemplate";

    private static final String TEMPLATE_CONTENT = "Content of the template";

    @Test
    void createWikiFromTemplateTest(TestUtils setup, LogCaptureConfiguration logCaptureConfiguration) throws Exception
    {
        setup.loginAsSuperAdmin();

        // Create the template
        createTemplateWiki();

        // Create the wiki from the template
        createWikiFromTemplate();
        // Do it twice to check if we can create a wiki with the name of a deleted one
        createWikiFromTemplate();

        // Delete the template wiki
        deleteTemplateWiki();

        logCaptureConfiguration.registerExcludes(
            "CSRFToken: Secret token verification failed");
    }

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
        // Creation step + click Finalize button
        WikiHomePage wikiHomePage = executeCreationStepAndFinalize(createWikiPageStepUser);

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
        DeleteWikiPage deleteWikiPage = wikiIndexPage.deleteWiki(TEMPLATE_WIKI_ID).confirm(TEMPLATE_WIKI_ID);
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

        // Creation step + click Finalize button
        WikiHomePage wikiHomePage = executeCreationStepAndFinalize(createWikiPageStepUser);

        // Go the created subwiki and verify the content of the main page is the same than in the template
        assertEquals(TEMPLATE_CONTENT, wikiHomePage.getContent());

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

    private WikiHomePage executeCreationStepAndFinalize(CreateWikiPageStepUser createWikiPageStepUser)
    {
        WikiCreationPage wikiCreationPage = createWikiPageStepUser.create();
        assertEquals("Wiki creation", wikiCreationPage.getStepTitle());

        // Wait for the finalize button to be displayed.
        // Note that the whole flavor defined in the pom.xml (i.e. org.xwiki.platform:xwiki-platform-wiki-ui-wiki) will
        // be copied and that's a lot of pages (over 800+), and this takes time. If the CI agent is busy with other
        // jobs running in parallel it'll take even more time. Thus we put a large value to be safe.
        wikiCreationPage.waitForFinalizeButton(60 * 5);
        // Ensure there is no error in the log
        assertFalse(wikiCreationPage.hasLogError());

        // Finalization
        WikiHomePage wikiHomePage = wikiCreationPage.finalizeCreation();
        return wikiHomePage;
    }
}
