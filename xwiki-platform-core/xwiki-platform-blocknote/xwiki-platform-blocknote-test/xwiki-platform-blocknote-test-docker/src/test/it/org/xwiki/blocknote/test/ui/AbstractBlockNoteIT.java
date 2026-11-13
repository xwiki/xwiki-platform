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
package org.xwiki.blocknote.test.ui;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.xwiki.test.docker.junit5.MultiUserTestUtils;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Base class for all BlockNote integration tests.
 *
 * @version $Id$
 * @since 18.1.0RC1
 */
@UITest(
    properties = {
        // The Image Wizard needs this to be able to upload images.
        "xwikiCfgPlugins=com.xpn.xwiki.plugin.fileupload.FileUploadPlugin",

        // The page, attachment and image suggestions execute a Solr query from XWiki.SuggestSolrService /
        // XWiki.SuggestSolrMacros, which requires programming right. These pages are authored by a user that has
        // programming right in a normal wiki, but the test wiki blocks programming right by default, so we need to
        // explicitly exclude them.
        "xwikiPropertiesAdditionalProperties=test.prchecker.excludePattern=.*:XWiki\\.SuggestSolr(Service|Macros)"
    },
    extraJARs = {
        // The WebSocket end-point implementation based on XWiki components needs to be installed as core extension.
        "org.xwiki.platform:xwiki-platform-websocket",

        // The macro service uses the extension index script service to get the list of uninstalled macros (from
        // extensions) which expects an implementation of the extension index. The extension index script service is a
        // core extension so we need to make the extension index also core.
        "org.xwiki.platform:xwiki-platform-extension-index",

        // Solr search is used to get suggestions for the link and image quick actions.
        "org.xwiki.platform:xwiki-platform-search-solr-query"
    },
    servletEngineNetworkAliases = AbstractBlockNoteIT.XWIKI_ALIAS
)
abstract class AbstractBlockNoteIT
{
    public static final String XWIKI_ALIAS = "xwiki-alias";

    @BeforeAll
    static void beforeAll(TestUtils setup)
    {
        // Make Blocknote the default WYSIWYG editor.
        setup.loginAsSuperAdmin();
        setup.addObject("XWiki", "XWikiPreferences", "XWiki.EditorBindingClass", "dataType",
            "org.xwiki.rendering.syntax.SyntaxContent#wysiwyg", "roleHint", "blocknote");

        // Create a user without administration and script rights, then make it advanced in order to have access to the
        // edit dropdown and also to be able to edit in-place.
        setup.createUserAndLogin("John", "pass", "editor", "Wysiwyg", "usertype", "Advanced");

        // The BlockNote tests are often failing because clicking on the "Edit" goes to Wiki edit mode instead of
        // loading the in-place edit mode. The cause seems to be that we're not logged in as John. Adding an assert here
        // for debugging purposes, to catch the problem earlier.
        assertEquals("John", setup.getLoggedInUserName());
    }

    @AfterEach
    void afterEach(TestUtils setup, MultiUserTestUtils multiUserSetup)
    {
        // Handle the edit mode leave confirmation modal (when there are unsaved changes).
        setup.getDriver().getWindowHandles().forEach(handle -> {
            multiUserSetup.switchToBrowserTab(handle);
            setup.maybeLeaveEditMode();
        });

        multiUserSetup.closeTabs();
    }

    protected void loginAsJohn(TestUtils setup)
    {
        setup.login("John", "pass");
    }

    protected void loginAsAlice(TestUtils setup)
    {
        setup.createUserAndLogin("Alice", "pass", "editor", "Wysiwyg");
    }
}
