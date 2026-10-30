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

import java.util.List;
import java.util.Map;

/**
 * Builds the readable reports of a WCAG validation. The format of a rule report follows the one of the docker
 * based functional tests ({@code AbstractXWikiCustomAxeReporter}), adapted to page tests: the labeled rendered
 * page replaces the tested URL.
 *
 * @version $Id$
 * @since 18.10.0RC1
 */
final class WCAGReporter
{
    private WCAGReporter()
    {
        // Utility class: never instantiated.
    }

    /**
     * @param pages the validated pages, as rendered HTML mapped to their label
     * @param results the outcome of the validation of each page, in the same order
     * @param testMethodName the name of the test method the validation happened in, or {@code null} when
     *     unknown
     * @return the report of all the violations of all the pages
     */
    static String buildFailureReport(Map<String, String> pages, Map<String, AxePageResult> results,
        String testMethodName)
    {
        StringBuilder report = new StringBuilder();
        if (testMethodName != null) {
            report.append(String.format("WCAG validation of the pages rendered during the test method [%s] "
                + "failed.", testMethodName)).append(System.lineSeparator());
        }
        for (Map.Entry<String, AxePageResult> result : results.entrySet()) {
            if (!result.getValue().violations().isEmpty()) {
                appendPageHeader(report, pages.get(result.getKey()), testMethodName,
                    result.getValue().violations());
            }
        }
        return report.toString().trim();
    }

    /**
     * @param pages the validated pages, as rendered HTML mapped to their label
     * @param results the outcome of the validation of each page, in the same order
     * @return a compact listing of the incomplete checks of all the pages, empty when there is none
     */
    static String buildIncompleteReport(Map<String, String> pages, Map<String, AxePageResult> results)
    {
        StringBuilder report = new StringBuilder();
        for (Map.Entry<String, AxePageResult> result : results.entrySet()) {
            for (AxeRuleResult rule : result.getValue().incomplete()) {
                report.append(String.format("[%s] (%s) — %s — %s%n", rule.id(), rule.impact(),
                    pages.get(result.getKey()), rule.helpUrl()));
            }
        }
        return report.toString();
    }

    private static void appendPageHeader(StringBuilder report, String pageLabel, String testMethodName,
        List<AxeRuleResult> violations)
    {
        report.append("__________").append(System.lineSeparator());
        if (testMethodName != null) {
            report.append(String.format("Validation in the test method [%s]", testMethodName))
                .append(System.lineSeparator());
        }
        report.append(String.format("Check for page [%s].", pageLabel)).append(System.lineSeparator());
        report.append(String.format("Found [%s] failing rule(s).", violations.size()))
            .append(System.lineSeparator()).append(System.lineSeparator());

        int ruleNumber = 1;
        for (AxeRuleResult rule : violations) {
            report.append(ruleNumber++).append(": ").append(rule.help()).append(System.lineSeparator());
            appendProperty(report, "Rule ID: ", rule.id());
            appendProperty(report, "Description: ", rule.description());
            appendProperty(report, "Help URL: ", rule.helpUrl());
            appendProperty(report, "Help: ", rule.help());
            appendProperty(report, "Impact: ", rule.impact());
            appendProperty(report, "Tags: ", String.join(", ", rule.tags()));
            for (AxeNodeResult node : rule.nodes()) {
                report.append(System.lineSeparator());
                report.append("HTML element: ").append(System.lineSeparator());
                for (String line : node.html().split("\\r?\\n")) {
                    report.append('\t').append(line.trim()).append(System.lineSeparator());
                }
                appendProperty(report, "Selector: ", String.join(" ", node.target()));
                appendProperty(report, "", node.failureSummary());
            }
            report.append(System.lineSeparator());
        }
    }

    private static void appendProperty(StringBuilder message, String label, String value)
    {
        message.append(label).append(value).append(System.lineSeparator());
    }
}
