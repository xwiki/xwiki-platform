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
package org.xwiki.skin.test.ui;

import org.junit.jupiter.api.Test;
import org.xwiki.administration.test.po.AdministrationPage;
import org.xwiki.administration.test.po.ThemesAdministrationSectionPage;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.skin.test.po.SkinInlinePage;
import org.xwiki.skin.test.po.SkinTemplateElement;
import org.xwiki.skin.test.po.SkinViewPage;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verify the behavior of wiki based skins.
 *
 * @version $Id$
 */
@UITest
class WikiSkinIT
{
    /**
     * Make sure it's possible to provide a template as xobject in a wiki skin.
     */
    @Test
    void modifySkinObjectTemplate(TestUtils setup) throws Exception
    {
        setup.loginAsSuperAdmin();

        // Set the default skin
        AdministrationPage adminPage = AdministrationPage.gotoPage();
        ThemesAdministrationSectionPage themesPage = adminPage.clickThemesSection();
        themesPage.setSkin("XWiki.DefaultSkin");
        themesPage.clickSave();

        // Customize the skin
        themesPage.clickOnCustomizeSkin();
        SkinViewPage viewSkinPage = new SkinViewPage();

        // Add a custom template
        SkinInlinePage editSkinPage = viewSkinPage.editSkin();
        SkinTemplateElement templateElement = editSkinPage.addTemplate("test.vm");
        templateElement.setContent("content");
        editSkinPage.clickSaveAndContinue();

        // Use the template
        assertEquals("content", setup.executeWiki("{{template name='test.vm'/}}", Syntax.XWIKI_2_1));

        // Modify the template
        adminPage = AdministrationPage.gotoPage();
        themesPage = adminPage.clickThemesSection();
        themesPage.clickOnCustomizeSkin();
        viewSkinPage = new SkinViewPage();
        editSkinPage = viewSkinPage.editSkin();
        templateElement = editSkinPage.getTemplate("test.vm", true);
        templateElement.setContent("modified content");
        editSkinPage.clickSaveAndContinue();

        // Make sure the template result changes
        assertEquals("modified content", setup.executeWiki("{{template name='test.vm'/}}", Syntax.XWIKI_2_1));
    }
}
