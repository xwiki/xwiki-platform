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
package org.xwiki.wysiwyg.server.internal.cleaner;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.Suite;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;
import org.xwiki.gwt.wysiwyg.client.cleaner.HTMLCleaner;
import org.xwiki.test.XWikiComponentInitializer;

/**
 * A suite of {@link HTMLCleanerTest}.
 * 
 * @version $Id$
 */
public class HTMLCleanerTestSuite extends Suite
{
    /**
     * The line that separates the HTML input from the expected clean HTML.
     */
    private static final String INPUT_EXPECTED_SEPARATOR = "---";

    /**
     * The string that prefixes comment lines.
     */
    private static final String COMMENT_LINE_PREFIX = "#";

    /**
     * The string used to escape the new line character. This is very useful when you want to split the HTML input or
     * the expected clean HTML on multiple lines without inserting new line characters in the content.
     */
    private static final String NEW_LINE_ESCAPE = "\\";

    /**
     * The class used to instantiate test, passing the test data to the constructor.
     */
    private class TestClassRunnerForParameters extends BlockJUnit4ClassRunner
    {
        /**
         * The index of this test.
         */
        private final int parameterSetNumber;

        /**
         * The object used to initialize the component manager.
         */
        private final XWikiComponentInitializer componentInitializer = new XWikiComponentInitializer();

        /**
         * The list of all tests data.
         */
        private final List<Object[]> parameterList;

        /**
         * @param type the test class
         * @param parameterList the list of data for all tests
         * @param i the index of the current test
         * @throws InitializationError if the runner initialization fails
         */
        TestClassRunnerForParameters(Class< ? > type, List<Object[]> parameterList, int i) throws InitializationError
        {
            super(type);
            this.parameterList = parameterList;
            this.parameterSetNumber = i;
        }

        @Override
        public Object createTest() throws Exception
        {
            return getTestClass().getOnlyConstructor().newInstance(computeParams());
        }

        /**
         * @return the parameters that are passed to the test constructor
         * @throws Exception if we fail to include the component manager in the list of parameters
         */
        private Object[] computeParams() throws Exception
        {
            // Add the Component Manager as the last parameter in order to pass it to the Test constructor
            // Remove the first parameter which is the test name and that is not needed in HTMLCleanerTest.
            Object[] originalObjects = this.parameterList.get(this.parameterSetNumber);
            Object[] newObjects = new Object[originalObjects.length];
            System.arraycopy(originalObjects, 1, newObjects, 0, originalObjects.length - 1);
            HTMLCleaner cleaner = this.componentInitializer.getComponentManager().getInstance(HTMLCleaner.class);
            newObjects[originalObjects.length - 1] = cleaner;
            return newObjects;
        }

        @Override
        protected String getName()
        {
            return (String) this.parameterList.get(this.parameterSetNumber)[0];
        }

        @Override
        protected String testName(FrameworkMethod method)
        {
            return getName();
        }

        @Override
        protected void validateConstructor(List<Throwable> errors)
        {
            validateOnlyOneConstructor(errors);
        }

        @Override
        protected Statement classBlock(RunNotifier notifier)
        {
            return childrenInvoker(notifier);
        }

        /**
         * {@inheritDoc}
         * <p>
         * Initialize the Component Manager before each test is executed, to ensure test isolation.
         */
        @Override
        protected void runChild(FrameworkMethod method, RunNotifier notifier)
        {
            try {
                this.componentInitializer.initializeConfigurationSource();
                this.componentInitializer.initializeExecution();
            } catch (Exception e) {
                throw new RuntimeException("Failed to initialize Component Manager", e);
            }

            try {
                super.runChild(method, notifier);
            } finally {
                try {
                    this.componentInitializer.shutdown();
                } catch (Exception e) {
                    throw new RuntimeException("Failed to shutdown Component Manager", e);
                }
            }
        }
    }

    /**
     * The tests to run.
     */
    private final List<Runner> runners = new ArrayList<Runner>();

    /**
     * Only called reflectively.
     * 
     * @param klass the class that is run with this suite
     * @throws Exception if the if the suite can't be initialized
     */
    public HTMLCleanerTestSuite(Class< ? > klass) throws Exception
    {
        super(HTMLCleanerTest.class, Collections.<Runner> emptyList());

        List<Object[]> testsData = readTestsDataFromResource("/HTMLCleanerTests.txt");
        for (int i = 0; i < testsData.size(); i++) {
            this.runners.add(new TestClassRunnerForParameters(getTestClass().getJavaClass(), testsData, i));
        }
    }

    @Override
    protected List<Runner> getChildren()
    {
        return this.runners;
    }

    /**
     * {@inheritDoc} We override this method so that the JUnit results are not displayed in a test hierarchy with a
     * single test result for each node (as it would be otherwise since RenderingTest has a single test method).
     */
    @Override
    public Description getDescription()
    {
        return Description.createSuiteDescription(getTestClass().getJavaClass());
    }

    /**
     * Reads the data for the tests listed in the specified resource file.
     * 
     * @param testResourceName the name of a resource file that lists the tests to be run
     * @return the data for all the test to be run
     * @throws IOException if reading the test resource fails
     */
    private List<Object[]> readTestsDataFromResource(String testResourceName) throws IOException
    {
        List<Object[]> testsData = new ArrayList<Object[]>();
        for (String testName : getTestNames(getClass().getResourceAsStream(testResourceName))) {
            testsData.add(readTestDataFromResource(testName));
        }
        return testsData;
    }

    /**
     * Reads the list of test names from the given input stream.
     * 
     * @param source where to read the test names from
     * @return a list of test names
     * @throws IOException if reading the test names fails
     */
    private List<String> getTestNames(InputStream source) throws IOException
    {
        BufferedReader reader = new BufferedReader(new InputStreamReader(source));
        List<String> testNames = new ArrayList<String>();
        try {
            String line = reader.readLine();
            while (line != null) {
                if (!line.startsWith(COMMENT_LINE_PREFIX)) {
                    testNames.add(line.trim());
                }
                line = reader.readLine();
            }
        } finally {
            reader.close();
        }
        return testNames;
    }

    /**
     * Reads the test data, the HTML input and the expected clean HTML, from the resource file with the specified name.
     * 
     * @param testResourceName the name of a resource file that contains the test data
     * @return the test data read from the specified resource
     * @throws IOException if reading the resource files fails
     */
    private String[] readTestDataFromResource(String testResourceName) throws IOException
    {
        InputStream source = getClass().getResourceAsStream('/' + testResourceName + ".test");
        BufferedReader reader = new BufferedReader(new InputStreamReader(source));
        try {
            StringBuilder input = new StringBuilder();
            String line = reader.readLine();
            while (line != null && !line.equals(INPUT_EXPECTED_SEPARATOR)) {
                appendLine(input, line);
                line = reader.readLine();
            }
            StringBuilder expected = new StringBuilder();
            // Skip the line that separates the input from the expected HTML.
            line = reader.readLine();
            while (line != null) {
                appendLine(expected, line);
                line = reader.readLine();
            }
            return new String[] {testResourceName, input.toString(), expected.toString()};
        } finally {
            reader.close();
        }
    }

    /**
     * Skips the line if it's a comment and removes the new line character if it is escaped.
     * 
     * @param output where to append the given line
     * @param line the line of text to be appended
     */
    private void appendLine(StringBuilder output, String line)
    {
        if (line.startsWith(COMMENT_LINE_PREFIX)) {
            return;
        } else if (line.endsWith(NEW_LINE_ESCAPE)) {
            output.append(line, 0, line.length() - NEW_LINE_ESCAPE.length());
        } else {
            output.append(line).append('\n');
        }
    }
}
