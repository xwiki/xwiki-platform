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
import org.openqa.selenium.Keys;
import org.xwiki.administration.test.po.AdministrationEditPage;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.docker.junit5.browser.Browser;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.CopyOrRenameOrDeleteStatusPage;
import org.xwiki.test.ui.po.CopyPage;
import org.xwiki.test.ui.po.SuggestInputElement;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.test.ui.po.editor.ClassEditPage;
import org.xwiki.test.ui.po.editor.ObjectEditPage;
import org.xwiki.test.ui.po.editor.ObjectEditPane;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests related to the ConfigurableClass feature.
 *
 * @version $Id$
 * @since 11.3RC1
 */
@UITest
public class ConfigurableClassIT
{
    @BeforeAll
    public void setup(TestUtils setup)
    {
        setup.loginAsSuperAdmin();
    }

    /*
     * If a value is specified for linkPrefix, then a link is generated with linkPrefix + prettyName of the property
     * from the configuration class.
     * linkPrefix = "http://www.xwiki.org/bin/view/Main/"
     * property prettyName = "WebHome"
     * generated link should equal "http://www.xwiki.org/bin/view/Main/WebHome"
     *
     * Tests: XWiki.ConfigurableClass
     */
    @Test
    public void labelLinkGeneration(TestUtils setup, TestReference testReference)
    {
        String space = testReference.getLastSpaceReference().getName();
        String page = testReference.getName();
        createConfigurableApplication(setup, space, page, "TestSection3", true);
        ObjectEditPage objectEditPage = ObjectEditPage.gotoPage(space, page);
        ObjectEditPane objectEditPane = objectEditPage.getObjectsOfClass("XWiki.ConfigurableClass").get(0);
        objectEditPane.setPropertyValue("linkPrefix", "TheLinkPrefix");
        objectEditPage.clickSaveAndView();

        AdministrationEditPage administrationEditPage = AdministrationEditPage.gotoPage("TestSection3");
        assertTrue(administrationEditPage.hasLink("TheLinkPrefixString"));
        assertTrue(administrationEditPage.hasLink("TheLinkPrefixBoolean"));
        assertTrue(administrationEditPage.hasLink("TheLinkPrefixTextArea"));
        assertTrue(administrationEditPage.hasLink("TheLinkPrefixSelect"));
    }

    /**
     * Creates a new page with a configuration class with some simple fields<br/>
     * then adds an object of class configurable and one of it's own class.<br/>
     * Tests: XWiki.ConfigurableClass
     */
    private void createConfigurableApplication(TestUtils setup, String space, String page, String section,
        boolean global)
    {
        // We have to use an existing space because the copy page form doesn't allow entering a new space.
        String storageSpace = "Sandbox";
        String storagePage = "CreateConfigurableApplication";

        if (!tryToCopyPage(setup, storageSpace, storagePage, space, page)) {
            // Create the page with a simple configuration class.
            setup.createPage(space, page, "Test configurable application", "Configurable App");
            ClassEditPage classEditPage = ClassEditPage.gotoPage(space, page);
            classEditPage.addProperty("String", "String");
            classEditPage.addProperty("Boolean", "Boolean");
            classEditPage.addProperty("TextArea", "TextArea");
            // Expand the TextArea property we just added in order to set the "editor" meta property.
            classEditPage.getPropertyEditPane("TextArea")
                .expand()
                .setMetaProperty("editor", "Text");
            classEditPage.clickSaveAndContinue();
            classEditPage.addProperty("Select", "StaticList");

            // Go to the object section.
            ObjectEditPage objectEditPage = ObjectEditPage.gotoPage(space, page);
            // Add a configurable object which points to the new class as the configuration class.
            objectEditPage.addObject("XWiki.ConfigurableClass");
            objectEditPage.clickSaveAndView();

            // Try to place it in the storage area.
            tryToCopyPage(setup, space, page, storageSpace, storagePage);
        }

        // Go to the object section.
        ObjectEditPage objectEditPage = ObjectEditPage.gotoPage(space, page);

        // Add an object of the new class.
        objectEditPage.addObject(space + "." + page);
        ObjectEditPane objectEditPane = objectEditPage.getObjectsOfClass("XWiki.ConfigurableClass").get(0);
        objectEditPane.setPropertyValue("displayInSection", section)
            .setPropertyValue("heading", "Some Heading");

        SuggestInputElement configurationSuggest = objectEditPane.getSuggestInput("configurationClass");
        configurationSuggest.sendKeys(space + "." + page).waitForSuggestions().sendKeys(Keys.ENTER);

        // Unfold the XWiki.ConfigurableClass object so that we can modify its properties
        if (global) {
            objectEditPane.setPropertyValue("configureGlobally", "true");
        } else {
            objectEditPane.setPropertyValue("configureGlobally", "false");
        }
        // We won't set linkPrefix, propertiesToShow, codeToExecute, or iconAttachment.

        objectEditPage.clickSaveAndView();
    }

    /**
     * This is used by createConfigurableApplication to store a copy of the default configurable to speed up making
     * them.
     */
    public boolean tryToCopyPage(TestUtils setup, String fromSpace, String fromPage, String toSpace, String toPage)
    {
        ViewPage viewPage = setup.gotoPage(fromSpace, fromPage);
        if (!viewPage.exists()) {
            return false;
        }
        CopyPage copyPage = viewPage.copy();
        copyPage.setTargetSpaceName(toSpace);
        copyPage.setTargetPageName(toPage);
        CopyOrRenameOrDeleteStatusPage copyOrRenameOrDeleteStatusPage = copyPage.clickCopyButton();
        return copyOrRenameOrDeleteStatusPage.waitUntilFinished().getInfoMessage().equals("Done.");
    }
}
