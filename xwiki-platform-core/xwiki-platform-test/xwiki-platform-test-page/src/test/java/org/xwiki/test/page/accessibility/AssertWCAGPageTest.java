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
package org.xwiki.test.page.accessibility;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.page.HTML50ComponentList;
import org.xwiki.test.page.PageTest;
import org.xwiki.test.page.XWikiSyntax21ComponentList;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests of {@code assertWCAG}, the explicit WCAG validation of a rendered page.
 *
 * @version $Id$
 * @since 18.10.0RC1
 */
@HTML50ComponentList
@XWikiSyntax21ComponentList
class AssertWCAGPageTest extends PageTest
{
    private static final DocumentReference GOOD_PAGE = new DocumentReference("xwiki", "WCAG", "GoodPage");

    private static final DocumentReference BAD_PAGE = new DocumentReference("xwiki", "WCAG", "BadPage");

    private static final DocumentReference INCOMPLETE_PAGE =
        new DocumentReference("xwiki", "WCAG", "IncompletePage");

    /**
     * The tests of this class exercise the real bridge, so they fail when it cannot be found: the default build
     * installs its npm dependencies through the frontend-maven-plugin, and a missing bridge would otherwise be
     * silently skipped.
     */
    private static final String UNAVAILABILITY_REASON = AxeBridge.getUnavailabilityReason();

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.INFO,
        WCAGValidator.class.getName());

    @BeforeAll
    static void assertBridgeAvailable()
    {
        assertNull(UNAVAILABILITY_REASON, "The axe-core bridge is expected to be available here: "
            + UNAVAILABILITY_REASON);
    }

    @Test
    void validatePageWithoutViolation()
    {
        assertDoesNotThrow(() -> assertWCAG(renderPage(GOOD_PAGE)));
    }

    @Test
    void validatePageWithViolations() throws Exception
    {
        String pageHTML = renderPage(BAD_PAGE);
        AssertionError error = assertThrows(AssertionError.class, () -> assertWCAG(pageHTML));

        // The report lists the violated rules, with their help and the failing element.
        assertTrue(error.getMessage().contains("Rule ID: image-alt"), error.getMessage());
        assertTrue(error.getMessage().contains("Rule ID: button-name"), error.getMessage());
        assertTrue(error.getMessage().contains("Rule ID: link-name"), error.getMessage());
        assertTrue(error.getMessage().contains("Check for page [page]."), error.getMessage());
        assertTrue(error.getMessage().contains("https://dequeuniversity.com/rules/axe/"), error.getMessage());
        assertTrue(error.getMessage().contains("<button type=\"button\"></button>"), error.getMessage());
    }

    @Test
    void validatePageWithCustomLabel() throws Exception
    {
        String pageHTML = renderPage(BAD_PAGE);
        WCAGCustomizer customizer = new WCAGCustomizer().withPageLabel("my page");
        AssertionError error = assertThrows(AssertionError.class, () -> assertWCAG(pageHTML, customizer));

        assertTrue(error.getMessage().contains("Check for page [my page]."), error.getMessage());
    }

    @Test
    void validatePageWithDisabledRules()
    {
        assertDoesNotThrow(() -> assertWCAG(renderPage(BAD_PAGE), new WCAGCustomizer()
            .withDisabledRules("image-alt", "button-name", "link-name")));
    }

    @Test
    void validatePageWithTagsOverride()
    {
        // All the violations of the page are WCAG 2.0 level A rules: validating level AA rules only passes.
        assertDoesNotThrow(() -> assertWCAG(renderPage(BAD_PAGE), new WCAGCustomizer().withTags("wcag2aa")));
    }

    @Test
    void validatePageWithIncompleteChecks()
    {
        // Incomplete checks never fail the validation, they are reported at info level.
        assertDoesNotThrow(() -> assertWCAG(renderPage(INCOMPLETE_PAGE)));
        assertEquals(1, this.logCapture.size());
        assertTrue(this.logCapture.getMessage(0).contains("th-has-data-cells"),
            this.logCapture.getMessage(0));
    }
}
