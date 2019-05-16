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

/**
 * Validate logs against default excludes/expected lines and those registered by the tests.
 *
 * @version $Id$
 * @since 11.4RC1
 */
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
        new Line("Loading extension 'screenshots@mozilla.org': "),
        // The LibreOffice container outputs this error on startup. We should try to understand why it kills LO before
        // restarting it.
        new Line("Office process died with exit code 81; restarting it"),
        // When executing tests on PosgtreSQL we get the following errors when the hibernate upgrade code kicks in at
        // XWiki startup
        new Line("relation \"xwikidbversion\" does not exist at character 45"),
        new Line("relation \"xwikidoc\" does not exist at character 29"),
        new Line("relation \"hibernate_sequence\" already exists"),
        // Jetty 9.4.x emits some warning about ASM, see https://github.com/eclipse/jetty.project/issues/2412
        // Remove once "latest" image of the Jetty container doesn't have the issue anymore
        new Line("Unknown asm implementation version, assuming version"),
        // Note: Happens when verbose is turned on
        new Line("Collision between core extension [javax.annotation:javax.annotation-api"),
        new Line("[javax.annotation:javax.annotation-api/"),
        // Appears only for PostgreSQL database.
        new Line("WARNING: enabling \"trust\" authentication for local connections"),
        // Those errors appears from time to time, mainly on the CI, related to various JS resources such as:
        // jsTree, jQuery, keypress, xwiki-events-bridge, iScroll, etc.
        // This seems to be related to actions being performed before all the resources have been correctly loaded.
        new Line("require.min.js?r=1, line 7")
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
        List<Line> matchingDefinitions = new ArrayList<>();
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
                        matchingDefinitions.add(excludedLine);
                        return false;
                    }
                }
                for (Line expectedLine : allExpected) {
                    if (isMatching(p, expectedLine)) {
                        matchingDefinitions.add(expectedLine);
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

        // Also display not matching excludes and expected so that developers can notice them and realize that the
        // issues that existed might have been fixed. Note however that currently we can't have exclude/expetced by
        // configuration (for Docker-based tests) and thus it's possible that there are non matching excludes/expected
        // simply because they exist only in a different configuration.
        displayMissingWarning(configuration.getExcludedLines(), matchingDefinitions, "excludes");
        displayMissingWarning(configuration.getExpectedLines(), matchingDefinitions, "expected");

        // Fail the test if there are matching lines that have no exclude or no expected.
        if (!matchingLines.isEmpty()) {
            throw new AssertionError(String.format("The following lines were matching forbidden content:[\n%s\n]",
                matchingLines.stream().collect(Collectors.joining(NL))));
        }
    }

    private void displayMissingWarning(List<Line> definitions, List<Line> matchingDefinitions, String missingType)
    {
        List<String> notMatchingLines = new ArrayList<>();
        for (Line line : definitions) {
            if (!matchingDefinitions.contains(line)) {
                notMatchingLines.add(line.getContent());
            }
        }
        if (!notMatchingLines.isEmpty()) {
            LOGGER.warn("The following {} were not matched and could be candidates for removal "
                + "(beware of configs): [\n{}\n]", missingType, StringUtils.join(notMatchingLines, NL));
        }
    }

    private boolean isMatching(String lineToMatch, Line line)
    {
        return (line.isRegex() && lineToMatch.matches(line.getContent()))
            || (!line.isRegex() && lineToMatch.contains(line.getContent()));
    }
}
