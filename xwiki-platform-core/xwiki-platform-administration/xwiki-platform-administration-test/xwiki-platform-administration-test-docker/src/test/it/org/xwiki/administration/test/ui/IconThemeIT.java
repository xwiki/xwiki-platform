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
package org.xwiki.administration.test.ui;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.xwiki.administration.test.po.AdministrationPage;
import org.xwiki.administration.test.po.ThemesAdministrationSectionPage;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.ViewPage;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Validate that the icon theme can be changed from the Themes administration section (at space level) and that the
 * change is inherited by child pages.
 *
 * @version $Id$
 * @since 18.6.0RC1
 */
@UITest
class IconThemeIT
{
    /**
     * Content that renders the {@code add} icon using the currently configured icon theme. With the Font Awesome
     * theme this produces a {@code <span class="fa fa-plus">} element while with the Silk theme this produces an
     * {@code <img src=".../icons/silk/add.png">} element.
     */
    private static final String ICON_CONTENT = "{{displayIcon name=\"add\"/}}";

    @BeforeAll
    void beforeAll(TestUtils setup)
    {
        setup.loginAsSuperAdmin();
    }

    @Test
    void changeIconThemeFromSpaceAdministration(TestUtils setup, TestReference testReference)
    {
        // Create a parent page 'a' and a child page 'a.b', both rendering an icon, so that we can observe the icon
        // theme used and the inheritance from the parent space to the child page.
        SpaceReference parentSpace = new SpaceReference("a", testReference.getLastSpaceReference());
        DocumentReference parentPage = new DocumentReference("WebHome", parentSpace);
        DocumentReference parentPreferences = new DocumentReference("WebPreferences", parentSpace);
        DocumentReference childPage =
            new DocumentReference("WebHome", new SpaceReference("b", parentSpace));
        setup.createPage(parentPage, ICON_CONTENT, "a");
        setup.createPage(childPage, ICON_CONTENT, "b");

        // Establish a deterministic baseline by setting the Font Awesome icon theme on the parent space (the icon
        // theme is not configured by default in the test instance).
        setup.updateObject(parentPreferences, "XWiki.XWikiPreferences", 0, "iconTheme", "IconThemes.FontAwesome");

        // The parent page uses Font Awesome (font-based icons).
        ViewPage viewPage = setup.gotoPage(parentPage);
        assertTrue(viewPage.isFontAwesomeIconDisplayedInContent(),
            "The parent page should use the Font Awesome icon theme before the change");

        // Change the icon theme to Silk from the parent space administration.
        ThemesAdministrationSectionPage themesPage =
            AdministrationPage.gotoSpaceAdministrationPage(parentSpace).clickThemesSection();
        themesPage.setIconTheme("Silk");
        themesPage.clickSave();

        // The parent page now uses Silk (image-based icons).
        viewPage = setup.gotoPage(parentPage);
        assertTrue(viewPage.isSilkIconDisplayedInContent(),
            "The parent page should use the Silk icon theme after the change");
        assertFalse(viewPage.isFontAwesomeIconDisplayedInContent(),
            "The parent page should no longer use the Font Awesome icon theme after the change");

        // The child page inherits the Silk icon theme from its parent space.
        viewPage = setup.gotoPage(childPage);
        assertTrue(viewPage.isSilkIconDisplayedInContent(),
            "The child page should inherit the Silk icon theme from its parent space");
    }
}
