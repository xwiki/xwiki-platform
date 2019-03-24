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
import org.junit.jupiter.api.Test;
import org.xwiki.administration.test.po.AdministrationSectionPage;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.docker.junit5.servletengine.ServletEngine;
import org.xwiki.test.ui.TestUtils;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests related to the ConfigurableClass feature.
 *
 * @version $Id$
 * @since 11.3RC1
 */
@UITest(servletEngine = ServletEngine.TOMCAT)
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

    private void setupConfigurableApplication(TestUtils setup, DocumentReference testReference,
        Object... configurableClassProperties)
    {
        setup.deletePage(testReference);

        // Create the page with a simple configuration class.
        setup.createPage(testReference, "Test configurable application", "Configurable App");
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
