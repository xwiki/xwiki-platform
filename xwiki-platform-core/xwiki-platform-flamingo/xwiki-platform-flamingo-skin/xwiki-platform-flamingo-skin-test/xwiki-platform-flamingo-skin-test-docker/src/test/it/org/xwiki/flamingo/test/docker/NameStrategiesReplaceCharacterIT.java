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
package org.xwiki.flamingo.test.docker;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.validation.test.po.NameStrategiesAdministrationSectionPage;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rest.model.jaxb.Objects;
import org.xwiki.rest.model.jaxb.Page;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.TestUtils.RestTestUtils;
import org.xwiki.test.ui.po.CreatePagePage;
import org.xwiki.test.ui.po.DocumentPicker;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.test.ui.po.editor.EditPage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verify the "Character Replacement" entity name validation strategy end to end: a custom {@code "@"} to {@code "-"}
 * mapping is configured, the automatic transformation and validation options are set through the Name Strategies
 * administration section, and the effect is checked by creating a page whose name contains the forbidden character.
 * <p>
 * The strategy is configured with its own forbidden character (nothing is inherited from the standard flavor, which is
 * not installed here). The mapping is seeded directly through the REST API rather than through the administration's
 * "Add new character" UI, because that UI relies on the configuration document (with its list properties) shipped by
 * the flavor, which is absent from this WAR. The {@code ReplaceCharacterEntityNameValidation} strategy caches its
 * replacement map in a singleton and only reloads it from {@code EntityNameValidationManager.resetStrategies()} (the
 * "Add new character" UI is the only production caller); the REST seed alone does not refresh it, so {@code beforeAll}
 * calls {@code resetStrategies()} explicitly once the mapping has been seeded.
 *
 * @version $Id$
 * @since 18.6.0RC1
 */
@UITest
class NameStrategiesReplaceCharacterIT
{
    private static final String STRATEGY = "ReplaceCharacterEntityNameValidation";

    private static final String FORBIDDEN_NAME = "Te@st";

    private static final String TRANSFORMED_NAME = "Te-st";

    @BeforeAll
    void beforeAll(TestUtils setup) throws Exception
    {
        setup.loginAsSuperAdmin();

        // Configure the Character Replacement strategy with a custom "@" -> "-" mapping directly through the REST API.
        // The administration's "Add new character" UI is not used because it requires the configuration document (with
        // its list properties) shipped by the standard flavor, which is not installed in this test's WAR.
        DocumentReference configReference =
            new DocumentReference("xwiki", List.of("XWiki", "EntityNameValidation"), "Configuration");
        Page configPage = setup.rest().page(configReference);
        Objects objects = new Objects();
        configPage.setObjects(objects);
        org.xwiki.rest.model.jaxb.Object configObject =
            RestTestUtils.object("XWiki.EntityNameValidation.ConfigurationClass");
        configObject.setNumber(0);
        // The forbidden and replacement characters are single-value StaticListClass properties here; they are zipped by
        // index into the "@" -> "-" replacement map.
        configObject.withProperties(RestTestUtils.property("currentStrategy", STRATEGY));
        configObject.withProperties(RestTestUtils.property("replaceCharacters.forbiddenCharacters", "@"));
        configObject.withProperties(RestTestUtils.property("replaceCharacters.replacementCharacters", "-"));
        objects.withObjectSummaries(configObject);
        setup.rest().save(configPage);

        // The ReplaceCharacterEntityNameValidation strategy caches its replacement map at component initialisation and
        // is only refreshed by EntityNameValidationManager.resetStrategies(). In this shared-instance test run the
        // singleton has already been initialised (with an empty map) by an earlier test, and neither the REST save
        // above nor the administration form save in the tests below refreshes it. Force the reload so the seeded
        // "@" -> "-" mapping is actually used.
        setup.executeWikiPlain("{{velocity}}$services.modelvalidation.manager.resetStrategies(){{/velocity}}",
            Syntax.XWIKI_2_1);
    }

    /**
     * With the automatic transformation enabled, creating a page whose name contains the forbidden character must
     * automatically replace it in the page name, while the page title is kept as typed.
     */
    @Test
    @Order(1)
    void transformNameAutomatically(TestUtils setup, TestReference reference)
    {
        SpaceReference spaceReference = reference.getLastSpaceReference();
        setup.deleteSpace(spaceReference);

        // Enable the automatic transformation and disable the validation through the administration UI.
        NameStrategiesAdministrationSectionPage section = NameStrategiesAdministrationSectionPage.gotoPage();
        assertEquals(STRATEGY, section.getSelectedStrategy());
        section.setTransformNameAutomatically(true);
        section.setValidateNames(false);
        section.save();

        // Typing a title that contains the forbidden character makes the create form automatically derive a page name
        // where the strategy has replaced it ("Te@st" -> "Te-st"), while the title is kept as typed.
        CreatePagePage createPage = openCreateFormInTestSpace(setup, spaceReference);
        DocumentPicker picker = createPage.getDocumentPicker();
        picker.setTitle(FORBIDDEN_NAME);
        picker.waitForName(TRANSFORMED_NAME);
        createPage.setTerminalPage(true);
        createPage.clickCreate();

        // The name has been transformed while the title has been kept as typed.
        ViewPage savedPage = new EditPage().clickSaveAndView();
        assertEquals(TRANSFORMED_NAME, savedPage.getMetaDataValue("page"));
        assertEquals(FORBIDDEN_NAME, savedPage.getDocumentTitle());
    }

    /**
     * With the validation enabled (and the automatic transformation disabled), creating a page whose name contains the
     * forbidden character must be rejected with an error message and the page must not be created.
     */
    @Test
    void validateNamesBeforeSaving(TestUtils setup, TestReference reference) throws Exception
    {
        SpaceReference spaceReference = reference.getLastSpaceReference();
        setup.deleteSpace(spaceReference);

        // Disable the automatic transformation and enable the validation through the administration UI.
        NameStrategiesAdministrationSectionPage section = NameStrategiesAdministrationSectionPage.gotoPage();
        assertEquals(STRATEGY, section.getSelectedStrategy());
        section.setTransformNameAutomatically(false);
        section.setValidateNames(true);
        section.save();

        // With the automatic transformation disabled, the create form keeps the forbidden character in the derived
        // page name, so it reaches the server where the validation rejects it.
        CreatePagePage createPage = openCreateFormInTestSpace(setup, spaceReference);
        DocumentPicker picker = createPage.getDocumentPicker();
        picker.setTitle(FORBIDDEN_NAME);
        picker.waitForName(FORBIDDEN_NAME);
        createPage.setTerminalPage(true);
        createPage.clickCreate();

        createPage = new CreatePagePage();
        createPage.waitForErrorMessage();
        assertTrue(createPage.getErrorMessage().contains(FORBIDDEN_NAME),
            String.format("Unexpected error message: [%s]", createPage.getErrorMessage()));

        // The page has not been created.
        assertFalse(setup.rest().exists(new DocumentReference(FORBIDDEN_NAME, spaceReference)));
    }

    private CreatePagePage openCreateFormInTestSpace(TestUtils setup, SpaceReference spaceReference)
    {
        // Open the create form from an existing page located in the test's own space so that the location fields are
        // editable and default to that space. Reaching the create form from the wiki home instead yields a read-only
        // location that cannot be overridden.
        setup.createPage(new DocumentReference("WebHome", spaceReference), "", "Parent");
        setup.gotoPage(new DocumentReference("WebHome", spaceReference));
        CreatePagePage createPage = new ViewPage().createPage();
        // Reveal the advanced location fields so that the page name is editable and kept in sync with the title.
        createPage.getDocumentPicker().toggleLocationAdvancedEdit();
        return createPage;
    }
}
