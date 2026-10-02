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

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.repository.test.SolrTestUtils;
import org.xwiki.test.docker.junit5.SubWikiTestUtils;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.CopyOrRenameOrDeleteStatusPage;
import org.xwiki.test.ui.po.LoginPage;
import org.xwiki.test.ui.po.RenamePage;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.test.ui.po.editor.WikiEditPage;
import org.xwiki.wiki.test.po.DeleteWikiPage;
import org.xwiki.wiki.test.po.JoinWikiPage;
import org.xwiki.wiki.test.po.WikiIndexPage;
import org.xwiki.wiki.test.po.WikiUsersAdministrationSectionPage;
import org.xwiki.wiki.user.MembershipType;
import org.xwiki.wiki.user.UserScope;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
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
        // The RightsManagerPlugin is needed to list the members in the wiki members table
        "xwikiCfgPlugins=com.xpn.xwiki.plugin.rightsmanager.RightsManagerPlugin",
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
    }
)
class SubWikiIT
{
    private static final String SUBWIKI_NAME = "subwiki";

    private static final String MAIN_WIKI_NAME = "xwiki";

    @BeforeAll
    void createSubWiki(TestUtils setup, SubWikiTestUtils subWikiSetup) throws Exception
    {
        setup.loginAsSuperAdmin();
        // Install the default subwiki flavor, as the wiki creation wizard does, which brings in particular the wiki
        // members administration.
        subWikiSetup.createWiki(SUBWIKI_NAME, "org.xwiki.platform:xwiki-platform-wiki-ui-wiki");
        subWikiSetup.setMembershipType(SUBWIKI_NAME, MembershipType.OPEN);
        subWikiSetup.setUserScope(SUBWIKI_NAME, UserScope.LOCAL_AND_GLOBAL);
        setup.forceGuestUser();
    }

    @AfterAll
    void deleteSubWiki(TestUtils setup) throws Exception
    {
        setup.loginAsSuperAdmin();
        WikiIndexPage wikiIndexPage = WikiIndexPage.gotoPage();
        if (wikiIndexPage.getWikiLink(SUBWIKI_NAME) == null) {
            throw new Exception(String.format("The wiki [%s] is not in the wiki index.", SUBWIKI_NAME));
        }
        DeleteWikiPage deleteWikiPage = wikiIndexPage.deleteWiki(SUBWIKI_NAME).confirm(SUBWIKI_NAME);
        assertTrue(deleteWikiPage.hasSuccessMessage());
        // Verify the wiki has been deleted
        wikiIndexPage = WikiIndexPage.gotoPage();
        assertNull(wikiIndexPage.getWikiLink(SUBWIKI_NAME, false));
        setup.forceGuestUser();
    }

    @Test
    @Order(1)
    void movePageToSubwiki(TestUtils setup, TestReference testReference) throws Exception
    {
        setup.loginAsSuperAdmin();
        DocumentReference mainWikiLinkPage = new DocumentReference("xwiki", "Test", "Link");
        DocumentReference externalLinkPageMainWiki = new DocumentReference("xwiki", "SubWikiIT", "ExternalPage");
        DocumentReference externalLinkPageSubWiki = new DocumentReference(SUBWIKI_NAME, "SubWikiIT", "ExternalPage");

        // Ensure that the page does not exist before the test.
        setup.rest().delete(mainWikiLinkPage);
        // The page that will be moved.
        // We'll check moving a hierarchy with relative links
        setup.createPage(testReference,
            "[[Alice]]\n[[Bob]]\n[[Eve]]\n[[Test link>>Test.Link]]\n[[In both>>SubWikiIT.ExternalPage]]",
            "Test relative links");
        SpaceReference rootSpaceReference = testReference.getLastSpaceReference();
        SpaceReference aliceSpace = new SpaceReference("Alice", rootSpaceReference);
        DocumentReference alicePage = new DocumentReference("WebHome", aliceSpace);
        setup.createPage(alicePage, "Alice page", "Alice");
        SpaceReference bobSpace = new SpaceReference("Bob", rootSpaceReference);
        DocumentReference bobPage = new DocumentReference("WebHome", bobSpace);
        setup.createPage(bobPage, "[[Alice]]", "Alice");

        setup.createPage(externalLinkPageMainWiki, "External main wiki");
        setup.createPage(externalLinkPageSubWiki, "External sub wiki");

        // For checking the link update in an external page.
        setup.createPage(mainWikiLinkPage,
            String.format("[[%s]]%n[[%s]]%n[[%s]]",
                setup.serializeLocalReference(testReference),
                setup.serializeLocalReference(alicePage),
                setup.serializeLocalReference(bobPage)));

        // Wait for the Solr indexing to be completed before moving the page
        new SolrTestUtils(setup).waitEmptyQueue();

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
        assertEquals("[[Alice]]\n[[Bob]]\n[[Eve]]\n[[Test link>>xwiki:Test.Link]]"
            + "\n[[In both>>xwiki:SubWikiIT.ExternalPage]]", wikiEditPage.getContent());

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
        assertEquals(String.format("[[%s]]%n[[Bob]]%n[[Eve]]%n[[Test link>>xwiki:Test.Link]]"
                    + "%n[[In both>>xwiki:SubWikiIT.ExternalPage]]",
                serializedlocalAlice2Reference),
            wikiEditPage.getContent());
        wikiEditPage.setContent(String.format("[[Alice2]]%n[[%s]]%n[[Bob]]%n[[Eve]]%n[[Test link>>xwiki:Test.Link]]"
                + "%n[[In both>>xwiki:SubWikiIT.ExternalPage]]",
            serializedlocalAlice2Reference));
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
        assertEquals(String.format("[[%1$s]]%n[[%1$s]]%n[[Bob]]%n[[Eve]]%n[[Test link>>xwiki:Test.Link]]"
                    + "%n[[In both>>xwiki:SubWikiIT.ExternalPage]]",
                serializedAliceReference),
            wikiEditPage.getContent());
        viewPage = setup.gotoPage(mainWikiLinkPage);
        wikiEditPage = viewPage.editWiki();
        assertEquals(
            String.format("[[%s]]%n[[%s]]%n[[%s]]",
                setup.serializeReference(movedPageReference),
                setup.serializeReference(Alice2Reference),
                setup.serializeReference(newBobPage)), wikiEditPage.getContent());
    }

    /**
     * Join an open wiki from the Wiki Index, as a user who is not a member of that wiki.
     */
    @Test
    @Order(2)
    void joinOpenWiki(TestUtils setup)
    {
        String userName = "JoinWikiUser";
        setup.createUserAndLogin(userName, "JoinWikiUserPassword");

        WikiIndexPage wikiIndexPage = WikiIndexPage.gotoPage();
        assertFalse(wikiIndexPage.canLeaveWiki(SUBWIKI_NAME));
        JoinWikiPage joinWikiPage = wikiIndexPage.joinWiki(SUBWIKI_NAME);
        assertThat(joinWikiPage.getConfirmationMessage(),
            containsString(String.format("Are you sure you want to join the wiki %s?", SUBWIKI_NAME)));
        joinWikiPage = joinWikiPage.confirm();
        assertThat(joinWikiPage.getSuccessMessage(), containsString(
            String.format("The user xwiki:XWiki.%s successfully joined wiki %s.", userName, SUBWIKI_NAME)));

        // The user is now a member: Join is replaced by Leave in the Wiki Index, and the wiki administration lists
        // the user among the members.
        wikiIndexPage = WikiIndexPage.gotoPage();
        assertFalse(wikiIndexPage.canJoinWiki(SUBWIKI_NAME));
        assertTrue(wikiIndexPage.canLeaveWiki(SUBWIKI_NAME));

        setup.loginAsSuperAdmin();
        assertThat(WikiUsersAdministrationSectionPage.gotoPage(SUBWIKI_NAME).getMembers(), hasItem(userName));
        setup.forceGuestUser();
    }

    /**
     * Add a global user as member of the wiki from the wiki administration, then remove it.
     */
    @Test
    @Order(3)
    void addAndRemoveWikiMembers(TestUtils setup)
    {
        String userName = "WikiMemberUser";
        setup.loginAsSuperAdmin();
        setup.createUser(userName, "WikiMemberUserPassword", null);

        WikiUsersAdministrationSectionPage usersSection = WikiUsersAdministrationSectionPage.gotoPage(SUBWIKI_NAME);
        assertThat(usersSection.getMembers(), not(hasItem(userName)));
        usersSection.addMembers(userName);
        assertThat(usersSection.getMembers(), hasItem(userName));

        usersSection.removeMember(userName);
        assertThat(usersSection.getMembers(), not(hasItem(userName)));
        // Check that the removal has been saved.
        assertThat(WikiUsersAdministrationSectionPage.gotoPage(SUBWIKI_NAME).getMembers(), not(hasItem(userName)));
        setup.forceGuestUser();
    }

    /**
     * A user local to a subwiki can log in on that subwiki but not on the main wiki.
     */
    @Test
    @Order(4)
    void localSubwikiUserCannotLogInOnMainWiki(TestUtils setup)
    {
        String userName = "LocalSubWikiUser";
        String password = "LocalSubWikiUserPassword";
        setup.loginAsSuperAdmin();
        setup.setCurrentWiki(SUBWIKI_NAME);
        try {
            setup.createUser(userName, password, null);
        } finally {
            setup.setCurrentWiki(MAIN_WIKI_NAME);
        }
        setup.forceGuestUser();

        LoginPage loginPage = LoginPage.gotoPage();
        loginPage.loginAs(userName, password);
        loginPage = new LoginPage();
        assertTrue(loginPage.hasInvalidCredentialsErrorMessage());
        assertFalse(loginPage.isAuthenticated());

        // The same credentials are accepted on the subwiki, which proves the refusal comes from the user being local.
        setup.gotoPage(new DocumentReference(SUBWIKI_NAME, "XWiki", "XWikiLogin"), "login");
        loginPage = new LoginPage();
        loginPage.loginAs(userName, password);
        assertTrue(new ViewPage().isAuthenticated());
        setup.forceGuestUser();
    }
}
