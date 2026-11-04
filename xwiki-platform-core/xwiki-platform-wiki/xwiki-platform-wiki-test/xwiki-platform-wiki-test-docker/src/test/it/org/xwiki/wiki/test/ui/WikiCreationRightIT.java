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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xwiki.test.docker.junit5.ExtensionOverride;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.EditRightsPane;
import org.xwiki.wiki.test.po.CreateWikiPage;
import org.xwiki.wiki.test.po.DeleteWikiPage;
import org.xwiki.wiki.test.po.WikiCreationPage;
import org.xwiki.wiki.test.po.WikiCreationRightsAdministrationSectionPage;
import org.xwiki.wiki.test.po.WikiHomePage;
import org.xwiki.wiki.test.po.WikiIndexPage;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.xwiki.wiki.test.po.WikiCreationRightsAdministrationSectionPage.CREATE_WIKI_RIGHT;

/**
 * Tests the right to create wikis, granted from the Wikis &gt; Creation Right administration section.
 *
 * @version $Id$
 */
@UITest(
    properties = {
        // The RightsManagerPlugin is needed to list the users in the rights editor
        "xwikiCfgPlugins=com.xpn.xwiki.plugin.rightsmanager.RightsManagerPlugin",
        // The Notifications module contributes a Hibernate mapping that needs to be added to hibernate.cfg.xml
        "xwikiDbHbmCommonExtraMappings=notification-filter-preferences.hbm.xml",
        // Deleting a wiki through a script service currently requires that the document hold the script has
        // programming rights, see https://tinyurl.com/2p8u5mhu
        "xwikiPropertiesAdditionalProperties=test.prchecker.excludePattern=.*:WikiManager\\.DeleteWiki"
    },
    extraJARs = {
        // It's currently not possible to install a JAR contributing a Hibernate mapping file as an Extension. Thus
        // we need to provide the JAR inside WEB-INF/lib. See https://jira.xwiki.org/browse/XWIKI-19932
        "org.xwiki.platform:xwiki-platform-notifications-filters-default",
        // Required by components located in a core extensions
        "org.xwiki.platform:xwiki-platform-wiki-template-default",
        // These extensions are needed when creating the subwiki or it'll fail with some NPE.
        "org.xwiki.platform:xwiki-platform-wiki-script",
        "org.xwiki.platform:xwiki-platform-wiki-user-default",
        "org.xwiki.platform:xwiki-platform-wiki-user-script"
    },
    extensionOverrides = {
        @ExtensionOverride(
            extensionId = "org.xwiki.platform:xwiki-platform-web-war",
            overrides = {
                // Set a default UI for the subwiki in the webapp, so that the wiki creation wizard installs it without
                // displaying the flavor picker, which the functional tests do not handle.
                "properties=xwiki.extension.distribution.wikiui=org.xwiki.platform:xwiki-platform-wiki-ui-wiki"
            }
        )
    }
)
class WikiCreationRightIT
{
    private static final String WIKI_CREATOR = "WikiCreatorUser";

    private static final String WIKI_CREATOR_PASSWORD = "WikiCreatorUserPassword";

    private static final String WIKI_NAME = "creatorwiki";

    @AfterEach
    void deleteWiki(TestUtils setup)
    {
        setup.loginAsSuperAdmin();
        WikiIndexPage wikiIndexPage = WikiIndexPage.gotoPage();
        if (wikiIndexPage.getWikiLink(WIKI_NAME, false) != null) {
            DeleteWikiPage deleteWikiPage = wikiIndexPage.deleteWiki(WIKI_NAME).confirm(WIKI_NAME);
            assertTrue(deleteWikiPage.hasSuccessMessage());
            assertNull(WikiIndexPage.gotoPage().getWikiLink(WIKI_NAME, false));
        }
        setup.forceGuestUser();
    }

    @Test
    @Order(1)
    void grantCreateWikiRightToUser(TestUtils setup)
    {
        // A user without the Create Wiki right is not offered to create a wiki.
        setup.createUserAndLogin(WIKI_CREATOR, WIKI_CREATOR_PASSWORD);
        assertFalse(WikiIndexPage.gotoPage().canCreateWiki());

        // Grant the right to the user from the Creation Right administration section.
        setup.loginAsSuperAdmin();
        EditRightsPane rightsPane = WikiCreationRightsAdministrationSectionPage.gotoPage().getEditRightsPane();
        rightsPane.switchToUsers();
        rightsPane.setRight(WIKI_CREATOR, CREATE_WIKI_RIGHT, EditRightsPane.State.ALLOW);
        // Check that the right has been saved.
        rightsPane = WikiCreationRightsAdministrationSectionPage.gotoPage().getEditRightsPane();
        rightsPane.switchToUsers();
        assertEquals(EditRightsPane.State.ALLOW, rightsPane.getRight(WIKI_CREATOR, CREATE_WIKI_RIGHT));

        // The user can now create a wiki with the wiki creation wizard.
        setup.login(WIKI_CREATOR, WIKI_CREATOR_PASSWORD);
        WikiIndexPage wikiIndexPage = WikiIndexPage.gotoPage();
        assertTrue(wikiIndexPage.canCreateWiki());
        CreateWikiPage createWikiPage = wikiIndexPage.createWiki();
        createWikiPage.setPrettyName(WIKI_NAME);
        assertEquals(WIKI_NAME, createWikiPage.getComputedName());
        createWikiPage.setIsTemplate(false);
        WikiCreationPage wikiCreationPage = createWikiPage.goUserStep().create();
        assertEquals("Wiki creation", wikiCreationPage.getStepTitle());
        // The whole default subwiki flavor (xwiki-platform-wiki-ui-wiki, 800+ pages) is installed, which takes time,
        // even more when the CI agent is busy, hence the large timeout.
        wikiCreationPage.waitForFinalizeButton(60 * 5);
        assertFalse(wikiCreationPage.hasLogError());
        // The home page of the new wiki is displayed and the wiki is listed in the Wiki Index.
        WikiHomePage wikiHomePage = wikiCreationPage.finalizeCreation();
        assertThat(wikiHomePage.getPageURL(), containsString("/wiki/" + WIKI_NAME + "/"));
        assertNotNull(WikiIndexPage.gotoPage().getWikiLink(WIKI_NAME));
    }
}
