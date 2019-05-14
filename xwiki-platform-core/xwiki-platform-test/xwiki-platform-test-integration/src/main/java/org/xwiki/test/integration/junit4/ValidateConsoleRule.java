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
package org.xwiki.test.integration.junit4;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.xwiki.test.integration.junit.LogCapture;
import org.xwiki.test.integration.junit.LogCaptureConfiguration;
import org.xwiki.test.integration.junit.LogCaptureValidator;

/**
 * Captures content sent to stdout/stderr by JUnit4 functional tests and report a failure if the content contains some
 * violations. See {@link org.xwiki.test.integration.junit5.ValidateConsoleExtension} for more details.
 *
 * @version $Id$
 * @since 11.4RC1
 */
public class ValidateConsoleRule implements TestRule
{
    private LogCaptureConfiguration logCaptureConfiguration = new LogCaptureConfiguration();

    /**
     * The actual code that executes our capturing logic before the test runs and removes it after it has run.
     */
    public class ValidateConsoleStatement extends Statement
    {
        /**
         * @see #ValidateConsoleStatement(org.junit.runners.model.Statement)
         */
        private final Statement statement;

        private LogCapture logCapture;

        /**
         * @param statement the wrapping statement that we save so that we can execute it (the statement represents
         *        the test to execute).
         */
        public ValidateConsoleStatement(Statement statement)
        {
            this.statement = statement;
        }

        @Override
        public void evaluate() throws Throwable
        {
            before();

            boolean hasTestErrors = false;
            try {
                // Run the test
                this.statement.evaluate();
            } catch (Throwable t) {
                hasTestErrors = true;
                throw t;
            } finally {
                after(hasTestErrors);
            }
        }

        private void before()
        {
            this.logCapture = new LogCapture();
            this.logCapture.startCapture();
        }

        private void after(boolean hasTestErrors)
        {
            String logContent = this.logCapture.stopCapture();

            // Validate the captured log content (but only if no test is failing to not confuse the user)
            if (!hasTestErrors) {
                LogCaptureValidator validator = new LogCaptureValidator();
                validator.validate(logContent, logCaptureConfiguration);
            }
        }
    }

    @Override
    public Statement apply(Statement statement, Description description)
    {
        return new ValidateConsoleStatement(statement);
    }

    /**
     * @return the configuration object to be used by tests to register excludes and expected lines
     */
    public LogCaptureConfiguration getLogCaptureConfiguration()
    {
        return this.logCaptureConfiguration;
    }
}
