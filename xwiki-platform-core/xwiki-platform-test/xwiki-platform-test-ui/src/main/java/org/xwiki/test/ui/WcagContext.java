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
package org.xwiki.test.ui;

import com.deque.html.axecore.results.Results;
import com.deque.html.axecore.results.Rule;
import com.deque.html.axecore.selenium.AxeBuilder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.io.Writer;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Map.entry;

/**
 * Context related to WCAG (accessibility) validation with axe-core.
 * @since 15.2RC1
 * @version $Id$
 */
public class WcagContext
{

    /**
     * List of rules to fail on.
     * Failing rules will fail the test suite on violation, non failing rules will only return a warning if violated.
     * Those rules are identified by ID, and a full list of ids can be found here:
     * <a href="https://github.com/dequelabs/axe-core/blob/develop/doc/rule-descriptions.md#deprecated-rules">...</a>
     * Only rules for validating up to WCAG level AA 2.1 are specified here.
     * Rules not defined here will keep their default behavior and not fail the test-suite on violation. See
     * <a href="https://design.xwiki.org/xwiki/bin/view/Proposal/WCAG21validationinbuild#HSeparatingfailsandwarnings">
     *     for more information on how to use and update this list.
     * Note: There is a parameter for rules *reviewOnFail* in axe-core, however it's not possible to change it using the
     * current selenium integration library.
     **/
    private static final Map<String, Boolean> FAILS_ON_RULE = Map.ofEntries(

            // WCAG 2.0 Level A & AA Rules
            entry("area-alt", false),
            entry("aria-allowed-attr", false),
            entry("aria-command-name", false),
            entry("aria-hidden-body", false),
            entry("aria-hidden-focus", false),
            entry("aria-input-field-name", false),
            entry("aria-meter-name", false),
            entry("aria-progressbar-name", false),
            entry("aria-required-attr", false),
            entry("aria-required-children", false),
            entry("aria-required-parent", false),
            entry("aria-roledescription", false),
            entry("aria-roles", false),
            entry("aria-toggle-field-name", false),
            entry("aria-tooltip-name", false),
            entry("aria-valid-attr-value", false),
            entry("aria-valid-attr", false),
            entry("audio-caption", false),
            entry("blink", false),
            entry("button-name", false),
            entry("bypass", false),
            entry("color-contrast", false),
            entry("definition-list", false),
            entry("dlitem", false),
            entry("document-title", false),
            entry("duplicate-id-active", false),
            entry("duplicate-id-aria", false),
            entry("duplicate-id", false),
            entry("form-field-multiple-labels", false),
            entry("frame-focusable-content", false),
            entry("frame-title-unique", false),
            entry("frame-title", false),
            entry("html-has-lang", false),
            entry("html-lang-valid", false),
            entry("html-xml-lang-mismatch", false),
            entry("image-alt", false),
            entry("input-button-name", false),
            entry("input-image-alt", false),
            entry("label", false),
            entry("link-in-text-block", false),
            entry("link-name", false),
            entry("list", false),
            entry("listitem", false),
            entry("marquee", false),
            entry("meta-refresh", false),
            entry("meta-viewport", false),
            entry("nested-interactive", false),
            entry("no-autoplay-audio", false),
            entry("object-alt", false),
            entry("role-img-alt", false),
            entry("scrollable-region-focusable", false),
            entry("select-name", false),
            entry("server-side-image-map", false),
            entry("svg-img-alt", false),
            entry("td-headers-attr", false),
            entry("th-has-data-cells", false),
            entry("valid-lang", false),
            entry("video-caption", false),

            // WCAG 2.1 Level A & AA Rules
            entry("autocomplete-valid", false),
            entry("avoid-inline-spacing", false),

            // WCAG 2.2 Level A & AA Rules
            entry("target-size", false)
    );

    /**
     * Tags to take into account during axe-core validation.
     * All rules with at least one of those tags will be validated.
     */
    private static final List<String> VALIDATE_TAGS = Arrays.asList("wcag2a", "wcag2aa", "wcag21a", "wcag21aa");

    /**
     * Rules to disable during axe-core validation.
     */
    private static final List<String> DISABLED_RULES = List.of();
    /**
     * Stores the result of an axe-core validity scan.
     */
    private static final class WCAGTestResults
    {
        private String failReport = "";
        private String warnReport = "";
        /**
         * @param testMethodName in which the validation happened.
         * @param url from which these results have been generated
         * @param pageClassName class of the page validated
         * @param axeResults results of the validation
         */
        private WCAGTestResults(String testMethodName, String url, String pageClassName, Results axeResults)
        {
            // Generate the report as soon as the results are in.
            List<Rule> failingViolations = axeResults.getViolations()
                .stream()
                .filter(rule -> !FAILS_ON_RULE.containsKey(rule.getId()) || FAILS_ON_RULE.get(rule.getId()))
                /* If the ruleid is not defined in FAILS_ON_RULE,
                the default behavior will be to add it to the fails.
                In order to resolve these test-suite fails quickly, set them as "false" in FAILS_ON_RULE.
                 */
                .collect(Collectors.toList());
            if (!failingViolations.isEmpty())
            {
                this.failReport = XWikiCustomAxeReporter.getReadableAxeResults(testMethodName, pageClassName,
                        url, failingViolations);
            }

            List<Rule> warningViolations = axeResults.getViolations()
                    .stream()
                    .filter(rule -> FAILS_ON_RULE.containsKey(rule.getId()) && !FAILS_ON_RULE.get(rule.getId()))
                    .collect(Collectors.toList());
            if (!warningViolations.isEmpty())
            {
                this.warnReport = XWikiCustomAxeReporter.getReadableAxeResults(testMethodName, pageClassName,
                        url, warningViolations);
            }
        }
        String getFailReport()
        {
            return this.failReport;
        }
        String getWarnReport()
        {
            return this.warnReport;
        }


        boolean isFailEmpty()
        {
            return this.failReport.equals("");
        }

        boolean isWarnEmpty()
        {
            return this.warnReport.equals("");
        }
    }

    private List<WCAGTestResults> wcagResults = new ArrayList<>();

    private boolean wcagEnabled = true;
    private long wcagTimer;
    private final Map<String, ArrayList<String> > wcagValidationCache = new HashMap<>();
    private String testClassName;
    private String testMethodName;

    /**
     * @return the first non-empty fail report from all the validations.
     */
    public String getFirstWcagFail()
    {
        String report = "";
        for (WCAGTestResults wcagResult : wcagResults)
        {
            if (!wcagResult.isFailEmpty())
            {
                report = wcagResult.getFailReport();
                break;
            }
        }
        return report;
    }

    /**
     * Sets the current test class name.
     * @param testClassName new value to be set.
     */
    public void setTestClassName(String testClassName)
    {
        this.testClassName = testClassName;
    }
    protected String getTestClassName()
    {
        return this.testClassName;
    }
    /**
     * Sets the current test method name.
     * @param testMethodName new value to be set.
     */
    public void setTestMethodName(String testMethodName)
    {
        this.testMethodName = testMethodName;
    }
    protected String getTestMethodName()
    {
        return this.testMethodName;
    }

    /**
     * Instantiate and initialize an axe builder with context options.
     * @return a ready to use AxeBuilder
     */
    public AxeBuilder getAxeBuilder()
    {
        AxeBuilder axeBuilder = new AxeBuilder();
        axeBuilder.withTags(VALIDATE_TAGS);
        if (!DISABLED_RULES.isEmpty()) {
            axeBuilder.disableRules(DISABLED_RULES);
        }
        return axeBuilder;
    }

    /**
     * @param url of the page to analyze
     * @param className class of the page to analyze
     * @return if there's a need to perform an accessibility analysis of a basePage
     */
    public boolean isNotCached(String url, String className)
    {
        return !(this.wcagValidationCache.containsKey(url)
                && this.wcagValidationCache.get(url).contains(className));
    }

    /**
     * Appends WCAG results to the current context.
     * @param url where the page analyzed is available
     * @param className class of the page
     * @param newViolations violations found on the page
     */
    public void addWcagResults(String url, String className, Results newViolations)
    {
        /* Whatever the case, keep a trace of the current report. */
        this.wcagResults.add(new WCAGTestResults(getTestMethodName(), url, className, newViolations));
        if (this.isNotCached(url, className)) {
            /* Avoid duplicate entries in the cache. */
            this.wcagValidationCache.putIfAbsent(url, new ArrayList<>());
            this.wcagValidationCache.get(url).add(className);
        }
    }

    /**
     * @param time The amount of time to count up, in ms.
     */
    public void addWcagTime(long time)
    {
        this.wcagTimer += time;
    }

    /**
     * @return the accumulated time spent validating accessibility.
     */
    public long getWcagTime()
    {
        return this.wcagTimer;
    }

    /**
     * @param wcag validation enabled setup parameter to use in the test suite.
     */
    public void setWcagEnabled(boolean wcag)
    {
        this.wcagEnabled = wcag;
    }

    /**
     * @return the state of wcag validation
     */
    public boolean isWcagEnabled()
    {
        return this.wcagEnabled;
    }

    /**
     * @return any of the results about fails stored is not empty
     */
    public boolean hasWCAGFails()
    {
        for (WCAGTestResults results : this.wcagResults) {
            if (!results.isFailEmpty()) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return any of the results about warnings stored is not empty
     */
    public boolean hasWCAGWarnings()
    {
        for (WCAGTestResults results : this.wcagResults) {
            if (!results.isWarnEmpty()) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return a readable report of wcag fails found so far.
     */
    public String buildFailsReport()
    {
        StringBuilder mergedReport = new StringBuilder();
        boolean failsEmpty = true;
        for (WCAGTestResults result : this.wcagResults) {
            if (!result.isFailEmpty()) {
                if (failsEmpty) {
                    mergedReport.append(String.format("WCAG fails in the test class %s:", getTestClassName()));
                    mergedReport.append(System.lineSeparator());
                    failsEmpty = false;
                }
                mergedReport.append(result.getFailReport());
                mergedReport.append(System.lineSeparator());
                mergedReport.append(System.lineSeparator());
            }
        }
        return mergedReport.toString();
    }

    /**
     * @return a readable report of wcag violations (non failing) found so far.
     */
    public String buildWarningsReport()
    {
        StringBuilder mergedReport = new StringBuilder();
        boolean warningsEmpty = true;
        for (WCAGTestResults result : this.wcagResults) {
            if (!result.isWarnEmpty()) {
                if (warningsEmpty) {
                    mergedReport.append(String.format("WCAG warnings in the test class %s:", getTestClassName()));
                    mergedReport.append(System.lineSeparator());
                    warningsEmpty = false;
                }
                mergedReport.append(result.getWarnReport());
                mergedReport.append(System.lineSeparator());
                mergedReport.append(System.lineSeparator());
            }
        }
        return mergedReport.toString();
    }
    /**
     * Writes a WCAG report to a file for proper accessibility warnings and fails examination.
     * @param outputFile file to write the object to.
     * @param output object to keep in memory.
     */
    public static void writeWCAGReportToFile(final File outputFile, final Object output)
    {
        try (Writer writer =
                     new BufferedWriter(
                             new OutputStreamWriter(
                                     new FileOutputStream(outputFile), StandardCharsets.UTF_8))) {
            writer.write(output.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
