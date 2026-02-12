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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xwiki.administration.test.po.AdministrationPage;
import org.xwiki.administration.test.po.LocalizationAdministrationSectionPage;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.ViewPage;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Validate the Date Format configuration in the Presentation section of the Administration application.
 *
 * @version $Id$
 */
@UITest
class DateFormatIT
{
    @Test
    @Order(1)
    void verifyDateFormatConfiguration(TestUtils setup, TestReference testReference) throws Exception
    {
        setup.loginAsSuperAdmin();

        // Create a new page to verify the date format later on.
        ViewPage page = setup.createPage(testReference, "Test page content");
        String lastModifiedText = page.getLastModifiedText();
        // The text should have the form "Last modified by superadmin on yyyy/MM/dd HH:mm". Parse the values to match
        // them later after changing the date format.
        Pattern pattern =
            Pattern.compile("Last modified by superadmin on (\\d{4})/(\\d{2})/(\\d{2}) (\\d{2}):(\\d{2})");
        assertThat(lastModifiedText, matchesPattern(pattern));
        Matcher matcher = pattern.matcher(lastModifiedText);
        assertTrue(matcher.matches());
        String year = matcher.group(1);
        String month = matcher.group(2);
        String day = matcher.group(3);
        String hour = matcher.group(4);
        String minute = matcher.group(5);
        String expectedNewLastModifiedText = String.format("Last modified by superadmin on %s:%s, %s/%s/%s",
            hour, minute, day, month, year);

        // Navigate to the Presentation section of the Administration application.
        AdministrationPage administrationPage = AdministrationPage.gotoPage();
        LocalizationAdministrationSectionPage localizationSection = administrationPage.clickLocalizationSection();

        try {
            // Change the date format and save.
            String dateFormat = "HH:mm, dd/MM/yyyy";
            localizationSection.setDateFormat(dateFormat);
            localizationSection.clickSave();

            // Verify that the date format is applied.
            page = setup.gotoPage(testReference);
            lastModifiedText = page.getLastModifiedText();
            assertEquals(expectedNewLastModifiedText, lastModifiedText);
        } finally {
            // Reset the date format to the default value. Use the REST API to avoid failures in the finally block
            // that could hide the original failure if the test fails.
            setup.setWikiPreference("dateformat", "");
        }
    }
}
