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

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.xwiki.test.integration.junit.LogCapture;
import org.xwiki.test.integration.junit.LogCaptureConfiguration;
import org.xwiki.test.integration.junit.LogCaptureValidator;

/**
 * Captures content sent to stdout/stderr by JUnit5 functional tests and report a failure if the content contains one of
 * the following strings.
 * <ul>
 * <li>Deprecated method calls from Velocity</li>
 * <li>Error messages</li>
 * <li>Warning messages</li>
 * <li>Javascript errors</li>
 * </ul>
 * <p>
 * Tests can register expected failing lines or even exclude failing lines by adding a parameter of type {@link
 * LogCaptureConfiguration} in a test method signature and calling one of the {@code register*()} methods.
 *
 * @version $Id$
 * @since 11.4RC1
 */
public class ValidateConsoleExtension implements BeforeAllCallback, AfterAllCallback, ParameterResolver
{
    private static final ExtensionContext.Namespace NAMESPACE =
        ExtensionContext.Namespace.create(ValidateConsoleExtension.class);

    private static final String SKIP_PROPERTY = "xwiki.test.validateconsole.skip";

    private static final boolean SKIP = Boolean.valueOf(System.getProperty(SKIP_PROPERTY, "false"));

    @Override
    public void beforeAll(ExtensionContext extensionContext)
    {
        if (SKIP) {
            return;
        }

        LogCapture logCapture = new LogCapture();
        logCapture.startCapture();
        saveLogCapture(extensionContext, logCapture);
    }

    @Override
    public void afterAll(ExtensionContext extensionContext)
    {
        if (SKIP) {
            return;
        }

        String logContent = loadLogCapture(extensionContext).stopCapture();
        LogCaptureConfiguration configuration = loadLogCaptureConfiguration(extensionContext);

        // Validate the captured log content (but only if no test is failing so that errors in the console that are
        // due to the test failure will not appear as forbidden content).
        if (!extensionContext.getExecutionException().isPresent()) {
            LogCaptureValidator validator = new LogCaptureValidator();
            validator.validate(logContent, configuration);
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
        LogCaptureConfiguration logCaptureConfiguration =
            store.get(LogCaptureConfiguration.class, LogCaptureConfiguration.class);
        if (logCaptureConfiguration == null) {
            LogCaptureConfiguration configuration = new LogCaptureConfiguration();
            saveLogCaptureConfiguration(context, configuration);
        }
        return logCaptureConfiguration;
    }
}
