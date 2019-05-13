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
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Captures content sent to stdout/stderr by JUnit5 functional tests and report a failure if the content contains one of
 * the following strings.
 * <ul>
 *   <li>Deprecated method calls from Velocity</li>
 *   <li>Error messages</li>
 *   <li>Warning messages</li>
 *   <li>Javascript errors</li>
 * </ul>
 * <p>
 * Tests can register expected failing lines or even exclude failing lines by adding a parameter of type
 * {@link LogCaptureConfiguration} in a test method signature and calling one of the {@code register*()} methods.
 *
 * @version $Id$
 * @since 11.4RC1
 */
public class ValidateConsoleExtension implements BeforeAllCallback, AfterAllCallback, ParameterResolver
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ValidateConsoleTestExecutionListener.class);

    private static final String NL = "\n";

    private static final ExtensionContext.Namespace NAMESPACE =
        ExtensionContext.Namespace.create(ValidateConsoleExtension.class);

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

    @Override
    public void beforeAll(ExtensionContext extensionContext)
    {
        LogCapture logCapture = new LogCapture();
        logCapture.startCapture();
        saveLogCapture(extensionContext, logCapture);

        LogCaptureConfiguration configuration = new LogCaptureConfiguration();
        saveLogCaptureConfiguration(extensionContext, configuration);
    }

    @Override
    public void afterAll(ExtensionContext extensionContext)
    {
        String logContent = loadLogCapture(extensionContext).stopCapture();
        LogCaptureConfiguration configuration = loadLogCaptureConfiguration(extensionContext);

        // Validate the captured log content (but only if no test is failing to not confuse the user)
        if (!extensionContext.getExecutionException().isPresent()) {
            validate(logContent, configuration);
        }
    }

    private void validate(String logContent, LogCaptureConfiguration configuration)
    {
        List<String> allExcludes = new ArrayList<>();
        allExcludes.addAll(GLOBAL_EXCLUDES);
        allExcludes.addAll(configuration.getExcludedLines());

        List<String> allExpected = new ArrayList<>();
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
                for (String excludedLine : allExcludes) {
                    if (p.contains(excludedLine)) {
                        matchingExcludes.add(p);
                        return false;
                    }
                }
                for (String expectedLine : allExpected) {
                    if (p.contains(expectedLine)) {
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

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
        throws ParameterResolutionException
    {
        Class<?> type = parameterContext.getParameter().getType();
        return LogCaptureConfiguration.class.isAssignableFrom(type);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
        throws ParameterResolutionException
    {
        return loadLogCaptureConfiguration(extensionContext);
    }

    private static ExtensionContext.Store getStore(ExtensionContext context)
    {
        return context.getRoot().getStore(NAMESPACE);
    }

    private void saveLogCapture(ExtensionContext context, LogCapture logCapture)
    {
        ExtensionContext.Store store = getStore(context);
        store.put(LogCapture.class, logCapture);
    }

    private LogCapture loadLogCapture(ExtensionContext context)
    {
        ExtensionContext.Store store = getStore(context);
        return store.get(LogCapture.class, LogCapture.class);
    }

    private void saveLogCaptureConfiguration(ExtensionContext context, LogCaptureConfiguration logCaptureConfiguration)
    {
        ExtensionContext.Store store = getStore(context);
        store.put(LogCaptureConfiguration.class, logCaptureConfiguration);
    }

    private LogCaptureConfiguration loadLogCaptureConfiguration(ExtensionContext context)
    {
        ExtensionContext.Store store = getStore(context);
        return store.get(LogCaptureConfiguration.class, LogCaptureConfiguration.class);
    }
}
