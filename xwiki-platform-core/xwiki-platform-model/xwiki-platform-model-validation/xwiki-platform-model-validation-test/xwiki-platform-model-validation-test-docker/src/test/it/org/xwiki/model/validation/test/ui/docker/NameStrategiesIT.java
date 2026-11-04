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
package org.xwiki.model.validation.test.ui.docker;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.validation.test.po.NameStrategiesAdministrationSectionPage;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.CreatePagePage;
import org.xwiki.test.ui.po.DocumentPicker;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.test.ui.po.editor.EditPage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Validate the "test selected strategy" field of the Name Strategies administration section: entering a name there must
 * report whether the name is valid for the currently selected entity name validation strategy and display the
 * transformed version of that name.
 *
 * @version $Id$
 * @since 18.5.0RC1
 */
@UITest
class NameStrategiesIT
{
    private static final String SLUG_STRATEGY = "SlugEntityNameValidation";

    private static final List<String> CONFIGURATION_SPACES = List.of("XWiki", "EntityNameValidation");

    private static final String CONFIGURATION_CLASS = "XWiki.EntityNameValidation.ConfigurationClass";

    @BeforeAll
    void beforeAll(TestUtils setup)
    {
        setup.loginAsSuperAdmin();
    }

    @AfterEach
    void afterEach(TestUtils setup)
    {
        // The name strategy configuration applies to every page created afterwards on the shared XWiki instance:
        // restore the default options (the Kebab-case ones are disabled or empty, the names are transformed but not
        // validated).
        setup.updateObject(CONFIGURATION_SPACES, "Configuration", CONFIGURATION_CLASS, 0, "useTransformation", "1",
            "useValidation", "0", "slug.lowercase", "0", "slug.dotsBetweenDigits", "0", "slug.forbiddenWords", "");
    }

    /**
     * Select and save the Slug strategy, then verify through the "test selected strategy" field that names are
     * validated and transformed according to that strategy. The Slug strategy is used because, once saved with its
     * default configuration, its behaviour is deterministic. The expected results below mirror the unit tests in
     * {@code SlugEntityNameValidationTest}.
     */
    @Test
    @Order(1)
    void testSelectedStrategy()
    {
        NameStrategiesAdministrationSectionPage section = NameStrategiesAdministrationSectionPage.gotoPage();
        // Select and save the Slug strategy so that its configuration properties are initialized before testing it.
        section.selectStrategy("SlugEntityNameValidation");
        section.save();

        section = NameStrategiesAdministrationSectionPage.gotoPage();

        // A name that is already a valid slug: it is reported as valid and left unchanged by the transformation.
        section.assertTestResult("TeSt", true, "TeSt");

        // A name containing an accent: it is reported as invalid, and the accent is removed by the transformation.
        section.assertTestResult("tést", false, "test");

        // A name with accents and special characters: it is reported as invalid and transformed into a valid slug.
        section.assertTestResult("test âccents/and.special%characters", false, "test-accents-and-special-characters");
    }

    /**
     * Configure the Kebab-case strategy with all its options (conversion to lowercase, dots allowed between digits and
     * a forbidden word) and the automatic transformation, then create a page whose title contains accents, spaces,
     * special characters, dots and the forbidden word: the page name must be transformed according to these options
     * while the title is kept as typed.
     */
    @Test
    @Order(2)
    void kebabCaseStrategy(TestUtils setup, TestReference reference) throws Exception
    {
        SpaceReference spaceReference = reference.getLastSpaceReference();
        setup.deleteSpace(spaceReference);

        NameStrategiesAdministrationSectionPage section = NameStrategiesAdministrationSectionPage.gotoPage();
        section.selectStrategy(SLUG_STRATEGY);
        section.setTransformNameAutomatically(true);
        section.setValidateNames(false);
        section.setSlugConvertToLowercase(true);
        section.setSlugAllowDots(true);
        section.setSlugForbiddenWords("Forbidden");
        section.save();

        // The accents are removed, the dot between the digits 8 and 9 is kept, the other dots, the spaces and the
        // special characters are replaced by a single dash, the name is lowercased and the forbidden word is removed
        // (whatever its case).
        String title = "T\\é£\"  s.t8.9e.Forbidden d";
        String expectedName = "t-e-s-t8.9e-d";

        // Open the create form from a page of the test space so that the location fields are editable and default to
        // that space.
        CreatePagePage createPage =
            setup.createPage(new DocumentReference("WebHome", spaceReference), "", "Parent").createPage();
        DocumentPicker picker = createPage.getDocumentPicker();
        // Reveal the advanced location fields to check the page name derived from the title.
        picker.toggleLocationAdvancedEdit();
        picker.setTitle(title);
        picker.waitForName(expectedName);
        createPage.setTerminalPage(true);
        createPage.clickCreate();

        ViewPage savedPage = new EditPage().clickSaveAndView();
        assertEquals(expectedName, savedPage.getMetaDataValue("page"));
        assertEquals(title, savedPage.getDocumentTitle());
        assertTrue(setup.rest().exists(new DocumentReference(expectedName, spaceReference)));
    }
}
