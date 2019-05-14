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
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.platform.launcher.TestIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.test.integration.junit.StackTraceLogParser;
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

    private static final ValidationParser VALIDATION_PARSER = new ValidationParser();

    @Override
    protected String getSkipSystemPropertyKey()
    {
        return SKIP_PROPERTY;
    }

    @Override
    protected void validateOutputForTest(String outputContent, TestIdentifier testIdentifier)
    {
        List<ValidationLine> excludeList = getExcludeList();
        List<ValidationLine> expectedList = getExpectedList();

        Pair<List<String>, List<String>> matching =
            getMatchingLines(outputContent, excludeList, expectedList, testIdentifier);
        List<String> matchingLines = matching.getLeft();
        List<String> matchingExcludes = matching.getRight();

        if (testIdentifier.isTest()) {
            if (!matchingLines.isEmpty()) {
                throw new AssertionError(String.format("The following lines were matching forbidden content:[\n%s\n]",
                    matchingLines.stream().collect(Collectors.joining(NL))));
            }
        } else if (!testIdentifier.getParentId().isPresent() && !matchingExcludes.isEmpty()) {
            // At the end of the tests, output warnings for matching excluded lines so that developers can see that
            // there  are excludes that need to be fixed.
            LOGGER.warn("The following lines were matching excluded patterns and need to be fixed: [\n{}\n]",
                StringUtils.join(matchingExcludes, NL));
        }
    }

    private Pair<List<String>, List<String>> getMatchingLines(String outputContent, List<ValidationLine> excludeList,
        List<ValidationLine> expectedList, TestIdentifier testIdentifier)
    {
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
                for (ValidationLine excludeLine : excludeList) {
                    String testName = excludeLine.getTestName();
                    if ((testName == null || testIdentifier.getUniqueId().matches(testName))
                        && p.contains(excludeLine.getLine()))
                    {
                        matchingExcludes.add(p);
                        return false;
                    }
                }
                return containsForExpectedList(p, expectedList, testIdentifier);
            })
            .collect(Collectors.toList());

        return new ImmutablePair(matchingLines, matchingExcludes);
    }

    private boolean containsForExpectedList(String line, List<ValidationLine> expectedList,
        TestIdentifier testIdentifier)
    {
        boolean result = true;
        for (ValidationLine expectedLine : expectedList) {
            String testName = expectedLine.getTestName();
            if ((testName == null || testIdentifier.getUniqueId().matches(testName))
                && line.contains(expectedLine.getLine()))
            {
                return false;
            }
        }
        return result;
    }

    private List<ValidationLine> getExcludeList()
    {
        return getListFromProperty(GLOBAL_EXCLUDES, EXCLUDE_PROPERTY);
    }

    private List<ValidationLine> getExpectedList()
    {
        return getListFromProperty(GLOBAL_EXPECTED, EXPECTED_PROPERTY);
    }

    private List<ValidationLine> getListFromProperty(List<String> globalList, String propertyKey)
    {
        List<ValidationLine> result = new ArrayList<>();
        for (String globalListItem : globalList) {
            result.add(new ValidationLine(globalListItem));
        }
        String propertyString = getPropertyValue(propertyKey);
        if (propertyString != null) {
            result.addAll(VALIDATION_PARSER.parse(propertyString));
        }
        return result;
    }
}
