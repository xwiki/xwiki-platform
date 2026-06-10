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
package org.xwiki.test.ui.docker;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.xwiki.model.validation.test.po.NameStrategiesAdministrationSectionPage;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Validate the default entity name validation configuration shipped with the standard flavor: the Character
 * Replacement strategy must be selected by default, with {@code "/"} and {@code "\"} forbidden (and removed by the
 * transformation). This complements {@code NameStrategiesIT} (in the model-validation module), which validates the
 * "test selected strategy" administration field but cannot verify the shipped default because that default is provided
 * by the flavor (in {@code xwiki-platform-distribution-ui-base}), not by the model-validation module itself.
 *
 * @version $Id$
 * @since 18.5.0RC1
 */
@UITest
class NameStrategiesDefaultConfigurationIT
{
    @BeforeAll
    void beforeAll(TestUtils setup)
    {
        setup.loginAsSuperAdmin();
    }

    /**
     * Open the Name Strategies administration section and verify, without changing anything, that the shipped default
     * uses the Character Replacement strategy and forbids the {@code "/"} and {@code "\"} characters (both removed by
     * the transformation).
     */
    @Test
    void defaultStrategyForbidsSlashAndBackslash()
    {
        NameStrategiesAdministrationSectionPage section = NameStrategiesAdministrationSectionPage.gotoPage();

        // The Character Replacement strategy is selected by default (no selection/save is performed here: the default
        // Configuration document is shipped by the flavor).
        assertEquals("ReplaceCharacterEntityNameValidation", section.getSelectedStrategy());

        // "/" and "\" are forbidden by default: a name containing either is reported as invalid and the character is
        // removed by the transformation.
        section.assertTestResult("a/b", false, "ab");
        section.assertTestResult("a\\b", false, "ab");

        // A name with neither forbidden character is valid and left unchanged.
        section.assertTestResult("abc", true, "abc");
    }
}
