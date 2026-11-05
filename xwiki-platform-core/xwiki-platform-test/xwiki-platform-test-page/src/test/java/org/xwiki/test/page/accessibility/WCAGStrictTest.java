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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests of the handling of a missing axe-core bridge: skipped with a warning by default, hard failure when the
 * {@code xwiki.testPage.wcag.strict} system property is set.
 *
 * @version $Id$
 * @since 18.10.0RC1
 */
class WCAGStrictTest
{
    /**
     * Points the npm root of the bridge to a directory that cannot exist, making the bridge systematically
     * unavailable regardless of where the test runs from.
     */
    private static final String BOGUS_NPM_ROOT = "/no/such/npm/root";

    /**
     * A page without any assumption on the bridge: only the availability handling is tested here.
     */
    private static final String SOME_HTML = "<div>some page</div>";

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN,
        WCAGValidator.class.getName());

    @AfterEach
    void tearDown()
    {
        System.clearProperty("xwiki.testPage.wcag.npmRoot");
        System.clearProperty("xwiki.testPage.wcag.strict");
    }

    @Test
    void skipValidationWhenBridgeIsMissing()
    {
        System.setProperty("xwiki.testPage.wcag.npmRoot", BOGUS_NPM_ROOT);
        WCAGCustomizer customizer = new WCAGCustomizer();

        assertDoesNotThrow(() -> WCAGValidator.validate(SOME_HTML, customizer));

        assertTrue(this.logCapture.getMessage(0).contains("Skipping the WCAG validation of [1] page(s)"),
            this.logCapture.getMessage(0));
    }

    @Test
    void failValidationWhenBridgeIsMissingInStrictMode()
    {
        System.setProperty("xwiki.testPage.wcag.npmRoot", BOGUS_NPM_ROOT);
        System.setProperty("xwiki.testPage.wcag.strict", "true");
        WCAGCustomizer customizer = new WCAGCustomizer();

        IllegalStateException error = assertThrows(IllegalStateException.class,
            () -> WCAGValidator.validate(SOME_HTML, customizer));
        assertTrue(error.getMessage().contains("The axe-core Node.js bridge is not available"), error.getMessage());
        assertTrue(error.getMessage().contains("xwiki.testPage.wcag.strict"), error.getMessage());
    }
}
