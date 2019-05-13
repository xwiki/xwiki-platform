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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger LOGGER = LoggerFactory.getLogger(ValidateConsoleTestExecutionListener.class);

    private static final String NL = "\n";

    private static final String SKIP_PROPERTY = "xwiki.surefire.validateconsole.skip";

    /**
     * Property for the list of validation errors that are ignire for now but that need to be fixed ASAP.
     */
    private static final String EXCLUDE_PROPERTY = "xwiki.surefire.validateconsole.excludes";

    /**
     * Property for the list of validation errors that are expected by the test. There's nothing to fix, it's normal!
     */
    private static final String EXPECTED_PROPERTY = "xwiki.surefire.validateconsole.expected";

    private static final List<String> SEARCH_STRINGS = Arrays.asList(
        "Deprecated usage of", "ERROR", "WARN", "JavaScript error");

    private static final List<String> GLOBAL_EXCLUDES = Arrays.asList(
        // Exclude the log produced by this class.
        "The following lines were matching excluded patterns and need to be fixed",
        // See https://jira.xwiki.org/browse/XCOMMONS-1627
        "Could not validate integrity of download from file",
        // Warning that can happen on Tomcat when the generation of the random takes a bit long to execute.
        // TODO: fix this so that it doesn't happen. It could mean that we're not using the right secure random
        // implementation and we're using a too slow one.
        "Creation of SecureRandom instance for session ID generation using [SHA1PRNG] took",
        // TODO: Fix this by moving to non-deprecated plugins
        "Solr loaded a deprecated plugin/analysis class [solr.LatLonType].",
        "Solr loaded a deprecated plugin/analysis class [solr.WordDelimiterFilterFactory]"
    );

    private static final List<String> GLOBAL_EXPECTED = Arrays.asList(
        // Broken pipes can happen when the browser moves away from the current page and there are unfinished
        // queries happening on the server side. These are normal errors that can happen for normal users too and
        // we shouldn't consider them faults.
        "Caused by: java.io.IOException: Broken pipe",
        // Warning coming from the XWikiDockerExtension when in verbose mode (which is our default on the CI)
        "Failure when attempting to lookup auth config"
    );

    private static final StackTraceLogParser LOG_PARSER = new StackTraceLogParser();

    @Override
    protected String getSkipSystemPropertyKey()
    {
        return SKIP_PROPERTY;
    }

    @Override
    protected void validateOutputForTest(String outputContent)
    {
        List<String> excludeList = getExcludeList();
        List<String> expectedList = getExpectedList();

        List<String> matchingExcludes = new ArrayList<>();
        List<String> matchingLines = LOG_PARSER.parse(outputContent).stream()
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
                        matchingExcludes.add(p);
                        return false;
                    }
                }
                for (String searchExceptionString : expectedList) {
                    if (p.contains(searchExceptionString)) {
                        return false;
                    }
                }
                return true;
            })
            .collect(Collectors.toList());

        if (!matchingExcludes.isEmpty()) {
            LOGGER.warn("The following lines were matching excluded patterns and need to be fixed: [\n{}\n]",
                StringUtils.join(matchingExcludes, NL));
        }

        if (!matchingLines.isEmpty()) {
            throw new AssertionError(String.format("The following lines were matching forbidden content:[\n%s\n]",
                matchingLines.stream().collect(Collectors.joining(NL))));
        }
    }

    private List<String> getExcludeList()
    {
        return getListFromProperty(GLOBAL_EXCLUDES, EXCLUDE_PROPERTY);
    }

    private List<String> getExpectedList()
    {
        return getListFromProperty(GLOBAL_EXPECTED, EXPECTED_PROPERTY);
    }

    private List<String> getListFromProperty(List<String> globalList, String propertyKey)
    {
        List<String> result = new ArrayList<>();
        result.addAll(globalList);
        String propertyString = getPropertyValue(propertyKey);
        if (propertyString != null) {
            result.addAll(parseCommaSeparatedQuotedString(propertyString));
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
