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
/*
 * Copyright (C) 2020 Deque Systems Inc.,
 *
 * Your use of this Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * This entire copyright notice must appear in every copy of this file you
 * distribute or in any file that contains substantial portions of this source
 * code.
 */
/*
    Class inspired by and built upon:
    com.deque.html.axecore.selenium.AxeReporter
    https://github.com/dequelabs/axe-core-maven-html/blob/develop/selenium/src/main/java/com/deque/html/axecore/selenium
    /AxeReporter.java
 */
package org.xwiki.test.ui;

import com.deque.html.axecore.results.CheckedNode;
import com.deque.html.axecore.results.Rule;

import java.util.List;

/**
 * Creates a human-readable report from an axe-core scan result.
 * @since 15.2RC1
 * @version $Id$
 */
abstract class AbstractXWikiCustomAxeReporter
{
    /**
     * Appends a field to a wcag report.
     *
     * @param message to append to.
     * @param label   of the field to report.
     * @param value   of the field to report
     */
    private static void appendPropertyToReport(StringBuilder message, String label, String value)
    {
        message.append(label).append(value);
        message.append(System.lineSeparator());
    }

    /**
     * Appends content to a stringBuilder by trimming and putting a tab before each line.
     * @param message in which we want to append the content
     * @param html string content to format and append
     */
    private static void buildHTMLString(StringBuilder message, String html)
    {
        for (String line : html.split("\\r?\\n")) {
            message.append("\t")
                .append(line.trim())
                .append(System.lineSeparator());
        }
    }

    /**
     * Appends content about one violation of one rule.
     * @param message in which we want to append the content
     * @param violation content to format and append.
     */
    private static void buildFromViolation(StringBuilder message, CheckedNode violation)
    {
        message.append(System.lineSeparator());
        message.append("HTML element: ");
        message.append(System.lineSeparator());
        buildHTMLString(message, violation.getHtml());
        appendPropertyToReport(message, "Selector: ", violation.getTarget().toString());
        appendPropertyToReport(message, "", violation.getFailureSummary());
    }

    /**
     * Appends content about one rule.
     * @param message in which we want to append the content
     * @param rule content to format.
     */
    private static void buildFromRule(StringBuilder message, Rule rule)
    {
        message.append(rule.getHelp());
        message.append(System.lineSeparator());
        appendPropertyToReport(message, "Description: ", rule.getDescription());
        appendPropertyToReport(message, "Help URL: ", rule.getHelpUrl());
        appendPropertyToReport(message, "Help: ", rule.getHelp());
        appendPropertyToReport(message, "Impact: ", rule.getImpact());
        appendPropertyToReport(message, "Tags: ", String.join(", ", rule.getTags()));
        if (rule.getNodes() != null && !rule.getNodes().isEmpty()) {
            for (CheckedNode item : rule.getNodes()) {
                buildFromViolation(message, item);
            }
        }
        message.append(System.lineSeparator());
    }

    /**
     * @param testMethodName from the current test context
     * @param pageClassName  name of the class of the page to analyze
     * @param url            of the page to analyze
     * @param scanResult    the rule violated during validation.
     * @return a human-readable summary of one violation contained in scanResults..
     */
    static String getReadableAxeResults(String testMethodName, String pageClassName,
        String url, Rule scanResult)
    {
        return getReadableAxeResults(testMethodName, pageClassName,
            url, List.of(scanResult));
    }

    /**
     * @param testMethodName from the current test context
     * @param pageClassName  name of the class of the page to analyze
     * @param url            of the page to analyze
     * @param scanResults    the list of rules violated during validation.
     * @return a human-readable summary of the violations contained in scanResults in axeResultString.
     */
    protected static String getReadableAxeResults(String testMethodName, String pageClassName,
                                         String url, List<Rule> scanResults)
    {
        StringBuilder message = new StringBuilder();
        int axeRulesAmount = scanResults.size();
        message.append("__________");
        message.append(System.lineSeparator());
        message.append(String.format("Validation in the test method [%s]", testMethodName));
        message.append(System.lineSeparator());
        message.append(String.format("Check for [%s] at [%s].", pageClassName, url));
        message.append(System.lineSeparator());
        message.append(String.format("Found [%s] items", axeRulesAmount));
        message.append(System.lineSeparator());
        if (scanResults.isEmpty()) {
            return "";
        } else {
            message.append(System.lineSeparator());
            int elementNo = 1;
            for (Rule element : scanResults) {
                message.append(elementNo++).append(": ");
                buildFromRule(message, element);
            }
            message.append(System.lineSeparator());
            return message.toString().trim();
        }
    }
}
