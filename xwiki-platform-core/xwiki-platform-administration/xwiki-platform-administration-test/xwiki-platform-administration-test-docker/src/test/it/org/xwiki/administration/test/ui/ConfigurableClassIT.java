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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.xwiki.administration.test.po.AdministrationSectionPage;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.FormContainerElement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
    @BeforeEach
    public void setUp(TestUtils setup)
    {
        setup.loginAsSuperAdmin();
    }
    /*
     * Verify that if a value is specified for the {@code linkPrefix} xproperty, then a link is generated with
     * linkPrefix + prettyName of the property from the configuration class.
     */
    @Test
    @Order(1)
    public void labelLinkGeneration(TestUtils setup, TestReference testReference)
    {
        // Fixture
        setupConfigurableApplication(setup, testReference,
            "displayInSection", "TestSection",
            "heading", "Some Heading",
            "configureGlobally", "true",
            "configurationClass", setup.serializeReference(testReference),
            "linkPrefix", "TheLinkPrefix");

        // Check that the links are there and contain the expected values
        AdministrationSectionPage asp = AdministrationSectionPage.gotoPage("TestSection");
        assertTrue(asp.hasLink("TheLinkPrefixString"));
        assertTrue(asp.hasLink("TheLinkPrefixBoolean"));
        assertTrue(asp.hasLink("TheLinkPrefixTextArea"));
        assertTrue(asp.hasLink("TheLinkPrefixSelect"));
    }

    /*
     * Creates a document with 2 configurable objects, one gets configured globally in one section and displays
     * 2 configuration fields, the other is configured in the space in another section and displays the other 2
     * fields. Fails if they are not displayed as they should be.
     *
     * Tests: XWiki.ConfigurableClass
     */
    @Test
    @Order(2)
    public void testApplicationConfiguredInMultipleSections(TestUtils setup, TestReference testReference)
    {
        // Fixture
        setupConfigurableApplication(setup, testReference,
            "displayInSection", "TestSection1",
            "configureGlobally", "true",
            "heading", "Some Heading",
            "configurationClass", setup.serializeReference(testReference),
            "propertiesToShow", "String, Boolean");

        setup.addObject(testReference, "XWiki.ConfigurableClass",
            "displayInSection", "TestSection2",
            "configureGlobally", "false",
            "heading", "Some Other Heading",
            "configurationClass", setup.serializeReference(testReference),
            "propertiesToShow", "TextArea, Select");

        String fullName = setup.serializeReference(testReference).split(":")[1];

        // Assert that half of the configuration shows up but not the other half.
        AdministrationSectionPage asp = AdministrationSectionPage.gotoPage("TestSection1");
        asp.hasHeading(2, "HSomeHeading");
        // Save button
        // Javascript injects a save button outside of the form and removes the default save button.
        setup.getDriver().waitUntilElementIsVisible(By.xpath(
            "//div/div/p/span/input[@type='submit'][@value='Save']"));

        FormContainerElement formContainerElement = asp.getFormContainerElement();

        // Form and fields
        assertEquals(String.format("%ssave/%s", setup.getBaseBinURL(), fullName.replace('.', '/')),
            formContainerElement.getFormAction());
        assertEquals(setup.getDriver().getCurrentUrl(),
            formContainerElement.getFieldValue(By.name("xredirect")));
        assertTrue(formContainerElement.hasField(By.name(fullName + "_0_String")));
        assertTrue(formContainerElement.hasField(By.name(fullName + "_0_Boolean")));
        assertTrue(formContainerElement.hasField(By.id(fullName + "_redirect")));

        // Should not be there
        assertFalse(formContainerElement.hasField(By.name(fullName + "_0_TextArea")));
        assertFalse(formContainerElement.hasField(By.name(fullName + "_0_Select")));

        // Now we go to where the other half of the configuration should be.
        asp = AdministrationSectionPage.gotoSpaceAdministration(testReference.getLastSpaceReference(), "TestSection2");
        asp.hasHeading(2, "HSomeOtherHeading");
        // Save button
        // Javascript injects a save button outside of the form and removes the default save button.
        setup.getDriver().waitUntilElementIsVisible(By.xpath(
            "//div/div/p/span/input[@type='submit'][@value='Save']"));

        formContainerElement = asp.getFormContainerElement();

        // Form and fields
        assertEquals(String.format("%ssave/%s", setup.getBaseBinURL(), fullName.replace('.', '/')),
            formContainerElement.getFormAction());
        assertEquals(setup.getDriver().getCurrentUrl(),
            formContainerElement.getFieldValue(By.name("xredirect")));
        assertTrue(formContainerElement.hasField(By.name(fullName + "_0_TextArea")));
        assertTrue(formContainerElement.hasField(By.name(fullName + "_0_Select")));
        assertTrue(formContainerElement.hasField(By.id(fullName + "_redirect")));

        // Should not be there
        assertFalse(formContainerElement.hasField(By.name(fullName + "_0_String")));
        assertFalse(formContainerElement.hasField(By.name(fullName + "_0_Boolean")));
    }

    private void setupConfigurableApplication(TestUtils setup, DocumentReference testReference,
        Object... configurableClassProperties)
    {
        setup.deletePage(testReference);

        // Create the page with a simple configuration class.
        setup.createPage(testReference, "Test configurable application", testReference.getName());
        setup.addClassProperty(testReference, "String", "String");
        setup.addClassProperty(testReference, "Boolean", "Boolean");
        setup.addClassProperty(testReference, "TextArea", "TextArea");

        // Set the editor to Text and the select to static list
        setup.updateClassProperty(testReference, "TextArea_editor", "Text");

        setup.addClassProperty(testReference, "Select", "StaticList");

        // Add a ConfigurableClass xobject.
        setup.addObject(testReference, "XWiki.ConfigurableClass", configurableClassProperties);

        // Add an xobject of the new class.
        setup.addObject(testReference, setup.serializeReference(testReference));
    }
}
