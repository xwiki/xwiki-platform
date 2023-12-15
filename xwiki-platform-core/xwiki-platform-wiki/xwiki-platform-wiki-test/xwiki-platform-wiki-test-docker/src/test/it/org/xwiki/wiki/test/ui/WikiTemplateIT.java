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

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xwiki.livedata.test.po.TableLayoutElement;
import org.xwiki.test.docker.junit5.ExtensionOverride;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.integration.junit.LogCaptureConfiguration;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.editor.WikiEditPage;
import org.xwiki.wiki.test.po.AdminWikiTemplatesPage;
import org.xwiki.wiki.test.po.CreateWikiPage;
import org.xwiki.wiki.test.po.CreateWikiPageStepUser;
import org.xwiki.wiki.test.po.DeleteWikiPage;
import org.xwiki.wiki.test.po.WikiCreationPage;
import org.xwiki.wiki.test.po.WikiHomePage;
import org.xwiki.wiki.test.po.WikiIndexPage;
import org.xwiki.wiki.test.po.WikiLink;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.xwiki.wiki.test.po.WikiIndexPage.WIKI_NAME_COLUMN_LABEL;

/**
 * UI tests for the wiki templates feature of the Wiki application.
 *
 * @version $Id$
 */
@UITest(
    properties = {
        // The Notifications module contributes a Hibernate mapping that needs to be added to hibernate.cfg.xml.
        "xwikiDbHbmCommonExtraMappings=notification-filter-preferences.hbm.xml",
        // Deleting a wiki through a script service currently requires that the document hold the script
        // has programming rights, see https://tinyurl.com/2p8u5mhu
        "xwikiPropertiesAdditionalProperties=test.prchecker.excludePattern=.*:WikiManager\\.DeleteWiki"
    },
    extraJARs = {
        // It's currently not possible to install a JAR contributing a Hibernate mapping file as an Extension. Thus,
        // we need to provide the JAR inside WEB-INF/lib. See https://jira.xwiki.org/browse/XWIKI-19932
        "org.xwiki.platform:xwiki-platform-notifications-filters-default",
        // The Solr store is not ready yet to be installed as an extension, so we need to add it to WEB-INF/lib
        // manually. See https://jira.xwiki.org/browse/XWIKI-21594
        // We need it since the Tag UI requires Notifications, as otherwise even streams won't have a store.
        "org.xwiki.platform:xwiki-platform-eventstream-store-solr",
        // Required by components located in a core extensions
        "org.xwiki.platform:xwiki-platform-wiki-template-default",
        // These extensions are needed when creating the subwiki or it'll fail with some NPE.
        // TODO: improve the docker test framework to indicate xwiki-platform-wiki-ui-wiki instead of all those jars one
        // by one
        "org.xwiki.platform:xwiki-platform-wiki-script",
        "org.xwiki.platform:xwiki-platform-wiki-user-default",
        "org.xwiki.platform:xwiki-platform-wiki-user-script"
    },
    extensionOverrides = {
        @ExtensionOverride(
            extensionId = "org.xwiki.platform:xwiki-platform-web-war",
            overrides = {
                // We set a default UI for the subwiki in the webapp, so that the Wiki Creation UI knows which extension
                // to install on a subwiki by default (which is something we test)
                // Otherwise the wiki creation form will display the flavor picker and the functional tests do not
                // handle it.
                "properties=xwiki.extension.distribution.wikiui=org.xwiki.platform:xwiki-platform-wiki-ui-wiki"
            }
        )
    }
)
class WikiTemplateIT
{
    private static final String TEMPLATE_WIKI_ID = "mynewtemplate";

    private static final String TEMPLATE_CONTENT = "Content of the template";

    @Test
    @Order(1)
    void createWikiFromTemplateTest(TestUtils setup, LogCaptureConfiguration logCaptureConfiguration) throws Exception
    {
        setup.loginAsSuperAdmin();

        // Create the template.
        createTemplateWiki(setup);

        // Create the wiki from the template
        createWikiFromTemplate();
        // Do it twice to check if we can create a wiki with the name of a deleted one.
        createWikiFromTemplate();

        // Delete the template wiki.
        deleteTemplateWiki();

        logCaptureConfiguration.registerExcludes(
            "CSRFToken: Secret token verification failed");
    }

    private void createTemplateWiki(TestUtils setup) throws Exception
    {
        // Go to the wiki creation wizard.
        WikiIndexPage wikiIndexPage = WikiIndexPage.gotoPage();
        CreateWikiPage createWikiPage = wikiIndexPage.createWiki();

        // Full the first step.
        createWikiPage.setPrettyName("My new template");
        String wikiName = createWikiPage.getComputedName();
        assertEquals(TEMPLATE_WIKI_ID, wikiName);
        createWikiPage.setDescription("This is the template I do for the tests");
        createWikiPage.setIsTemplate(true);

        // Second step.
        CreateWikiPageStepUser createWikiPageStepUser = createWikiPage.goUserStep();

        // Creation step.
        // Creation step + click Finalize button.
        WikiHomePage wikiHomePage = executeCreationStepAndFinalize(createWikiPageStepUser);

        // Go to the created subwiki, and modify the home page content.
        wikiHomePage.edit();
        WikiEditPage wikiEditPage = new WikiEditPage();
        wikiEditPage.setContent(TEMPLATE_CONTENT);
        wikiEditPage.clickSaveAndView();

        // Go back to the wiki creation wizard, and verify the template is in the list of templates in the wizard.
        createWikiPage = wikiHomePage.createWiki();
        assertTrue(createWikiPage.getTemplateList().contains("mynewtemplate"));

        // Verify the wiki is in the wiki index page.
        wikiIndexPage = WikiIndexPage.gotoPage();
        WikiLink wikiLink = wikiIndexPage.getWikiLink("My new template");
        if (wikiLink == null) {
            throw new Exception("The wiki [My new template] is not in the wiki index.");
        }
        assertThat(wikiLink.getURL(), endsWith("/xwiki/wiki/mynewtemplate/view/Main/"));

        // Verify the wiki template is displayed in the admin wiki templates list.
        TableLayoutElement tableLayout = AdminWikiTemplatesPage.goToPage().getLiveData().getTableLayout();
        assertEquals(1, tableLayout.countRows());
        tableLayout.assertCellWithLink(WIKI_NAME_COLUMN_LABEL, "My new template",
            setup.getBaseURL() + "wiki/mynewtemplate/view/Main/");
        tableLayout.assertRow("Description", "This is the template I do for the tests");
        tableLayout.assertRow("Owner", "superadmin");
        tableLayout.assertRow("Membership Type", "Open for any user to join");
    }

    private void deleteTemplateWiki() throws Exception
    {
        // Go to the template wiki.
        WikiIndexPage wikiIndexPage = WikiIndexPage.gotoPage();
        WikiLink templateWikiLink = wikiIndexPage.getWikiLink("My new template");
        if (templateWikiLink == null) {
            throw new Exception("The wiki [My new template] is not in the wiki index.");
        }
        DeleteWikiPage deleteWikiPage = wikiIndexPage.deleteWiki("My new template").confirm(TEMPLATE_WIKI_ID);
        assertTrue(deleteWikiPage.hasSuccessMessage());
        // Verify the wiki has been deleted.
        wikiIndexPage = WikiIndexPage.gotoPage();
        assertNull(wikiIndexPage.getWikiLink("My new template", false));
    }

    private void createWikiFromTemplate()
    {
        // Go to the wiki creation wizard.
        WikiIndexPage wikiIndexPage = WikiIndexPage.gotoPage();
        CreateWikiPage createWikiPage = wikiIndexPage.createWiki();

        // First step.
        createWikiPage.setPrettyName("My new wiki");
        String wikiName = createWikiPage.getComputedName();
        assertEquals("mynewwiki", wikiName);
        createWikiPage.setTemplate(TEMPLATE_WIKI_ID);
        createWikiPage.setIsTemplate(false);
        createWikiPage.setDescription("My first wiki");

        // Second step.
        CreateWikiPageStepUser createWikiPageStepUser = createWikiPage.goUserStep();

        // Creation step + click Finalize button.
        WikiHomePage wikiHomePage = executeCreationStepAndFinalize(createWikiPageStepUser);

        // Go the created subwiki and verify the content of the main page is the same than in the template.
        assertEquals(TEMPLATE_CONTENT, wikiHomePage.getContent());

        // Delete the wiki
        DeleteWikiPage deleteWikiPage = wikiHomePage.deleteWiki("My new wiki");
        deleteWikiPage = deleteWikiPage.confirm("");
        assertTrue(deleteWikiPage.hasUserErrorMessage());
        assertTrue(deleteWikiPage.hasWikiDeleteConfirmationInput(""));

        deleteWikiPage = deleteWikiPage.confirm("My new wiki");
        assertTrue(deleteWikiPage.hasUserErrorMessage());
        assertTrue(deleteWikiPage.hasWikiDeleteConfirmationInput("My new wiki"));

        deleteWikiPage = deleteWikiPage.confirm("mynewwiki");
        assertTrue(deleteWikiPage.hasSuccessMessage());

        // Verify the wiki has been deleted.
        wikiIndexPage = WikiIndexPage.gotoPage();
        assertNull(wikiIndexPage.getWikiLink("My new wiki", false));
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
        // Ensure there is no error in the log.
        assertFalse(wikiCreationPage.hasLogError());

        // Finalization.
        return wikiCreationPage.finalizeCreation();
    }
}
