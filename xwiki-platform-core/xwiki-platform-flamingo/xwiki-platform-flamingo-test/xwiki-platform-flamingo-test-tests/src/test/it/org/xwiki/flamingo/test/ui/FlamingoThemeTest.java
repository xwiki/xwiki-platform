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
package org.xwiki.flamingo.test.ui;

import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.xwiki.administration.test.po.AdministrationPage;
import org.xwiki.administration.test.po.PresentationAdministrationSectionPage;
import org.xwiki.flamingo.test.po.EditThemePage;
import org.xwiki.flamingo.test.po.PreviewBox;
import org.xwiki.test.ui.AbstractTest;
import org.xwiki.test.ui.SuperAdminAuthenticationRule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * UI tests for the Flamingo Theme Application.
 *
 * @version $Id$
 * @since 6.3M1
 */
public class FlamingoThemeTest extends AbstractTest
{
    @Rule
    public SuperAdminAuthenticationRule superAdminAuthenticationRule =
            new SuperAdminAuthenticationRule(getUtil(), getDriver());
    @Test
    public void editFlamingoTheme() throws Exception
    {
        // Go to the presentation section of the administration
        AdministrationPage administrationPage = AdministrationPage.gotoPage();
        PresentationAdministrationSectionPage presentationAdministrationSectionPage =
                administrationPage.clickPresentationSection();

        // Select the 'Charcoal' color theme
        presentationAdministrationSectionPage.setColorTheme("Charcoal");
        assertEquals("Charcoal", presentationAdministrationSectionPage.getCurrentColorTheme());

        // Click on the 'customize' button
        presentationAdministrationSectionPage.clickOnCustomize();
        EditThemePage editThemePage = new EditThemePage();

        verifyAllVariablesCategoriesArePresent(editThemePage);
        verifyThatPreviewWorks(editThemePage);
    }

    private void verifyAllVariablesCategoriesArePresent(EditThemePage editThemePage) throws Exception
    {
        List<String> categories = editThemePage.getVariableCategories();
        assertEquals(10, categories.size());
        assertTrue(categories.contains("Logos"));
        assertTrue(categories.contains("Base colors"));
        assertTrue(categories.contains("Typography"));
        assertTrue(categories.contains("Buttons"));
        assertTrue(categories.contains("Navigation Bar"));
        assertTrue(categories.contains("Drop downs"));
        assertTrue(categories.contains("Forms"));
        assertTrue(categories.contains("Panels"));
        assertTrue(categories.contains("Breadcrumb"));
        assertTrue(categories.contains("Advanced"));
    }

    /**
     * @since 6.3M2
     */
    private void verifyThatPreviewWorks(EditThemePage editThemePage) throws Exception
    {
        // Wait for the preview to be fully loaded
        assertTrue(editThemePage.isPreviewBoxLoading());
        editThemePage.waitUntilPreviewIsLoaded();
        // Disable auto refresh first
        editThemePage.setAutoRefresh(false);
        // Select a variable category and change value
        editThemePage.selectVariableCategory("Base colors");
        editThemePage.setVariableValue("xwiki-page-content-bg", "#ff0000");
        // Again...
        editThemePage.selectVariableCategory("Typography");
        editThemePage.setVariableValue("font-family-base", "Monospace");
        // Refresh
        editThemePage.refreshPreview();
        // Verify that the modification have been made in the preview
        PreviewBox previewBox = editThemePage.getPreviewBox();
        assertEquals("rgba(255, 0, 0, 1)", previewBox.getPageBackgroundColor());
        assertEquals("monospace", previewBox.getFontFamily());
    }
}
