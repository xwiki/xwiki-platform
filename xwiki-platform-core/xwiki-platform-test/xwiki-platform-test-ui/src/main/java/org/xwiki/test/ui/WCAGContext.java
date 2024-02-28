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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.test.ui.po.BasePage;

import com.deque.html.axecore.results.Results;
import com.deque.html.axecore.results.Rule;
import com.deque.html.axecore.selenium.AxeBuilder;
import com.google.common.base.Functions;
import com.google.common.collect.Ordering;

import static java.util.Map.entry;

/**
 * Context related to WCAG (accessibility) validation with axe-core.
 *
 * @version $Id$
 * @since 15.2RC1
 */
public class WCAGContext
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
            entry("area-alt", true),
            // Set to true once the build doesn't fail this rule anymore
            entry("aria-allowed-attr", false),
            entry("aria-command-name", true),
            entry("aria-hidden-body", true),
            entry("aria-hidden-focus", true),
            entry("aria-input-field-name", true),
            entry("aria-meter-name", true),
            entry("aria-progressbar-name", true),
            entry("aria-required-attr", true),
            entry("aria-required-children", true),
            entry("aria-required-parent", true),
            entry("aria-roledescription", true),
            entry("aria-roles", true),
            entry("aria-toggle-field-name", true),
            entry("aria-tooltip-name", true),
            entry("aria-valid-attr-value", true),
            entry("aria-valid-attr", true),
            entry("audio-caption", true),
            entry("blink", true),
            entry("button-name", true),
            entry("bypass", true),
            // Set to true once the build doesn't fail this rule anymore
            entry("color-contrast", false),
            entry("definition-list", true),
            entry("dlitem", true),
            entry("document-title", true),
            // Set to true once the build doesn't fail this rule anymore
            entry("duplicate-id-active", false),
            // Set to true once the build doesn't fail this rule anymore
            entry("duplicate-id-aria", false),
            // Set to true once the build doesn't fail this rule anymore
            entry("duplicate-id", false),
            entry("form-field-multiple-labels", true),
            entry("frame-focusable-content", true),
            entry("frame-title-unique", true),
            entry("frame-title", true),
            entry("html-has-lang", true),
            entry("html-lang-valid", true),
            entry("html-xml-lang-mismatch", true),
            // Set to true once the build doesn't fail this rule anymore
            entry("image-alt", false),
            entry("input-button-name", true),
            entry("input-image-alt", true),
            // Set to true once the build doesn't fail this rule anymore
            entry("label", false),
            // Set to true once the build doesn't fail this rule anymore
            entry("link-in-text-block", false),
            entry("link-name", true),
            entry("list", true),
            entry("listitem", true),
            entry("marquee", true),
            entry("meta-refresh", true),
            entry("meta-viewport", true),
            entry("nested-interactive", true),
            entry("no-autoplay-audio", true),
            entry("object-alt", true),
            entry("role-img-alt", true),
            // Set to true once the build doesn't fail this rule anymore
            entry("scrollable-region-focusable", false),
            // Set to true once the build doesn't fail this rule anymore
            entry("select-name", false),
            entry("server-side-image-map", true),
            entry("svg-img-alt", true),
            entry("td-headers-attr", true),
            entry("th-has-data-cells", true),
            entry("valid-lang", true),
            entry("video-caption", true),

            // WCAG 2.1 Level A & AA Rules
            entry("autocomplete-valid", true),
            entry("avoid-inline-spacing", true),

            // WCAG 2.2 Level A & AA Rules
            entry("target-size", true)
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

    private static final Logger LOGGER = LoggerFactory.getLogger(BasePage.class);

    /**
     * Stores the result of an axe-core validity scan.
     */
    private static final class WCAGTestResults
    {
        private String failReport = "";
        private final long failCount;
        private String warnReport = "";
        private final long warnCount;
        private String incompleteReport = "";
        private final long incompleteCount;
        private final long violationCount;
        private final long passCount;

        /**
         * @param testMethodName the method in which the validation happened
         * @param url the URL from which these results have been generated
         * @param pageClassName the PO class name representing the UI that has been validated
         * @param axeResults the object to which to append the results of the validation to
         */
        private WCAGTestResults(String testMethodName, String url, String pageClassName, Results axeResults)
        {
            // Count the amount of checks with each status
            this.violationCount = numberOfChecks(axeResults.getViolations());
            this.passCount = numberOfChecks(axeResults.getPasses());
            this.incompleteCount = numberOfChecks(axeResults.getIncomplete());
            if (this.incompleteCount != 0) {
                this.incompleteReport = AbstractXWikiCustomAxeReporter.getReadableAxeResults(testMethodName,
                    pageClassName, url, axeResults.getIncomplete());
            }

            // Generate the report as soon as the results are in.
            List<Rule> failingViolations = axeResults.getViolations()
                .stream()
                .filter(rule -> !FAILS_ON_RULE.containsKey(rule.getId()) || FAILS_ON_RULE.get(rule.getId()))
                // If the ruleid is not defined in FAILS_ON_RULE,
                // the default behavior will be to add it to the fails.
                // In order to resolve these test-suite fails quickly, set them as "false" in FAILS_ON_RULE.
                .toList();
            this.failCount = numberOfChecks(failingViolations);
            if (this.failCount != 0) {
                this.failReport = AbstractXWikiCustomAxeReporter.getReadableAxeResults(testMethodName, pageClassName,
                    url, failingViolations);
            }

            List<Rule> warningViolations = axeResults.getViolations()
                .stream()
                .filter(rule -> FAILS_ON_RULE.containsKey(rule.getId()) && !FAILS_ON_RULE.get(rule.getId()))
                .toList();
            this.warnCount = numberOfChecks(warningViolations);
            if (this.warnCount != 0) {
                this.warnReport = AbstractXWikiCustomAxeReporter.getReadableAxeResults(testMethodName, pageClassName,
                    url, warningViolations);
            }
        }

        private int numberOfChecks(List<Rule> violations)
        {
            return (violations == null) ? 0 : violations.stream().mapToInt(rule -> rule.getNodes().size()).sum();
        }

        String getFailReport()
        {
            return this.failReport;
        }

        String getWarnReport()
        {
            return this.warnReport;
        }

        String getIncompleteReport()
        {
            return this.incompleteReport;
        }
    }

    private List<WCAGTestResults> wcagResults = new ArrayList<>();

    private Map<String, Integer> violationCountPerRule = new HashMap<>();
    private Map<String, Integer> incompleteCountPerRule = new HashMap<>();
    private Map<String, Integer> passCountPerRule = new HashMap<>();
    private Map<String, Integer> allCountPerRule = new HashMap<>();
    private boolean wcagEnabled;

    private long wcagTimer;

    private int wcagFailCount;

    private int wcagWarnCount;
    private int wcagViolationCount;
    private int wcagIncompleteCount;
    private int wcagPassCount;

    private final Map<String, ArrayList<String> > wcagValidationCache = new HashMap<>();

    private String testClassName;

    private String testMethodName;

    private boolean stopOnError = true;

    /**
     * Sets the current test class name. This name is the string representation of the TestUI class in which the
     * current wcag validation happens.
     *
     * @param testClassName the test class name to be set
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
     * Sets the current test method name. This name is the string representation of a Test method in a testUI class in
     * which the current wcag validation happens.
     *
     * @param testMethodName the test method name to be set
     */
    public void setTestMethodName(String testMethodName)
    {
        this.testMethodName = testMethodName;
    }

    protected String getTestMethodName()
    {
        return this.testMethodName;
    }

    protected Map<String, Integer> getViolationCountPerRule()
    {
        return violationCountPerRule;
    }

    protected Map<String, Integer> getIncompleteCountPerRule()
    {
        return incompleteCountPerRule;
    }

    protected Map<String, Integer> getPassCountPerRule()
    {
        return passCountPerRule;
    }

    protected Boolean isFailing(String ruleID)
    {
        return FAILS_ON_RULE.get(ruleID);
    }

    /**
     * Instantiate and initialize an axe builder with context options.
     *
     * @return a ready-to-use AxeBuilder instance
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
     * @param url the URL of the page to analyze
     * @param className the class of the page to analyze
     * @return true if there's a need to perform an accessibility analysis of a basePage or false otherwise
     */
    public boolean isNotCached(String url, String className)
    {
        return !(this.wcagValidationCache.containsKey(url)
                && this.wcagValidationCache.get(url).contains(className));
    }

    /**
     * Record the count of checks for a status, per rule.
     * @param statusCountPerRule the map containing the current records.
     * @param newChecks with which the counts should be updated
     */
    private void updateStatusCountPerRule(Map<String, Integer> statusCountPerRule, List<Rule> newChecks)
    {
        for (Rule violation : CollectionUtils.emptyIfNull(newChecks)) {
            String violationID = violation.getId();
            int violationCount = violation.getNodes().size();
            statusCountPerRule.put(violationID,
                    statusCountPerRule.getOrDefault(violationID, 0) + violationCount);
        }
    }

    /**
     * Appends WCAG results to the current context.
     *
     * @param url the URL of the page analyzed
     * @param className the class of the page analyzed
     * @param axeResults results found on the page to append to the WCAGContext aggregated result
     */
    public void addWCAGResults(String url, String className, Results axeResults)
    {
        // Whatever the case, keep a trace of the current report.
        WCAGTestResults wcagTestResults =  new WCAGTestResults(getTestMethodName(), url, className, axeResults);
        updateStatusCountPerRule(violationCountPerRule, axeResults.getViolations());
        updateStatusCountPerRule(incompleteCountPerRule, axeResults.getIncomplete());
        updateStatusCountPerRule(passCountPerRule, axeResults.getPasses());
        updateStatusCountPerRule(allCountPerRule,
            Stream.of(axeResults.getViolations(), axeResults.getIncomplete(), axeResults.getPasses())
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .collect(Collectors.toList()));

        if (wcagTestResults.failCount != 0) {
            LOGGER.error("[{} : {}] Found [{}] failing WCAG violations.",
                url, className, wcagTestResults.failCount);
        }
        if (wcagTestResults.warnCount != 0) {
            LOGGER.warn("[{} : {}] Found [{}] warning WCAG violations.",
                url, className, wcagTestResults.warnCount);
        }
        this.wcagFailCount += wcagTestResults.failCount;
        this.wcagWarnCount += wcagTestResults.warnCount;

        this.wcagViolationCount += wcagTestResults.violationCount;
        this.wcagIncompleteCount += wcagTestResults.incompleteCount;
        this.wcagPassCount += wcagTestResults.passCount;

        this.wcagResults.add(wcagTestResults);
        if (isNotCached(url, className)) {
            // Avoid duplicate entries in the cache.
            this.wcagValidationCache.putIfAbsent(url, new ArrayList<>());
            this.wcagValidationCache.get(url).add(className);
        }
    }

    /**
     * @param time the amount of time to count up, in ms.
     */
    public void addWCAGTime(long time)
    {
        this.wcagTimer += time;
    }

    /**
     * @return the accumulated time spent validating accessibility.
     */
    public long getWCAGTime()
    {
        return this.wcagTimer;
    }

    /**
     * @return the total count of failing wcag violations.
     */
    public long getWCAGFailCount()
    {
        return this.wcagFailCount;
    }

    /**
     * @return the total count of non failing wcag violations.
     */
    public long getWCAGWarnCount()
    {
        return this.wcagWarnCount;
    }

    /**
     * @return the total count of incomplete wcag checks.
     */
    public long getWCAGIncompleteCount()
    {
        return this.wcagIncompleteCount;
    }

    /**
     * @param wcag validation enabled setup parameter to use in the test suite.
     */
    public void setWCAGEnabled(boolean wcag)
    {
        this.wcagEnabled = wcag;
    }

    /**
     * @return true if WCAG validation is enabled, false otherwise
     */
    public boolean isWCAGEnabled()
    {
        return this.wcagEnabled;
    }

    /**
     * @param stopOnError {@code false} if WCAG validation should ignore errors, {@code true} otherwise.
     */
    public void setWCAGStopOnError(boolean stopOnError)
    {
        this.stopOnError = stopOnError;
    }

    /**
     * @return {@code false} if WCAG validation should ignore errors, {@code true} otherwise.
     */
    public boolean shouldWCAGStopOnError()
    {
        return this.stopOnError;
    }

    /**
     * @return any of the validations found an incomplete check.
     */
    public boolean hasWCAGIncomplete()
    {
        return this.wcagIncompleteCount != 0;
    }

    /**
     * @return any of the validations found a failing violation.
     */
    public boolean hasWCAGFails()
    {
        return this.wcagFailCount != 0;
    }

    /**
     * @return any of the validations found a warning violation.
     */
    public boolean hasWCAGWarnings()
    {
        return this.wcagWarnCount != 0;
    }

    /**
     * @return a readable report of WCAG incomplete tests found so far.
     */
    public String buildIncompleteReport()
    {
        StringBuilder mergedReport = new StringBuilder();
        boolean incompleteStillEmpty = true;
        for (WCAGTestResults result : this.wcagResults) {
            if (result.incompleteCount != 0) {
                if (incompleteStillEmpty) {
                    mergedReport.append(String.format("WCAG incomplete checks in the test class [%s]:",
                        getTestClassName()));
                    mergedReport.append(System.lineSeparator());
                    incompleteStillEmpty = false;
                }
                mergedReport.append(result.getIncompleteReport());
                mergedReport.append(System.lineSeparator());
                mergedReport.append(System.lineSeparator());
            }
        }
        return mergedReport.toString();
    }

    /**
     * @return a readable report of WCAG failures found so far.
     */
    public String buildFailsReport()
    {
        StringBuilder mergedReport = new StringBuilder();
        boolean failStillEmpty = true;
        for (WCAGTestResults result : this.wcagResults) {
            if (result.failCount != 0) {
                if (failStillEmpty) {
                    mergedReport.append(String.format("WCAG fails in the test class [%s]:", getTestClassName()));
                    mergedReport.append(System.lineSeparator());
                    failStillEmpty = false;
                }
                mergedReport.append(result.getFailReport());
                mergedReport.append(System.lineSeparator());
                mergedReport.append(System.lineSeparator());
            }
        }
        return mergedReport.toString();
    }

    /**
     * @return a readable report of WCAG violations (non failing) found so far.
     */
    public String buildWarningsReport()
    {
        StringBuilder mergedReport = new StringBuilder();
        boolean warnStillEmpty = true;
        for (WCAGTestResults result : this.wcagResults) {
            if (result.warnCount != 0) {
                if (warnStillEmpty) {
                    mergedReport.append(String.format("WCAG warnings in the test class [%s]:", getTestClassName()));
                    mergedReport.append(System.lineSeparator());
                    warnStillEmpty = false;
                }
                mergedReport.append(result.getWarnReport());
                mergedReport.append(System.lineSeparator());
                mergedReport.append(System.lineSeparator());
            }
        }
        return mergedReport.toString();
    }

    /**
     * Build the content of the wcag analysis overview. This overview contains stats on all the checks done
     * in this test suite.
     * @return a readable overview of WCAG results.
     */
    public String buildOverview()
    {
        StringBuilder overview = new StringBuilder();
        int totalChecks = this.wcagViolationCount + this.wcagIncompleteCount + this.wcagPassCount;
        if (totalChecks > 0) {
            overview.append(String.format("This test suite conducted [%s] checks. Out of those:", totalChecks));
            overview.append(System.lineSeparator());
            overview.append(String.format("  [%d](%.1f%%) were fails",
                this.wcagViolationCount, 100 * (float) this.wcagViolationCount / totalChecks));
            overview.append(System.lineSeparator());
            overview.append(String.format("  [%d](%.1f%%) were incomplete",
                this.wcagIncompleteCount, 100 * (float) this.wcagIncompleteCount / totalChecks));
            overview.append(System.lineSeparator());
            overview.append(String.format("  [%d](%.1f%%) were passed",
                    this.wcagPassCount, 100 * (float) this.wcagPassCount / totalChecks));


            buildResultStatusOverview(overview, this.wcagViolationCount,
                    "Here a list of the rule IDs, sorted by the number of failed checks:",
                    this.getViolationCountPerRule(), "FAIL");
            buildResultStatusOverview(overview, this.wcagIncompleteCount,
                    "Here a list of the rule IDs, sorted by the number of incomplete checks:",
                    this.getIncompleteCountPerRule(), "INCOMPLETE");
            buildResultStatusOverview(overview, this.wcagPassCount,
                    "Here a list of the rule IDs, sorted by the number of passed checks:",
                    this.getPassCountPerRule(), "PASS");
        }
        overview.append(System.lineSeparator());
        return overview.toString();
    }

    private void buildResultStatusOverview(StringBuilder overview, Integer statusCount, String descriptionString,
        Map<String, Integer> statusCounts, String statusID)
    {
        if (statusCount > 0) {
            overview.append(System.lineSeparator());
            overview.append(System.lineSeparator());
            overview.append(descriptionString);
            Ordering.natural().onResultOf(Functions.forMap(statusCounts));
            statusCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .forEach(entry -> {
                    overview.append(System.lineSeparator());
                    overview.append(String.format("  %s %s [%d](%.1f%%)",
                        statusID, entry.getKey(), entry.getValue(),
                        100 * (float) entry.getValue() / allCountPerRule.get(entry.getKey())));
                });
        }
    }

    /**
     * Writes a WCAG report to a file for proper accessibility warnings and failures examination.
     *
     * @param outputFile the file to write the test results to
     * @param output the test results to write
     */
    public static void writeWCAGReportToFile(File outputFile, String output) throws IOException
    {
        try (FileWriter writer = new FileWriter(outputFile, StandardCharsets.UTF_8)) {
            writer.write(output);
        }
    }
}
