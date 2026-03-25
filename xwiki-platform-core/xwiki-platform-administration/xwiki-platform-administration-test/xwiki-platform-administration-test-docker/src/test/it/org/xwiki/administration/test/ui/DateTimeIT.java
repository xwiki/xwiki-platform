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

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
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

/**
 * Validate the date and time configuration in the Presentation section of the Administration application.
 *
 * @version $Id$
 */
@UITest(
    // Register the JodaTime plugin so the timezone selection uses Bootstrap select.
    properties = { "xwikiCfgPlugins=com.xpn.xwiki.plugin.jodatime.JodaTimePlugin" },
    // Plugins cannot be installed as extensions, they need to be part of the WAR.
    extraJARs = { "org.xwiki.platform:xwiki-platform-jodatime" }
)
class DateTimeIT
{
    private static final String LAST_MODIFIED_PREFIX = "Last modified by superadmin on ";

    private static final DateTimeFormatter DEFAULT_LAST_MODIFIED_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");

    private static final Pattern LAST_MODIFIED_PATTERN =
        Pattern.compile(Pattern.quote(LAST_MODIFIED_PREFIX) + "\\d{4}/\\d{2}/\\d{2} \\d{2}:\\d{2}");

    @Test
    @Order(1)
    void verifyDateFormatConfiguration(TestUtils setup, TestReference testReference) throws Exception
    {
        setup.loginAsSuperAdmin();

        // Create a new page to verify the date format later on.
        ViewPage page = setup.createPage(testReference, "Date format page content");
        String lastModifiedText = page.getLastModifiedText();
        LocalDateTime lastModifiedDateTime = parseLastModifiedDateTime(lastModifiedText);
        String dateFormat = "HH:mm, dd/MM/yyyy";
        String expectedNewLastModifiedText = LAST_MODIFIED_PREFIX
            + DateTimeFormatter.ofPattern(dateFormat).format(lastModifiedDateTime);

        // Navigate to the Localization section of the Administration application.
        AdministrationPage administrationPage = AdministrationPage.gotoPage();
        LocalizationAdministrationSectionPage localizationSection = administrationPage.clickLocalizationSection();

        try {
            // Change the date format and save.
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

    @Test
    @Order(2)
    void timeZoneConfiguration(TestUtils setup, TestReference testReference) throws Exception
    {
        setup.loginAsSuperAdmin();

        String utcTimezone = "UTC";
        String farTimezone = "Pacific/Kiritimati";

        AdministrationPage administrationPage = AdministrationPage.gotoPage();
        LocalizationAdministrationSectionPage localizationSection = administrationPage.clickLocalizationSection();
        assertEquals("System Default", localizationSection.getTimezone());

        try {
            // First, test that the selection of a timezone works.
            String europeParisTimeZone = "Europe/Paris";
            localizationSection.setTimezone(europeParisTimeZone);
            assertEquals(europeParisTimeZone, localizationSection.getTimezone());
            // Set a known timezone so we can parse the last modified date/time consistently.
            localizationSection.setTimezone(utcTimezone);
            localizationSection.clickSave();

            // Create a new page to verify the timezone later on.
            ViewPage page = setup.createPage(testReference, "Timezone page content");
            // Get the last modified date/time from the page and convert it to the far timezone.
            LocalDateTime lastModifiedUtc = parseLastModifiedDateTime(page.getLastModifiedText());
            ZonedDateTime lastModifiedInFarTimezone = lastModifiedUtc.atZone(ZoneId.of(utcTimezone))
                .withZoneSameInstant(ZoneId.of(farTimezone));
            String expectedLastModifiedText = formatLastModified(lastModifiedInFarTimezone.toLocalDateTime());

            // Now set the far timezone and then verify that the modified date changed as expected.
            administrationPage = AdministrationPage.gotoPage();
            localizationSection = administrationPage.clickLocalizationSection();
            // Verify that the previously selected timezone is still selected.
            assertEquals(utcTimezone, localizationSection.getTimezone());
            // Set a different timezone.
            localizationSection.setTimezone(farTimezone);
            localizationSection.clickSave();
            assertEquals(farTimezone, localizationSection.getTimezone());

            // Verify that the modified date changed accordingly.
            page = setup.gotoPage(testReference);
            assertEquals(expectedLastModifiedText, page.getLastModifiedText());
        } finally {
            setup.setWikiPreference("timezone", "");
        }
    }

    private static LocalDateTime parseLastModifiedDateTime(String lastModifiedText)
    {
        assertThat(lastModifiedText, matchesPattern(LAST_MODIFIED_PATTERN));
        return LocalDateTime.parse(lastModifiedText.substring(LAST_MODIFIED_PREFIX.length()),
            DEFAULT_LAST_MODIFIED_FORMATTER);
    }

    private static String formatLastModified(LocalDateTime dateTime)
    {
        return LAST_MODIFIED_PREFIX + DEFAULT_LAST_MODIFIED_FORMATTER.format(dateTime);
    }
}
