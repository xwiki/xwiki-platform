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
package org.xwiki.icon.test.ui;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.openqa.selenium.By;
import org.xwiki.administration.test.po.AdministrablePage;
import org.xwiki.administration.test.po.AdministrationPage;
import org.xwiki.administration.test.po.ThemesAdministrationSectionPage;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Functional tests of the icon theme Admin features.
 *
 * @version $Id$
 * @since 16.10.0RC1
 */
@UITest
class IconThemesAdminIT
{
  private static final String SILK_THEME = "Silk";
  private static final String FA_THEME = "Font Awesome";
  
  @Test
  void validateIconThemeFeatures(TestUtils setup, TestInfo info)
  {
    setup.loginAsSuperAdmin();
    
    // Validate setting a icon theme from the wiki Admin UI
    validateSetThemeFromWikiAdminUI(setup);

    // Validate setting a icon theme from the page Admin UI for the page and its children
    validateSetThemeFromPageAdminUI(setup, info);
    
  }
  
  private void validateSetThemeFromWikiAdminUI(TestUtils setup)
  {
    // Go to the Theme Admin UI to verify we can set a new theme from there using the select control
    AdministrationPage administrationPage = AdministrationPage.gotoPage();
    ThemesAdministrationSectionPage presentationAdministrationSectionPage = administrationPage.clickThemesSection();
    // We expect the default icon to be Silk
    assertFalse(iconThemeIsFA(setup));

    // Set the new icon theme as the active theme
    presentationAdministrationSectionPage.setIconTheme(FA_THEME);
    assertEquals(FA_THEME, presentationAdministrationSectionPage.getCurrentIconTheme());
    presentationAdministrationSectionPage.clickSave();

    // Verify that the icon theme has been applied.
    assertTrue(iconThemeIsFA(setup));

    // Switch back to Silk
    administrationPage = AdministrationPage.gotoPage();
    presentationAdministrationSectionPage = administrationPage.clickThemesSection();
    presentationAdministrationSectionPage.setIconTheme(SILK_THEME);
    presentationAdministrationSectionPage.clickSave();
  }

  private void validateSetThemeFromPageAdminUI(TestUtils setup, TestInfo info)
  {
    // Create two nested pages. We'll apply the theme on the top page and verify that the nested page has it too.
    DocumentReference topPage = new DocumentReference("xwiki", info.getTestClass().get().getSimpleName() +
            "Parent", "WebHome");
    DocumentReference childPage = new DocumentReference("xwiki", Arrays.asList(
            info.getTestClass().get().getSimpleName() + "Parent",
            info.getTestClass().get().getSimpleName() + "Child"), "WebHome");
    setup.deletePage(topPage, true);
    setup.createPage(childPage, "top page");
    setup.createPage(topPage, "top page");
    AdministrablePage ap = new AdministrablePage();

    // Navigate to the top page's admin UI.
    AdministrationPage page = ap.clickAdministerPage();
    ThemesAdministrationSectionPage presentationAdministrationSectionPage = page.clickThemesSection();

    // Set the newly created icon theme as the active theme for the page and children
    presentationAdministrationSectionPage.setIconTheme(FA_THEME);
    assertEquals(FA_THEME, presentationAdministrationSectionPage.getCurrentIconTheme());
    presentationAdministrationSectionPage.clickSave();

    // Verify that the icon theme has been applied to the top page
    setup.gotoPage(topPage);
    assertTrue(iconThemeIsFA(setup));

    // Verify that the icon theme has been applied to the children page
    setup.gotoPage(childPage);
    assertTrue(iconThemeIsFA(setup));

    /*Possible extension of the test:
     Verify that the icon theme has not been applied to other pages*/
  }

  private boolean iconThemeIsFA(TestUtils setup)
  {
    return setup.getDriver().findElementWithoutScrolling(By.cssSelector("#hierarchy_breadcrumb .dropdown .fa.fa-home")).isDisplayed();
  }
}