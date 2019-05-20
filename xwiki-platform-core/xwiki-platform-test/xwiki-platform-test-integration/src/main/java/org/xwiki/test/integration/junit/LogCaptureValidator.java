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
package org.xwiki.test.integration.junit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogCaptureValidator
{
    private static final Logger LOGGER = LoggerFactory.getLogger(LogCaptureValidator.class);

    private static final String NL = "\n";

    private static final List<String> SEARCH_STRINGS = Arrays.asList(
        "Deprecated usage of", "ERROR", "WARN", "JavaScript error");

    private static final List<Line> GLOBAL_EXCLUDES = Arrays.asList(
        // See https://jira.xwiki.org/browse/XCOMMONS-1627
        new Line("Could not validate integrity of download from file"),
        // Warning that can happen on Tomcat when the generation of the random takes a bit long to execute.
        // TODO: fix this so that it doesn't happen. It could mean that we're not using the right secure random
        // implementation and we're using a too slow one.
        new Line("Creation of SecureRandom instance for session ID generation using [SHA1PRNG] took"),
        // Firefox Selenium Driver warning
        new Line("Marionette\tWARN"),
        // The LibreOffice container outputs this error on startup. We should try to understand why it kills LO before
        // restarting it.
        new Line("Office process died with exit code 81; restarting it"),
        // FIXME: convert all ? based query to ?<number>
        new Line("Deprecated usage legacy-style HQL ordinal parameters (`?`)")
    );

    private static final List<Line> GLOBAL_EXPECTED = Arrays.asList(
        // Broken pipes can happen when the browser moves away from the current page and there are unfinished
        // queries happening on the server side. These are normal errors that can happen for normal users too and
        // we shouldn't consider them faults.
        new Line("Caused by: java.io.IOException: Broken pipe"),
        // Warning coming from the XWikiDockerExtension when in verbose mode (which is our default on the CI)
        new Line("Failure when attempting to lookup auth config")
    );

    private static final StackTraceLogParser LOG_PARSER = new StackTraceLogParser();

    /**
     * @param logContent the log content to validate
     * @param configuration the user-registered excludes and expected log lines
     */
    public void validate(String logContent, LogCaptureConfiguration configuration)
    {
        List<Line> allExcludes = new ArrayList<>();
        allExcludes.addAll(GLOBAL_EXCLUDES);
        allExcludes.addAll(configuration.getExcludedLines());

        List<Line> allExpected = new ArrayList<>();
        allExpected.addAll(GLOBAL_EXPECTED);
        allExpected.addAll(configuration.getExpectedLines());

        List<String> matchingExcludes = new ArrayList<>();
        List<String> matchingLines = LOG_PARSER.parse(logContent).stream()
            .filter(p -> {
                for (String searchString : SEARCH_STRINGS) {
                    if (p.contains(searchString)) {
                        return true;
                    }
                }
                return false;
            })
            .filter(p -> {
                for (Line excludedLine : allExcludes) {
                    if (isMatching(p, excludedLine)) {
                        matchingExcludes.add(p);
                        return false;
                    }
                }
                for (Line expectedLine : allExpected) {
                    if (isMatching(p, expectedLine)) {
                        return false;
                    }
                }
                return true;
            })
            .collect(Collectors.toList());

        // At the end of the tests, output warnings for matching excluded lines so that developers can see that
        // there  are excludes that need to be fixed.
        if (!matchingExcludes.isEmpty()) {
            LOGGER.warn("The following lines were matching excluded patterns and need to be fixed: [\n{}\n]",
                StringUtils.join(matchingExcludes, NL));
        }

        if (!matchingLines.isEmpty()) {
            throw new AssertionError(String.format("The following lines were matching forbidden content:[\n%s\n]",
                matchingLines.stream().collect(Collectors.joining(NL))));
        }
    }

    private boolean isMatching(String lineToMatch, Line line)
    {
        return (line.isRegex() && lineToMatch.matches(line.getContent()))
            || (!line.isRegex() && lineToMatch.contains(line.getContent()));
    }
}
