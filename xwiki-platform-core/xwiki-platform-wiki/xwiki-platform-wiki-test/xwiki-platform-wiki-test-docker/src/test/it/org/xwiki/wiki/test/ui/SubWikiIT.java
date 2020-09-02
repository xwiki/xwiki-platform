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

import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.test.docker.junit5.ExtensionOverride;
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
import org.xwiki.wiki.test.po.WikiHomePage;
import org.xwiki.wiki.test.po.WikiIndexPage;
import org.xwiki.wiki.test.po.WikiLink;

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
        // TODO: Remove once https://jira.xwiki.org/browse/XWIKI-7581 is fixed
        "xwikiCfgSuperadminPassword=pass",
        // The Notifications module contributes a Hibernate mapping that needs to be added to hibernate.cfg.xml
        "xwikiDbHbmCommonExtraMappings=notification-filter-preferences.hbm.xml",
        // Disable the DW
        "xwikiPropertiesAdditionalProperties=distribution.automaticStartOnMainWiki=false",
        // Enable Hibernate statistics to debug SQL execution times
        "xwikiDbAdditionalProperties=<property name=\"hibernate.generate_statistics\">true</property>"
    },
    extraJARs = {
        // It's currently not possible to install a JAR contributing a Hibernate mapping file as an Extension. Thus
        // we need to provide the JAR inside WEB-INF/lib. See https://jira.xwiki.org/browse/XWIKI-8271
        "org.xwiki.platform:xwiki-platform-notifications-filters-default",
        // Required by components located in a core extensions
        "org.xwiki.platform:xwiki-platform-wiki-template-default",
        // TODO: improve the docker test framework to indicate xwiki-platform-wiki-ui-wiki instead of all those jars one
        // by one
        // Needed by the subwikis
        "org.xwiki.platform:xwiki-platform-wiki-script",
        "org.xwiki.platform:xwiki-platform-wiki-user-default",
        "org.xwiki.platform:xwiki-platform-wiki-user-script"
    },
    extensionOverrides = {
        @ExtensionOverride(
            extensionId = "org.xwiki.platform:xwiki-platform-web",
            overrides = {
                // We set a default UI for the subwiki in the webapp, so that the Wiki Creation UI knows which extension
                // to install on a subwiki by default (which is something we test)
                // Otherwise the wiki creation form will display the flavor picker and the functional tests do not handle it.
                "properties=xwiki.extension.distribution.wikiui=org.xwiki.platform:xwiki-platform-wiki-ui-wiki"
            }
        )
    }
)
public class SubWikiIT
{
    private static final String SUBWIKI_NAME = "subwiki";

    private WikiHomePage wikiHomePage;

    // We create the subwiki as first global action before running tests.
    @BeforeAll
    public void createSubWiki(TestUtils setup)
    {
        setup.loginAsSuperAdmin();
        WikiIndexPage wikiIndexPage = WikiIndexPage.gotoPage();
        CreateWikiPage createWikiPage = wikiIndexPage.createWiki();
        createWikiPage.setPrettyName(SUBWIKI_NAME);
        String wikiName = createWikiPage.getComputedName();
        assertEquals(SUBWIKI_NAME, wikiName);
        createWikiPage.setIsTemplate(false);
        assertTrue(createWikiPage.isNextStepEnabled());

        // Code taken from WikiTemplateIT.
        WikiCreationPage wikiCreationPage = createWikiPage.goUserStep().create();
        assertEquals("Wiki creation", wikiCreationPage.getStepTitle());

        // Wait for the finalize button to be displayed.
        // Note that the whole flavor defined in the pom.xml (i.e. org.xwiki.platform:xwiki-platform-wiki-ui-wiki) will
        // be copied and that's a lot of pages (over 800+), and this takes time. If the CI agent is busy with other
        // jobs running in parallel it'll take even more time. Thus we put a large value to be safe.
        wikiCreationPage.waitForFinalizeButton(60 * 5);
        // Ensure there is no error in the log
        assertFalse(wikiCreationPage.hasLogError());

        // Finalization
       this.wikiHomePage = wikiCreationPage.finalizeCreation();
       setup.forceGuestUser();
    }

    // We delete the subwiki once all tests are finished.
    @AfterAll
    public void deleteSubWiki(TestUtils setup) throws Exception
    {
        setup.loginAsSuperAdmin();
        // Go to the template wiki
        WikiIndexPage wikiIndexPage = WikiIndexPage.gotoPage().waitUntilPageIsLoaded();
        WikiLink templateWikiLink = wikiIndexPage.getWikiLink(SUBWIKI_NAME);
        if (templateWikiLink == null) {
            throw new Exception("The wiki [My new template] is not in the wiki index.");
        }
        DeleteWikiPage deleteWikiPage = wikiIndexPage.deleteWiki(SUBWIKI_NAME).confirm(SUBWIKI_NAME);
        assertTrue(deleteWikiPage.hasSuccessMessage());
        // Verify the wiki has been deleted
        wikiIndexPage = WikiIndexPage.gotoPage().waitUntilPageIsLoaded();
        assertNull(wikiIndexPage.getWikiLink(SUBWIKI_NAME));
        setup.forceGuestUser();
    }

    @Test
    public void movePageToSubwiki(TestUtils setup, TestReference testReference) throws Exception
    {
        setup.loginAsSuperAdmin();
        DocumentReference mainWikiLinkPage = new DocumentReference("xwiki", "Test", "Link");

        // ensure that the page does not exist before the test
        setup.rest().delete(mainWikiLinkPage);

        String space = testReference.getSpaceReferences()
            .stream().map(SpaceReference::getName).collect(Collectors.joining("."));

        // the page that will be moved
        setup.createPage(testReference, "Some content", "My Page");

        // for checking the link update
        setup.createPage(mainWikiLinkPage, String.format("[[%s.WebHome]]", space));

        // move the page to subwiki
        ViewPage viewPage = setup.gotoPage(testReference);
        RenamePage renamePage = viewPage.rename();
        renamePage.setUpdateLinks(true);
        renamePage.getDocumentPicker().setWiki(SUBWIKI_NAME);
        CopyOrRenameOrDeleteStatusPage renameStatusPage = renamePage.clickRenameButton().waitUntilFinished();

        // Ensure the move has been properly done
        assertEquals("Done.", renameStatusPage.getInfoMessage());
        DocumentReference movedPageReference = testReference.setWikiReference(new WikiReference(SUBWIKI_NAME));
        assertTrue(setup.rest().exists(movedPageReference));
        viewPage = renameStatusPage.gotoNewPage();
        assertEquals(String.format("/%s/%s/%s", SUBWIKI_NAME, testReference.getLastSpaceReference().extractFirstReference(
            EntityType.SPACE).getName(), "My Page"), viewPage.getBreadcrumbContent());
        assertEquals("Some content", viewPage.getContent());

        // Check the link is updated
        viewPage = setup.gotoPage(mainWikiLinkPage);
        WikiEditPage wikiEditPage = viewPage.editWiki();
        assertEquals(String.format("[[subwiki:%s.WebHome]]", space), wikiEditPage.getContent());
    }
}
