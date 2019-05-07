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
package org.xwiki.test.integration.junit5;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.xwiki.test.junit5.AbstractConsoleTestExecutionListener;

/**
 * Captures content sent to stdout/stderr by JUnit5 functional tests and report a failure if the content contains one of
 * the following strings.
 * <ul>
 * <li>Deprecated method calls from Velocity</li>
 * <li>Error messages</li>
 * <li>Warning messages</li>
 * <li>Javascript errors</li>
 * </ul>
 *
 * @version $Id$
 * @since 11.4RC1
 */
public class ValidateConsoleTestExecutionListener extends AbstractConsoleTestExecutionListener
{
    private static final String SKIP_PROPERTY = "xwiki.surefire.validateconsole.skip";

    private static final String EXCLUDE_PROPERTY = "xwiki.surefire.validateconsole.excludes";

    private static final List<String> SEARCH_STRINGS = Arrays.asList(
        "Deprecated usage of", "ERROR", "WARN", "JavaScript error");

    private static final List<String> GLOBAL_EXCLUDES = Arrays.asList(
        // See https://jira.xwiki.org/browse/XCOMMONS-1627
        "Could not validate integrity of download from file");

    @Override
    protected String getSkipSystemPropertyKey()
    {
        return SKIP_PROPERTY;
    }

    @Override
    protected void validateOutputForTest(String outputContent)
    {
        List<String> excludeList = getExcludeList();
        List<String> matchingLines = new BufferedReader(new StringReader(outputContent)).lines()
            .filter(p -> {
                for (String searchString : SEARCH_STRINGS) {
                    if (p.contains(searchString)) {
                        return true;
                    }
                }
                return false;
            })
            .filter(p -> {
                for (String searchExceptionString : excludeList) {
                    if (p.contains(searchExceptionString)) {
                        return false;
                    }
                }
                return true;
            })
            .collect(Collectors.toList());

        if (!matchingLines.isEmpty()) {
            throw new AssertionError(String.format("The following lines were matching forbidden content:\n%s",
                matchingLines.stream().collect(Collectors.joining("\n"))));
        }
    }

    private List<String> getExcludeList()
    {
        List<String> result = new ArrayList<>();
        result.addAll(GLOBAL_EXCLUDES);
        String excludesString = getPropertyValue(EXCLUDE_PROPERTY);
        if (excludesString != null) {
            result.addAll(parseCommaSeparatedQuotedString(excludesString));
        }
        return result;
    }

    private List<String> parseCommaSeparatedQuotedString(String content)
    {
        List<String> tokensList = new ArrayList<>();
        boolean inQuotes = false;
        boolean inEscape = false;
        StringBuilder b = new StringBuilder();
        for (char c : content.toCharArray()) {
            switch (c) {
                case ',':
                    if (inQuotes || inEscape) {
                        b.append(c);
                    } else {
                        tokensList.add(b.toString());
                        b = new StringBuilder();
                    }
                    inEscape = false;
                    break;
                case '\"':
                    if (!inEscape) {
                        inQuotes = !inQuotes;
                    } else {
                        b.append(c);
                        inEscape = false;
                    }
                    break;
                case '\\':
                    if (inEscape) {
                        b.append(c);
                    }
                    inEscape = !inEscape;
                    break;
                default:
                    //Ignore characters not in quotes. This allows ignoring new lines and spaces between entries.
                    if (inQuotes) {
                        b.append(c);
                    }
                    inEscape = false;
                    break;
            }
        }
        tokensList.add(b.toString());
        return tokensList;
    }
}
