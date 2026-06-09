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

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.xwiki.model.validation.test.po.NameStrategiesAdministrationSectionPage;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;

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
    @BeforeAll
    void beforeAll(TestUtils setup)
    {
        setup.loginAsSuperAdmin();
    }

    /**
     * Select and save the Slug strategy, then verify through the "test selected strategy" field that names are
     * validated and transformed according to that strategy. The Slug strategy is used because, once saved with its
     * default configuration, its behaviour is deterministic. The expected results below mirror the unit tests in
     * {@code SlugEntityNameValidationTest}.
     */
    @Test
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
}
