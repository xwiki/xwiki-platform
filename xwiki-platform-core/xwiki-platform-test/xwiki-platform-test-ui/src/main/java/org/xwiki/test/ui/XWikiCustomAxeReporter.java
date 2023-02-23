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
 * Alternatively, at your choice, the contents of this file may be used under the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package org.xwiki.test.ui;

import com.deque.html.axecore.results.CheckedNode;
import com.deque.html.axecore.results.Rule;

import java.util.List;

/**
 * Creates a human-readable report from an axe-core scan result.
 * @since 15.1RC2
 * @version $Id$
 */
public final class XWikiCustomAxeReporter
{
    private static String axeResultString;
    private XWikiCustomAxeReporter()
    {
    }

    /**
     * @param newAxeResult a new human-readable report to store in axeResultString.
     */
    private static void setAxeResultString(String newAxeResult)
    {
        axeResultString = newAxeResult;
    }

    /**
     * @return the human-readable report generated in getReadableAxeResults
     */
    static String getAxeResultString()
    {
        return axeResultString;
    }

    /**
     * Appends a field to a wcag report.
     *
     * @param message to append to.
     * @param name    of the field to report.
     * @param value   of the field to report
     */
    private static void appendPropertyToReport(StringBuilder message, String name, String value)
    {
        message.append(name).append(value);
        message.append(System.lineSeparator());
    }

    /**
     * Stores a human-readable summary of the violations contained in scanResults in axeResultString.
     *
     * @param testMethodName from the current test context
     * @param pageClassName  name of the class of the page to analyze
     * @param url            of the page to analyze
     * @param scanResults    the list of rules breached during validation.
     * @return true if there is any rule breached in scanResults
     */
    static boolean getReadableAxeResults(String testMethodName, String pageClassName,
                                         String url, List<Rule> scanResults)
    {
        StringBuilder message = new StringBuilder();
        int axeRulesAmount = scanResults.size();
        message.append("Validation in the test method ").append(testMethodName);
        message.append(System.lineSeparator());
        message.append("Check for ").append(pageClassName).append(" at ").append(url);
        message.append(System.lineSeparator());
        message.append("Found ").append(axeRulesAmount).append(" items");
        message.append(System.lineSeparator());
        if (scanResults.isEmpty()) {
            setAxeResultString(message.toString().trim());
            return false;
        } else {
            message.append(System.getProperty("line.separator"));
            int elementNo = 1;

            for (Rule element : scanResults) {
                message.append(elementNo++).append(": ").append(element.getHelp());
                message.append(System.lineSeparator());
                appendPropertyToReport(message, "Description: ", element.getDescription());
                appendPropertyToReport(message, "Help URL: ", element.getHelpUrl());
                appendPropertyToReport(message, "Help: ", element.getHelp());
                appendPropertyToReport(message, "Impact: ", element.getImpact());
                appendPropertyToReport(message, "Tags: ", String.join(", ", element.getTags()));
                if (element.getNodes() != null && !element.getNodes().isEmpty()) {
                    for (CheckedNode item : element.getNodes()) {
                        appendPropertyToReport(message, "\tHTML element: ", item.getHtml());
                        appendPropertyToReport(message, "\tSelector: ", item.getTarget().toString());
                        appendPropertyToReport(message, "\t", item.getFailureSummary());
                    }
                }
                message.append(System.lineSeparator());
            }
            setAxeResultString(message.toString().trim());
            return true;
        }
    }
}
