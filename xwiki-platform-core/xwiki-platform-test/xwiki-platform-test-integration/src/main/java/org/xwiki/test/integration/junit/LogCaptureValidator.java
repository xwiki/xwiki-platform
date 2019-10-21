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
        new Line("MavenExtensionScanner - [javax.annotation:javax.annotation-api/"),
        new Line("Collision between core extension [javax.transaction:javax.transaction-api"),
        new Line("MavenExtensionScanner - [javax.transaction:javax.transaction-api/"),
        // Appears only for PostgreSQL database.
        new Line("WARNING: enabling \"trust\" authentication for local connections"),
        // Those errors appears from time to time, mainly on the CI, related to various JS resources such as:
        // jsTree, jQuery, keypress, xwiki-events-bridge, iScroll, etc.
        // This seems to be related to actions being performed before all the resources have been correctly loaded.
        new Line("require.min.js?r=1, line 7"),
        // Cannot reproduce locally but happened on the CI for MenuIT.
        new Line("jstree.min.js, line 2: TypeError: c is undefined"),
        // See: https://jira.xwiki.org/browse/XWIKI-13609 comments: this log could still happen from time to time.
        new Line("Failed to save job status"),
        // When updating a collection Hibernate start by deleting the existing elements and HSQLDB complains when there
        // is actually nothing to delete it seems
        new Line("SQL Warning Code: -1100, SQLState: 02000"),
        new Line("no data"),

        // See https://jira.xwiki.org/browse/XWIKI-16484
        new Line("SQL Error: -104, SQLState: 23505"),
        new Line("integrity constraint violation: unique constraint or index violation; SYS_PK_10260 table: XWIKILOCK"),
        new Line("Exception while setting up lock"),
        new Line("com.xpn.xwiki.XWikiException: Error number 13006 in 3: Exception while locking document for lock"
            + " [userName = [XWiki.superadmin], docId = [6152552094868048244],"
            + " date = [Tue Jun 18 12:21:51 CEST 2019]]"),
        new Line("Caused by: javax.persistence.PersistenceException:"
            + " org.hibernate.exception.ConstraintViolationException: could not execute statement"),
        new Line("Caused by: org.hibernate.exception.ConstraintViolationException: could not execute statement"),
        new Line("Caused by: java.sql.SQLIntegrityConstraintViolationException: integrity constraint violation: "
            + "unique constraint or index violation; SYS_PK_10260 table: XWIKILOCK"),
        new Line("Caused by: org.hsqldb.HsqlException: integrity constraint violation:"
            + " unique constraint or index violation; SYS_PK_10260 table: XWIKILOCK"),

        // Solr brings since 8.1.1 jetty dependencies in the classloader, so the upgrade might warn about collisions
        new Line("Collision between core extension [org.eclipse.jetty:jetty-http"),
        new Line("MavenExtensionScanner - [org.eclipse.jetty:jetty-http"),
        new Line("Collision between core extension [org.eclipse.jetty:jetty-io"),
        new Line("MavenExtensionScanner - [org.eclipse.jetty:jetty-io"),
        new Line("Collision between core extension [org.eclipse.jetty:jetty-util"),
        new Line("MavenExtensionScanner - [org.eclipse.jetty:jetty-util"),
        // This warning is not coming from XWiki but from one jetty dependency, apparently a configuration is not
        // properly used on Solr part. More details can be found there:
        // https://github.com/eclipse/jetty.project/issues/3454
        new Line("No Client EndPointIdentificationAlgorithm configured for SslContextFactory"),

        // Java 11 restriction on field access
        new Line("WARNING: An illegal reflective access operation has occurred"),
        new Line("WARNING: Illegal reflective access by "),
        new Line("WARNING: Please consider reporting this to the maintainers of"),
        new Line("WARNING: Use --illegal-access=warn to enable warnings of further illegal reflective "
            + "access operations"),
        new Line("WARNING: All illegal access operations will be denied in a future release"),

        // This warning happens in tests when using firefox.
        // It seems related to this closed issue https://github.com/SeleniumHQ/docker-selenium/issues/388
        // we might want to fix this using shm_size argument as explained in the thread, but AFAICS it doesn't guarantee
        // to solve the issue and will potentially consume more memory. Moreover the log error doesn't look related
        // to any problem during our tests.
        // The warning is something such as:
        // Connection reset by peer:
        // file /builds/worker/workspace/build/src/ipc/chromium/src/chrome/common/ipc_channel_posix.cc, line 357
        // But the path might be different from machine to machine, and the error might be localized.
        // So we keep only the end of the path to match warnings in different configurations.
        new Line("ipc/chromium/src/chrome/common/ipc_channel_posix.cc"),

        // Warning obtained locally only so far when testing with firefox.
        // It's related to this issue: https://bugzilla.mozilla.org/show_bug.cgi?id=1132140
        // It shouldn't have any impact on our tests.
        new Line(" ../glib/gobject/gsignal.c:3498: signal name 'load_complete' is invalid"),

        // Happened only locally so far, might be related to a bad configuration, anyway it shouldn't have any impact
        // on our tests.
        new Line("Unable to open /var/lib/flatpak/exports/share/dconf/profile/user"),

        // Triggered by the HTML5 Validator.
        // This should be fixed with https://jira.xwiki.org/browse/XWIKI-16791
        new Line("The “type” attribute is unnecessary for JavaScript resources."),

        // This error apparently occurs because an async resource was still loading when we move to another page
        // This is most certainly related to some pages for which we don't wait the JS to be loaded properly before
        // making some assertion or interactions.
        new Line("java.lang.IllegalStateException: Response is ABORTED")
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
        // issues that existed might have been fixed. Note however that currently we can't have exclude/expected by
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
