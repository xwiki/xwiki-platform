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

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runs the WCAG validation through the axe-core Node.js bridge, and turns its outcome into test failures and
 * logs. Violations fail the validation with an aggregated readable report; incomplete checks are logged at
 * info level and never fail.
 * <p>
 * When the bridge is not available (no Node.js executable, or npm dependencies not installed), the validation
 * is skipped with a warning so that plain {@code mvn test} runs stay possible anywhere; setting the
 * {@code xwiki.testPage.wcag.strict} system property to {@code true} turns the missing bridge into a hard
 * failure.
 *
 * @version $Id$
 * @since 18.10.0RC1
 */
public final class WCAGValidator
{
    private static final Logger LOGGER = LoggerFactory.getLogger(WCAGValidator.class);

    private static final String STRICT_PROPERTY = "xwiki.testPage.wcag.strict";

    private WCAGValidator()
    {
        // Utility class: never instantiated.
    }

    /**
     * Validates one rendered page with the passed customization.
     *
     * @param pageHTML the rendered page to validate
     * @param customizer the configuration of the validation
     */
    public static void validate(String pageHTML, WCAGCustomizer customizer)
    {
        Map<String, String> pages = new LinkedHashMap<>();
        pages.put(pageHTML, customizer.getPageLabel() != null ? customizer.getPageLabel() : "page");
        validate(pages, null, customizer);
    }

    /**
     * Validates several rendered pages, all with the default configuration.
     *
     * @param pages the pages to validate, as rendered HTML mapped to the label identifying each page in the
     *     report
     * @param testMethodName the name of the test method the validation happens in, or {@code null} when
     *     unknown
     */
    static void validate(Map<String, String> pages, String testMethodName)
    {
        validate(pages, testMethodName, new WCAGCustomizer());
    }

    private static void validate(Map<String, String> pages, String testMethodName, WCAGCustomizer customizer)
    {
        String unavailabilityReason = AxeBridge.getUnavailabilityReason();
        if (unavailabilityReason != null) {
            if (Boolean.getBoolean(STRICT_PROPERTY)) {
                throw new IllegalStateException(String.format("The axe-core Node.js bridge is not available: "
                    + "[%s]. Fixes: install the npm dependencies of xwiki-platform-test-page "
                    + "(frontend-maven-plugin does it when building the module), or point the [%s] system "
                    + "property to the directory holding them. This failure is a strictness check: without "
                    + "-D%s=true the validation is skipped with a warning.", unavailabilityReason,
                    "xwiki.testPage.wcag.npmRoot", STRICT_PROPERTY));
            }
            LOGGER.warn("Skipping the WCAG validation of [{}] page(s): [{}]. Set -D{}=true to make this a "
                + "failure instead of a skip.", pages.size(), unavailabilityReason, STRICT_PROPERTY);
            return;
        }

        Map<String, AxePageResult> results = new LinkedHashMap<>();
        for (Map.Entry<String, String> page : pages.entrySet()) {
            try {
                results.put(page.getKey(),
                    AxeBridge.getInstance().analyze(page.getKey(), customizer.getTags(),
                        customizer.getDisabledRules()));
            } catch (IOException e) {
                throw new IllegalStateException("Failed to run the axe-core Node.js bridge", e);
            }
        }

        if (results.values().stream().anyMatch(result -> !result.violations().isEmpty())) {
            throw new AssertionError(WCAGReporter.buildFailureReport(pages, results, testMethodName));
        }
        String incompleteReport = WCAGReporter.buildIncompleteReport(pages, results);
        if (!incompleteReport.isEmpty()) {
            LOGGER.info("Incomplete WCAG checks (reported for information, they never fail the "
                + "validation):\n{}", incompleteReport);
        }
    }
}
