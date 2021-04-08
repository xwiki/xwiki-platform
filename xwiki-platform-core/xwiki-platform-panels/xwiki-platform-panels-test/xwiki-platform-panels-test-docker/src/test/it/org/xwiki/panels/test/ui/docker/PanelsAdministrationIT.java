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
package org.xwiki.panels.test.ui.docker;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xwiki.administration.test.po.AdministrationPage;
import org.xwiki.panels.test.po.PageLayoutTabContent;
import org.xwiki.panels.test.po.PageWithPanels;
import org.xwiki.panels.test.po.PanelsAdministrationPage;
import org.xwiki.panels.test.po.PanelsHomePage;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.test.ui.po.editor.WikiEditPage;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verify the Panels Administration features.
 *
 * @version $Id$
 * @since 11.3RC1
 */
@UITest
public class PanelsAdministrationIT
{
    @BeforeAll
    public void setup(TestUtils setup) throws Exception
    {
        setup.loginAsSuperAdmin();

        // Reset the right and left panels.
        setup.setWikiPreference("rightPanels", "");
        setup.setWikiPreference("leftPanels", "");
    }

    @Order(1)
    @Test
    public void verifyPanelWizardPresentInAdministration()
    {
        AdministrationPage administrationPage = AdministrationPage.gotoPage();
        assertTrue(administrationPage.hasSection("Panels.PanelWizard"));

        // Select space administration (XWiki space, since that space exists)
        AdministrationPage spaceAdministrationPage = AdministrationPage.gotoSpaceAdministrationPage("XWiki");

        // The Panel Wizard should be available in the space administration.
        assertTrue(spaceAdministrationPage.hasSection("Panels.PanelWizard"));
    }

    @Order(2)
    @Test
    public void tabsExist()
    {
        PanelsAdministrationPage panelsAdministrationPage = PanelsAdministrationPage.gotoPage();
        assertTrue(panelsAdministrationPage.hasPageLayoutTab());
        assertTrue(panelsAdministrationPage.hasPanelListTab());
    }

    @Order(3)
    @Test
    public void pageLayout()
    {
        PanelsAdministrationPage panelsAdminPage = PanelsAdministrationPage.gotoPage();
        PageLayoutTabContent pageLayoutTabContent = panelsAdminPage.selectPageLayout();
        pageLayoutTabContent = pageLayoutTabContent.selectNoSideColumnLayout();
        assertFalse(pageLayoutTabContent.isLeftPanelVisible());
        assertFalse(pageLayoutTabContent.isRightPanelVisible());

        pageLayoutTabContent = pageLayoutTabContent.selectLeftColumnLayout();
        assertTrue(pageLayoutTabContent.isLeftPanelVisible());
        assertFalse(pageLayoutTabContent.isRightPanelVisible());

        pageLayoutTabContent = pageLayoutTabContent.selectRightColumnLayout();
        assertFalse(pageLayoutTabContent.isLeftPanelVisible());
        assertTrue(pageLayoutTabContent.isRightPanelVisible());

        pageLayoutTabContent = pageLayoutTabContent.selectBothColumnsLayout();
        assertTrue(pageLayoutTabContent.isLeftPanelVisible());
        assertTrue(pageLayoutTabContent.isRightPanelVisible());
    }

    @Order(4)
    @Test
    public void gotoPanels()
    {
        PanelsAdministrationPage panelsAdminPage = PanelsAdministrationPage.gotoPage();
        assertTrue(panelsAdminPage.hasGotoPanelsLink());
        PanelsHomePage panelsHomePage = panelsAdminPage.clickGotoPanels();
        assertTrue(panelsHomePage.exists());
    }

    @Order(5)
    @Test
    public void resetPanels()
    {
        PanelsAdministrationPage panelsAdminPage = PanelsAdministrationPage.gotoPage();
        assertTrue(panelsAdminPage.hasResetLink());

        PageLayoutTabContent pageLayoutTabContent = panelsAdminPage.selectPageLayout();
        pageLayoutTabContent = pageLayoutTabContent.selectLeftColumnLayout().openPanelListSection();
        assertTrue(pageLayoutTabContent.isLeftPanelVisible());
        assertFalse(pageLayoutTabContent.isRightPanelVisible());
        pageLayoutTabContent.dragPanelToColumn("QuickLinks", PageLayoutTabContent.Column.LEFT);
        assertTrue(new PageWithPanels().hasPanelInLeftColumn("QuickLinks"));

        panelsAdminPage.clickResetLink();
        pageLayoutTabContent = panelsAdminPage.selectPageLayout();
        assertTrue(pageLayoutTabContent.isLayoutSelected(PageLayoutTabContent.Layout.NOCOLUMN));
        assertFalse(pageLayoutTabContent.isLeftPanelVisible());
        assertFalse(pageLayoutTabContent.isRightPanelVisible());
        assertFalse(new PageWithPanels().hasPanelInLeftColumn("QuickLinks"));
    }

    @Order(6)
    @Test
    public void verifyPanelWizard(TestUtils setup, TestReference testReference)
    {
        PanelsAdministrationPage panelsAdminPage = PanelsAdministrationPage.gotoPage();
        PageLayoutTabContent pageLayoutTabContent = panelsAdminPage.selectPageLayout();
        // create a right column layout
        pageLayoutTabContent.selectRightColumnLayout().openPanelListSection();
        PageWithPanels pageWithPanels = new PageWithPanels();
        assertTrue(pageWithPanels.hasRightPanels());

        // put quicklinks to right column
        pageLayoutTabContent.dragPanelToColumn("QuickLinks", PageLayoutTabContent.Column.RIGHT);
        assertTrue(new PageWithPanels().hasPanelInRightColumn("QuickLinks"));
        panelsAdminPage.clickSave();

        setup.gotoPage("Main", "WebHome");
        pageWithPanels = new PageWithPanels();
        assertFalse(pageWithPanels.hasLeftPanels());
        assertTrue(pageWithPanels.hasRightPanels());
        assertTrue(pageWithPanels.hasPanelInRightColumn("QuickLinks"));

        // create a both columns layout
        panelsAdminPage = PanelsAdministrationPage.gotoPage();
        pageLayoutTabContent = panelsAdminPage.selectPageLayout();
        pageLayoutTabContent.selectBothColumnsLayout().openPanelListSection();
        pageWithPanels = new PageWithPanels();
        assertTrue(pageWithPanels.hasRightPanels());
        assertTrue(pageWithPanels.hasLeftPanels());

        // remove quicklinks from right panel
        pageLayoutTabContent.removePanelFromColumn("QuickLinks", PageLayoutTabContent.Column.RIGHT);
        pageWithPanels = new PageWithPanels();
        assertFalse(pageWithPanels.hasPanelInRightColumn("QuickLinks"));
        panelsAdminPage.clickSave();

        // columns are not visible anymore since there's nothing in them
        setup.gotoPage("Main", "WebHome");
        pageWithPanels = new PageWithPanels();
        assertFalse(pageWithPanels.hasLeftPanels());
        assertFalse(pageWithPanels.hasRightPanels());
        assertFalse(pageWithPanels.hasPanelInRightColumn("QuickLinks"));

        // put panels in both left and right columns
        panelsAdminPage = PanelsAdministrationPage.gotoPage();
        pageLayoutTabContent = panelsAdminPage.selectPageLayout();
        assertTrue(pageLayoutTabContent.isLayoutSelected(PageLayoutTabContent.Layout.NOCOLUMN));
        pageLayoutTabContent.selectBothColumnsLayout().openPanelListSection();
        pageWithPanels = new PageWithPanels();
        assertTrue(pageWithPanels.hasRightPanels());
        assertTrue(pageWithPanels.hasLeftPanels());
        pageLayoutTabContent.dragPanelToColumn("Welcome", PageLayoutTabContent.Column.RIGHT);
        pageLayoutTabContent.dragPanelToColumn("Applications", PageLayoutTabContent.Column.LEFT);
        panelsAdminPage.clickSave();

        setup.gotoPage("Main", "WebHome");
        pageWithPanels = new PageWithPanels();
        assertTrue(pageWithPanels.hasLeftPanels());
        assertTrue(pageWithPanels.hasRightPanels());
        assertTrue(pageWithPanels.hasPanelInRightColumn("Welcome"));
        assertTrue(pageWithPanels.hasPanelInLeftColumn("Applications"));

        // test panel wizard at space level
        ViewPage viewPage = setup.gotoPage(testReference);
        WikiEditPage wikiEditPage = viewPage.editWiki();
        wikiEditPage.setContent("aaa");
        wikiEditPage.clickSaveAndView();

        AdministrationPage.gotoSpaceAdministrationPage(testReference.getLastSpaceReference())
            .getAdministrationMenu().expandCategoryWithName("Look & Feel")
            .getSectionByName("Look & Feel", "Panels")
            .click();
        panelsAdminPage = new PanelsAdministrationPage();
        pageLayoutTabContent = panelsAdminPage.selectPageLayout().selectLeftColumnLayout().openPanelListSection();
        pageLayoutTabContent.dragPanelToColumn("QuickLinks", PageLayoutTabContent.Column.LEFT);
        panelsAdminPage.clickSave();

        setup.gotoPage(testReference);
        pageWithPanels = new PageWithPanels();
        assertTrue(pageWithPanels.hasLeftPanels());
        assertFalse(pageWithPanels.hasRightPanels());
        assertTrue(pageWithPanels.hasPanelInLeftColumn("QuickLinks"));
        assertTrue(pageWithPanels.hasPanelInLeftColumn("Applications"));

        setup.gotoPage("Main", "WebHome");
        pageWithPanels = new PageWithPanels();
        assertTrue(pageWithPanels.hasLeftPanels());
        assertTrue(pageWithPanels.hasRightPanels());
        assertTrue(pageWithPanels.hasPanelInRightColumn("Welcome"));
        assertTrue(pageWithPanels.hasPanelInLeftColumn("Applications"));
        assertFalse(pageWithPanels.hasPanelInRightColumn("QuickLinks"));
    }
}
