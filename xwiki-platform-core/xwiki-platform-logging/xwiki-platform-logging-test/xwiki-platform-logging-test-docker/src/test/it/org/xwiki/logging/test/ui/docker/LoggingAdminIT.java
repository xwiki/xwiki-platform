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
package org.xwiki.logging.test.ui.docker;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xwiki.logging.test.po.LoggingAdministrationSectionPage;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verify that the Logging administration section works: it lists the registered loggers and allows changing a logger's
 * level and resetting it to its default.
 *
 * @version $Id$
 * @since 18.5.0RC1
 */
@UITest
class LoggingAdminIT
{
    /**
     * The Logging section is reachable from the Administration and renders its intro text and the Live Data table of
     * loggers (with the logger, level and actions columns).
     */
    @Test
    @Order(1)
    void sectionIsReachableAndRenders(TestUtils setup)
    {
        // Programming right is required for the "Actions" column (the level-setting form) to be displayed.
        setup.loginAsSuperAdmin();

        LoggingAdministrationSectionPage section = LoggingAdministrationSectionPage.gotoPage();

        assertTrue(section.getContent().contains("review and modify the log level"),
            "The Logging section intro text is missing");
        assertTrue(section.getLiveDataTable().countRows() > 0, "No logger is listed in the Logging section");
        assertTrue(section.hasAllColumns(), "The logger, level and actions columns are not all displayed");
    }

    /**
     * Change the level of a logger to a value different from its current one, verify the success message and that the
     * change is reflected in the table, then reset the logger to its default level and verify the unset success message
     * and that the level is cleared. The logger is read from the table rather than hardcoded so that the test does not
     * depend on which loggers happen to be registered in the running instance.
     */
    @Test
    @Order(2)
    void setAndUnsetLevel()
    {
        LoggingAdministrationSectionPage section = LoggingAdministrationSectionPage.gotoPage();

        String logger = section.getLogger(1);
        String targetLevel = differentLevel(section.getLevel(1));

        // Set the level and check the success message. The message box text is prefixed by a "Success" label coming
        // from the skin, so we assert on the message being contained rather than on strict equality.
        section.setLevel(1, targetLevel);
        String setMessage = section.getSuccessMessage();
        assertTrue(
            setMessage.contains(String.format("Logger \"%s\" level has been set to \"%s\".", logger, targetLevel)),
            String.format("Unexpected success message after setting the level: [%s]", setMessage));

        // The new level is reflected in the table.
        int row = section.findRowForLogger(logger);
        assertEquals(targetLevel, section.getLevel(row));

        // Reset the logger to its default level (empty value option) and check the unset success message.
        section.setLevel(row, "");
        String unsetMessage = section.getSuccessMessage();
        assertTrue(unsetMessage.contains(String.format("Logger \"%s\" level has been unset.", logger)),
            String.format("Unexpected success message after unsetting the level: [%s]", unsetMessage));

        // The level is cleared in the table.
        assertEquals("", section.getLevel(section.findRowForLogger(logger)));
    }

    /**
     * @param currentLevel the level currently set on a logger (possibly empty for the default level)
     * @return a level guaranteed to be different from {@code currentLevel}, hence offered by the level select (which
     *         omits the option matching the current level)
     */
    private static String differentLevel(String currentLevel)
    {
        for (String level : List.of("DEBUG", "INFO", "WARN", "ERROR", "TRACE")) {
            if (!level.equals(currentLevel)) {
                return level;
            }
        }
        return "DEBUG";
    }
}
