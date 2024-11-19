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
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.repository.test.SolrTestUtils;
import org.xwiki.test.docker.junit5.ExtensionOverride;
import org.xwiki.test.docker.junit5.TestConfiguration;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.CopyOrRenameOrDeleteStatusPage;
import org.xwiki.test.ui.po.RenamePage;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.test.ui.po.editor.WikiEditPage;
import org.xwiki.wiki.test.po.CreateWikiPage;
import org.xwiki.wiki.test.po.DeleteWikiPage;
import org.xwiki.wiki.test.po.WikiCreationPage;
import org.xwiki.wiki.test.po.WikiIndexPage;
import org.xwiki.wiki.test.po.WikiLink;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Dedicated scenario to perform various tests that needs a subwiki.
 * For testing specifically creation and management of wikis, check the other scenario.
 *
 * @since 12.5RC1
 * @version $Id$
 */
@UITest(
    properties = {
        // The Notifications module contributes a Hibernate mapping that needs to be added to hibernate.cfg.xml
        "xwikiDbHbmCommonExtraMappings=notification-filter-preferences.hbm.xml",
        // Creating and Deleting a wiki through a script service currently requires that the document hold the script
        // has programming rights, see https://tinyurl.com/2p8u5mhu
        "xwikiPropertiesAdditionalProperties=test.prchecker.excludePattern=.*:WikiManager\\.DeleteWiki"
    },
    extraJARs = {
        // It's currently not possible to install a JAR contributing a Hibernate mapping file as an Extension. Thus
        // we need to provide the JAR inside WEB-INF/lib. See https://jira.xwiki.org/browse/XWIKI-19932
        "org.xwiki.platform:xwiki-platform-notifications-filters-default",
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
class SubWikiIT
{
    private static final String SUBWIKI_NAME = "subwiki";

    @Test
    @Order(1)
    void movePageToSubwiki(TestUtils setup, TestReference testReference, TestConfiguration testConfiguration)
        throws Exception
    {
        createSubWiki(setup);

        // Checks that a non-admin user has the Join action proposed for the new sub-wiki.
        setup.createUserAndLogin("U1", "U1");
        WikiIndexPage wikiIndexPage = WikiIndexPage.gotoPage();
        TableLayoutElement tableLayout = wikiIndexPage.getLiveData().getTableLayout();
        tableLayout.assertRow("Actions", hasItem(tableLayout.getWebElementCellWithLinkMatcher("Join",
            setup.getURL(new DocumentReference("xwiki", "WikiManager", "JoinWiki"), "view", "wikiId=subwiki"))));

        setup.loginAsSuperAdmin();
        DocumentReference mainWikiLinkPage = new DocumentReference("xwiki", "Test", "Link");

        // Ensure that the page does not exist before the test.
        setup.rest().delete(mainWikiLinkPage);
        // The page that will be moved.
        // We'll check moving a hierarchy with relative links
        setup.createPage(testReference, "[[Alice]]\n[[Bob]]\n[[Eve]]", "Test relative links");
        SpaceReference rootSpaceReference = testReference.getLastSpaceReference();
        SpaceReference aliceSpace = new SpaceReference("Alice", rootSpaceReference);
        DocumentReference alicePage = new DocumentReference("WebHome", aliceSpace);
        setup.createPage(alicePage, "Alice page", "Alice");
        SpaceReference bobSpace = new SpaceReference("Bob", rootSpaceReference);
        DocumentReference bobPage = new DocumentReference("WebHome", bobSpace);
        setup.createPage(bobPage, "[[Alice]]",
            "Alice");

        // For checking the link update in an external page.
        setup.createPage(mainWikiLinkPage,
            String.format("[[%s]]%n[[%s]]%n[[%s]]",
                setup.serializeLocalReference(testReference),
                setup.serializeLocalReference(alicePage),
                setup.serializeLocalReference(bobPage)));

        // Wait for the Solr indexing to be completed before moving the page
        new SolrTestUtils(setup, testConfiguration.getServletEngine()).waitEmptyQueue();

        // Move the page to subwiki.
        ViewPage viewPage = setup.gotoPage(testReference);
        RenamePage renamePage = viewPage.rename();
        renamePage.setUpdateLinks(true);
        renamePage.getDocumentPicker().setWiki(SUBWIKI_NAME);
        CopyOrRenameOrDeleteStatusPage renameStatusPage = renamePage.clickRenameButton().waitUntilFinished();

        // Ensure the move has been properly done.
        assertEquals("Done.", renameStatusPage.getInfoMessage());
        DocumentReference movedPageReference = testReference.setWikiReference(new WikiReference(SUBWIKI_NAME));
        SpaceReference newRootSpace = movedPageReference.getLastSpaceReference();
        assertTrue(setup.rest().exists(movedPageReference));
        viewPage = renameStatusPage.gotoNewPage();
        assertEquals(
            String.format("/%s/%s/%s", SUBWIKI_NAME, testReference.getLastSpaceReference().extractFirstReference(
                EntityType.SPACE).getName(), "Test relative links"), viewPage.getBreadcrumbContent());
        WikiEditPage wikiEditPage = viewPage.editWiki();
        assertEquals("[[Alice]]\n[[Bob]]\n[[Eve]]", wikiEditPage.getContent());

        SpaceReference newBobSpace = new SpaceReference("Bob", newRootSpace);
        DocumentReference newBobPage = new DocumentReference("WebHome", newBobSpace);
        wikiEditPage = WikiEditPage.gotoPage(newBobPage);
        assertEquals("[[Alice]]", wikiEditPage.getContent());

        SpaceReference newAliceSpace = new SpaceReference("Alice", newRootSpace);
        DocumentReference newAliceReference = new DocumentReference("WebHome", newAliceSpace);

        // Check the link is updated.
        viewPage = setup.gotoPage(mainWikiLinkPage);
        wikiEditPage = viewPage.editWiki();
        assertEquals(
            String.format("[[%s]]%n[[%s]]%n[[%s]]",
                setup.serializeReference(movedPageReference),
                setup.serializeReference(newAliceReference),
                setup.serializeReference(newBobPage)), wikiEditPage.getContent());


        viewPage = setup.gotoPage(newAliceReference);
        renamePage = viewPage.rename();
        renamePage.getDocumentPicker().setName("Alice2");
        renameStatusPage = renamePage.clickRenameButton().waitUntilFinished();
        assertEquals("Done.", renameStatusPage.getInfoMessage());

        SpaceReference Alice2Space = new SpaceReference("Alice2", newRootSpace);
        DocumentReference newrootPage = new DocumentReference("WebHome", newRootSpace);
        DocumentReference Alice2Reference = new DocumentReference("WebHome", Alice2Space);
        wikiEditPage = WikiEditPage.gotoPage(newrootPage);
        String serializedlocalAlice2Reference = setup.serializeLocalReference(Alice2Reference);
        assertEquals(String.format("[[%s]]%n[[Bob]]%n[[Eve]]", serializedlocalAlice2Reference),
            wikiEditPage.getContent());
        wikiEditPage.setContent(String.format("[[Alice2]]%n[[%s]]%n[[Bob]]%n[[Eve]]",serializedlocalAlice2Reference));
        wikiEditPage.clickSaveAndView();

        viewPage = setup.gotoPage(mainWikiLinkPage);
        wikiEditPage = viewPage.editWiki();
        assertEquals(
            String.format("[[%s]]%n[[%s]]%n[[%s]]",
                setup.serializeReference(movedPageReference),
                setup.serializeReference(Alice2Reference),
                setup.serializeReference(newBobPage)), wikiEditPage.getContent());

        viewPage = setup.gotoPage(Alice2Reference);
        renamePage = viewPage.rename();
        renamePage.getDocumentPicker().setWiki("xwiki");
        renameStatusPage = renamePage.clickRenameButton().waitUntilFinished();
        assertEquals("Done.", renameStatusPage.getInfoMessage());

        Alice2Reference = Alice2Reference.setWikiReference(new WikiReference("xwiki"));
        String serializedAliceReference = setup.serializeReference(Alice2Reference);
        wikiEditPage = WikiEditPage.gotoPage(newrootPage);
        assertEquals(String.format("[[%1$s]]%n[[%1$s]]%n[[Bob]]%n[[Eve]]", serializedAliceReference),
            wikiEditPage.getContent());
        viewPage = setup.gotoPage(mainWikiLinkPage);
        wikiEditPage = viewPage.editWiki();
        assertEquals(
            String.format("[[%s]]%n[[%s]]%n[[%s]]",
                setup.serializeReference(movedPageReference),
                setup.serializeLocalReference(Alice2Reference),
                setup.serializeReference(newBobPage)), wikiEditPage.getContent());

        deleteSubWiki(setup);
    }

    /**
     * We create the subwiki as first action before running the test.
     */
    private void createSubWiki(TestUtils setup)
    {
        setup.loginAsSuperAdmin();
        WikiIndexPage wikiIndexPage = WikiIndexPage.gotoPage();
        CreateWikiPage createWikiPage = wikiIndexPage.createWiki();
        createWikiPage.setPrettyName(SUBWIKI_NAME);
        String wikiName = createWikiPage.getComputedName();
        assertEquals(SUBWIKI_NAME, wikiName);
        createWikiPage.setIsTemplate(false);

        // Code taken from WikiTemplateIT.
        WikiCreationPage wikiCreationPage = createWikiPage.goUserStep().create();
        assertEquals("Wiki creation", wikiCreationPage.getStepTitle());

        // Wait for the finalize button to be displayed.
        // Note that the whole flavor defined in the pom.xml (i.e. org.xwiki.platform:xwiki-platform-wiki-ui-wiki) will
        // be copied and that's a lot of pages (over 800+), and this takes time. If the CI agent is busy with other
        // jobs running in parallel it'll take even more time. Thus we put a large value to be safe.
        wikiCreationPage.waitForFinalizeButton(60 * 5);
        // Ensure there is no error in the log.
        assertFalse(wikiCreationPage.hasLogError());

        // Finalization.
        wikiCreationPage.finalizeCreation();
        setup.forceGuestUser();
    }

    /**
     * We delete the subwiki at the end of the tests.
     */
    private void deleteSubWiki(TestUtils setup) throws Exception
    {
        setup.loginAsSuperAdmin();
        // Go to the template wiki
        WikiIndexPage wikiIndexPage = WikiIndexPage.gotoPage();
        WikiLink templateWikiLink = wikiIndexPage.getWikiLink(SUBWIKI_NAME);
        if (templateWikiLink == null) {
            throw new Exception("The wiki [My new template] is not in the wiki index.");
        }
        DeleteWikiPage deleteWikiPage = wikiIndexPage.deleteWiki(SUBWIKI_NAME).confirm(SUBWIKI_NAME);
        assertTrue(deleteWikiPage.hasSuccessMessage());
        // Verify the wiki has been deleted
        wikiIndexPage = WikiIndexPage.gotoPage();
        assertNull(wikiIndexPage.getWikiLink(SUBWIKI_NAME, false));
        setup.forceGuestUser();
    }
}
